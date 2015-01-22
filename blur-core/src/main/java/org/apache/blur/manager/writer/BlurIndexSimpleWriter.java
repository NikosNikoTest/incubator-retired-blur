/**
s * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.blur.manager.writer;

import static org.apache.blur.lucene.LuceneVersionConstant.LUCENE_VERSION;
import static org.apache.blur.utils.BlurConstants.ACL_DISCOVER;
import static org.apache.blur.utils.BlurConstants.ACL_READ;
import static org.apache.blur.utils.BlurConstants.BLUR_SHARD_QUEUE_MAX_INMEMORY_LENGTH;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import lucene.security.index.AccessControlFactory;

import org.apache.blur.BlurConfiguration;
import org.apache.blur.analysis.FieldManager;
import org.apache.blur.index.ExitableReader;
import org.apache.blur.index.IndexDeletionPolicyReader;
import org.apache.blur.log.Log;
import org.apache.blur.log.LogFactory;
import org.apache.blur.lucene.codec.Blur024Codec;
import org.apache.blur.lucene.search.IndexSearcherCloseable;
import org.apache.blur.server.IndexSearcherCloseableBase;
import org.apache.blur.server.IndexSearcherCloseableSecureBase;
import org.apache.blur.server.ShardContext;
import org.apache.blur.server.TableContext;
import org.apache.blur.thrift.generated.BlurException;
import org.apache.blur.thrift.generated.RowMutation;
import org.apache.blur.thrift.generated.TableDescriptor;
import org.apache.blur.trace.Trace;
import org.apache.blur.trace.Tracer;
import org.apache.blur.user.User;
import org.apache.blur.user.UserContext;
import org.apache.blur.utils.BlurConstants;
import org.apache.blur.utils.BlurUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.io.SequenceFile.Reader;
import org.apache.hadoop.io.SequenceFile.Sorter;
import org.apache.hadoop.io.SequenceFile.Writer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.DefaultCodec;
import org.apache.hadoop.io.compress.SnappyCodec;
import org.apache.hadoop.util.Progressable;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.BlurIndexWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;

import com.google.common.base.Splitter;

public class BlurIndexSimpleWriter extends BlurIndex {

  private static final String TRUE = "true";

  private static final Log LOG = LogFactory.getLog(BlurIndexSimpleWriter.class);

  private final AtomicBoolean _isClosed = new AtomicBoolean();
  private final BlurIndexCloser _indexCloser;
  private final AtomicReference<DirectoryReader> _indexReader = new AtomicReference<DirectoryReader>();
  private final ExecutorService _searchThreadPool;
  private final Directory _directory;
  private final IndexWriterConfig _conf;
  private final TableContext _tableContext;
  private final FieldManager _fieldManager;
  private final ShardContext _shardContext;
  private final AtomicReference<BlurIndexWriter> _writer = new AtomicReference<BlurIndexWriter>();
  private final boolean _makeReaderExitable = true;
  private final ReentrantReadWriteLock _lock = new ReentrantReadWriteLock();
  private final WriteLock _writeLock = _lock.writeLock();
  private final ReadWriteLock _indexRefreshLock = new ReentrantReadWriteLock();
  private final Lock _indexRefreshWriteLock = _indexRefreshLock.writeLock();
  private final Lock _indexRefreshReadLock = _indexRefreshLock.readLock();
  private final IndexDeletionPolicyReader _policy;
  private final SnapshotIndexDeletionPolicy _snapshotIndexDeletionPolicy;
  private final String _context;
  private final AtomicInteger _writesWaiting = new AtomicInteger();
  private final BlockingQueue<RowMutation> _queue;
  private final MutationQueueProcessor _mutationQueueProcessor;
  private final Timer _indexImporterTimer;
  private final Map<String, BulkEntry> _bulkWriters;
  private final boolean _security;
  private final AccessControlFactory _accessControlFactory;
  private final Set<String> _discoverableFields;
  private final Splitter _commaSplitter;

  private Thread _optimizeThread;
  private Thread _writerOpener;
  private IndexImporter _indexImporter;

  public BlurIndexSimpleWriter(ShardContext shardContext, Directory directory, SharedMergeScheduler mergeScheduler,
      final ExecutorService searchExecutor, BlurIndexCloser indexCloser, Timer indexImporterTimer) throws IOException {
    super(shardContext, directory, mergeScheduler, searchExecutor, indexCloser, indexImporterTimer);
    _commaSplitter = Splitter.on(',');
    _bulkWriters = new ConcurrentHashMap<String, BlurIndexSimpleWriter.BulkEntry>();
    _indexImporterTimer = indexImporterTimer;
    _searchThreadPool = searchExecutor;
    _shardContext = shardContext;
    _tableContext = _shardContext.getTableContext();
    _context = _tableContext.getTable() + "/" + shardContext.getShard();
    _fieldManager = _tableContext.getFieldManager();
    _discoverableFields = _tableContext.getDiscoverableFields();
    _accessControlFactory = _tableContext.getAccessControlFactory();
    TableDescriptor descriptor = _tableContext.getDescriptor();
    Map<String, String> tableProperties = descriptor.getTableProperties();
    if (tableProperties != null) {
      String value = tableProperties.get(BlurConstants.BLUR_RECORD_SECURITY);
      if (value != null && value.equals(TRUE)) {
        LOG.info("Record Level Security has been enabled for table [{0}] shard [{1}]", _tableContext.getTable(),
            _shardContext.getShard());
        _security = true;
      } else {
        _security = false;
      }
    } else {
      _security = false;
    }
    Analyzer analyzer = _fieldManager.getAnalyzerForIndex();
    _conf = new IndexWriterConfig(LUCENE_VERSION, analyzer);
    _conf.setWriteLockTimeout(TimeUnit.MINUTES.toMillis(5));
    _conf.setCodec(new Blur024Codec(_tableContext.getBlurConfiguration()));
    _conf.setSimilarity(_tableContext.getSimilarity());
    _conf.setInfoStream(new LoggingInfoStream(_tableContext.getTable(), _shardContext.getShard()));
    TieredMergePolicy mergePolicy = (TieredMergePolicy) _conf.getMergePolicy();
    mergePolicy.setUseCompoundFile(false);
    _conf.setMergeScheduler(mergeScheduler.getMergeScheduler());
    _snapshotIndexDeletionPolicy = new SnapshotIndexDeletionPolicy(_tableContext.getConfiguration(), new Path(
        shardContext.getHdfsDirPath(), "generations"));
    _policy = new IndexDeletionPolicyReader(_snapshotIndexDeletionPolicy);
    _conf.setIndexDeletionPolicy(_policy);
    BlurConfiguration blurConfiguration = _tableContext.getBlurConfiguration();
    _queue = new ArrayBlockingQueue<RowMutation>(blurConfiguration.getInt(BLUR_SHARD_QUEUE_MAX_INMEMORY_LENGTH, 100));
    _mutationQueueProcessor = new MutationQueueProcessor(_queue, this, _shardContext, _writesWaiting);

    if (!DirectoryReader.indexExists(directory)) {
      new BlurIndexWriter(directory, _conf).close();
    }

    _directory = directory;

    _indexCloser = indexCloser;
    _indexReader.set(wrap(DirectoryReader.open(_directory)));

    openWriter();
  }

  private synchronized void openWriter() {
    IOUtils.cleanup(LOG, _indexImporter);
    BlurIndexWriter writer = _writer.get();
    if (writer != null) {
      try {
        writer.close(false);
      } catch (IOException e) {
        LOG.error("Unknown error while trying to close the writer, [" + _shardContext.getTableContext().getTable()
            + "] Shard [" + _shardContext.getShard() + "]", e);
      }
      _writer.set(null);
    }
    _writerOpener = getWriterOpener(_shardContext);
    _writerOpener.start();
  }

  private DirectoryReader wrap(DirectoryReader reader) throws IOException {
    if (_makeReaderExitable) {
      reader = new ExitableReader(reader);
    }
    return _policy.register(reader);
  }

  private Thread getWriterOpener(ShardContext shardContext) {
    Thread thread = new Thread(new Runnable() {

      @Override
      public void run() {
        try {
          _writer.set(new BlurIndexWriter(_directory, _conf.clone()));
          synchronized (_writer) {
            _writer.notify();
          }
          _indexImporter = new IndexImporter(_indexImporterTimer, BlurIndexSimpleWriter.this, _shardContext,
              TimeUnit.SECONDS, 10);
        } catch (IOException e) {
          LOG.error("Unknown error on index writer open.", e);
        }
      }
    });
    thread.setName("Writer Opener for Table [" + shardContext.getTableContext().getTable() + "] Shard ["
        + shardContext.getShard() + "]");
    thread.setDaemon(true);
    return thread;
  }

  @Override
  public IndexSearcherCloseable getIndexSearcher() throws IOException {
    return getIndexSearcher(_security);
  }

  public IndexSearcherCloseable getIndexSearcher(boolean security) throws IOException {
    final IndexReader indexReader;
    _indexRefreshReadLock.lock();
    try {
      indexReader = _indexReader.get();
      indexReader.incRef();
    } finally {
      _indexRefreshReadLock.unlock();
    }
    if (indexReader instanceof ExitableReader) {
      ((ExitableReader) indexReader).reset();
    }
    if (security) {
      return getSecureIndexSearcher(indexReader);
    } else {
      return getInsecureIndexSearcher(indexReader);
    }
  }

  private IndexSearcherCloseable getSecureIndexSearcher(final IndexReader indexReader) throws IOException {
    String readStr = null;
    String discoverStr = null;
    User user = UserContext.getUser();
    if (user != null) {
      Map<String, String> attributes = user.getAttributes();
      if (attributes != null) {
        readStr = attributes.get(ACL_READ);
        discoverStr = attributes.get(ACL_DISCOVER);
      }
    }
    Collection<String> readAuthorizations = toCollection(readStr);
    Collection<String> discoverAuthorizations = toCollection(discoverStr);
    return new IndexSearcherCloseableSecureBase(indexReader, _searchThreadPool, _accessControlFactory,
        readAuthorizations, discoverAuthorizations, _discoverableFields) {
      private boolean _closed;

      @Override
      public Directory getDirectory() {
        return _directory;
      }

      @Override
      public synchronized void close() throws IOException {
        if (!_closed) {
          indexReader.decRef();
          _closed = true;
        } else {
          // Not really sure why some indexes get closed called twice on them.
          // This is in place to log it.
          if (LOG.isDebugEnabled()) {
            LOG.debug("Searcher already closed [{0}].", new Throwable(), this);
          }
        }
      }
    };
  }

  @SuppressWarnings("unchecked")
  private Collection<String> toCollection(String aclStr) {
    if (aclStr == null) {
      return Collections.EMPTY_LIST;
    }
    Set<String> result = new HashSet<String>();
    for (String s : _commaSplitter.split(aclStr)) {
      result.add(s);
    }
    return result;
  }

  private IndexSearcherCloseable getInsecureIndexSearcher(final IndexReader indexReader) {
    return new IndexSearcherCloseableBase(indexReader, _searchThreadPool) {
      private boolean _closed;

      @Override
      public Directory getDirectory() {
        return _directory;
      }

      @Override
      public synchronized void close() throws IOException {
        if (!_closed) {
          indexReader.decRef();
          _closed = true;
        } else {
          // Not really sure why some indexes get closed called twice on them.
          // This is in place to log it.
          if (LOG.isDebugEnabled()) {
            LOG.debug("Searcher already closed [{0}].", new Throwable(), this);
          }
        }
      }
    };
  }

  private void waitUntilNotNull(AtomicReference<?> ref) {
    while (true) {
      Object object = ref.get();
      if (object != null) {
        return;
      }
      synchronized (ref) {
        try {
          ref.wait(TimeUnit.SECONDS.toMillis(1));
        } catch (InterruptedException e) {
          return;
        }
      }
    }
  }

  @Override
  public void close() throws IOException {
    _isClosed.set(true);
    IOUtils.cleanup(LOG, _indexImporter, _mutationQueueProcessor, _writer.get(), _indexReader.get());
  }

  @Override
  public void refresh() throws IOException {

  }

  @Override
  public AtomicBoolean isClosed() {
    return _isClosed;
  }

  @Override
  public synchronized void optimize(final int numberOfSegmentsPerShard) throws IOException {
    final String table = _tableContext.getTable();
    final String shard = _shardContext.getShard();
    if (_optimizeThread != null && _optimizeThread.isAlive()) {
      LOG.info("Already running an optimize on table [{0}] shard [{1}]", table, shard);
      return;
    }
    _optimizeThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          waitUntilNotNull(_writer);
          BlurIndexWriter writer = _writer.get();
          writer.forceMerge(numberOfSegmentsPerShard, true);
          _writeLock.lock();
          try {
            commit();
          } finally {
            _writeLock.unlock();
          }
        } catch (Exception e) {
          LOG.error("Unknown error during optimize on table [{0}] shard [{1}]", e, table, shard);
        }
      }
    });
    _optimizeThread.setDaemon(true);
    _optimizeThread.setName("Optimize table [" + table + "] shard [" + shard + "]");
    _optimizeThread.start();
  }

  @Override
  public void createSnapshot(String name) throws IOException {
    _writeLock.lock();
    try {
      _snapshotIndexDeletionPolicy.createSnapshot(name, _indexReader.get(), _context);
    } finally {
      _writeLock.unlock();
    }
  }

  @Override
  public void removeSnapshot(String name) throws IOException {
    _writeLock.lock();
    try {
      _snapshotIndexDeletionPolicy.removeSnapshot(name, _context);
    } finally {
      _writeLock.unlock();
    }
  }

  @Override
  public List<String> getSnapshots() throws IOException {
    return new ArrayList<String>(_snapshotIndexDeletionPolicy.getSnapshots());
  }

  private void commit() throws IOException {
    Tracer trace1 = Trace.trace("prepareCommit");
    waitUntilNotNull(_writer);
    BlurIndexWriter writer = _writer.get();
    writer.prepareCommit();
    trace1.done();

    Tracer trace2 = Trace.trace("commit");
    writer.commit();
    trace2.done();

    Tracer trace3 = Trace.trace("index refresh");
    DirectoryReader currentReader = _indexReader.get();
    DirectoryReader newReader = DirectoryReader.openIfChanged(currentReader);
    if (newReader == null) {
      LOG.debug("Reader should be new after commit for table [{0}] shard [{1}].", _tableContext.getTable(),
          _shardContext.getShard());
    } else {
      DirectoryReader reader = wrap(newReader);
      _indexRefreshWriteLock.lock();
      try {
        _indexReader.set(reader);
      } finally {
        _indexRefreshWriteLock.unlock();
      }
      _indexCloser.close(currentReader);
    }
    trace3.done();
  }

  @Override
  public void process(IndexAction indexAction) throws IOException {
    _writesWaiting.incrementAndGet();
    _writeLock.lock();
    _writesWaiting.decrementAndGet();
    indexAction.setWritesWaiting(_writesWaiting);
    waitUntilNotNull(_writer);
    BlurIndexWriter writer = _writer.get();
    IndexSearcherCloseable indexSearcher = null;
    try {
      indexSearcher = getIndexSearcher(false);
      indexAction.performMutate(indexSearcher, writer);
      indexAction.doPreCommit(indexSearcher, writer);
      commit();
      indexAction.doPostCommit(writer);
    } catch (Exception e) {
      indexAction.doPreRollback(writer);
      writer.rollback();
      openWriter();
      indexAction.doPostRollback(writer);
      throw new IOException("Unknown error during mutation", e);
    } finally {
      if (indexSearcher != null) {
        indexSearcher.close();
      }
      _writeLock.unlock();
    }
  }

  public Path getSnapshotsDirectoryPath() {
    return _snapshotIndexDeletionPolicy.getSnapshotsDirectoryPath();
  }

  @Override
  public void enqueue(List<RowMutation> mutations) throws IOException {
    startQueueIfNeeded();
    try {
      for (RowMutation mutation : mutations) {
        _queue.put(mutation);
      }
      synchronized (_queue) {
        _queue.notifyAll();
      }
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }

  private void startQueueIfNeeded() {
    _mutationQueueProcessor.startIfNotRunning();
  }

  static class BulkEntry {
    final SequenceFile.Writer _writer;
    final Path _path;

    BulkEntry(Writer writer, Path path) {
      _writer = writer;
      _path = path;
    }
  }

  public BulkEntry startBulkMutate(String bulkId) throws IOException {
    BulkEntry bulkEntry = _bulkWriters.get(bulkId);
    if (bulkEntry == null) {
      Path tablePath = _tableContext.getTablePath();
      Path bulk = new Path(tablePath, "bulk");
      Path bulkInstance = new Path(bulk, bulkId);
      Path path = new Path(bulkInstance, _shardContext.getShard() + ".notsorted.seq");
      Configuration configuration = _tableContext.getConfiguration();
      FileSystem fileSystem = path.getFileSystem(configuration);

      Progressable progress = new Progressable() {
        @Override
        public void progress() {

        }
      };
      final CompressionCodec codec;
      final CompressionType type;

      if (isSnappyCodecLoaded(configuration)) {
        codec = new SnappyCodec();
        type = CompressionType.BLOCK;
      } else {
        codec = new DefaultCodec();
        type = CompressionType.NONE;
      }

      Writer writer = SequenceFile.createWriter(fileSystem, configuration, path, Text.class, RowMutationWritable.class,
          type, codec, progress);

      bulkEntry = new BulkEntry(writer, path);
      _bulkWriters.put(bulkId, bulkEntry);
    } else {
      LOG.info("Bulk [{0}] mutate already started on shard [{1}] in table [{2}].", bulkId, _shardContext.getShard(),
          _tableContext.getTable());
    }
    return bulkEntry;
  }

  private boolean isSnappyCodecLoaded(Configuration configuration) {
    try {
      Method methodHadoop1 = SnappyCodec.class.getMethod("isNativeSnappyLoaded", new Class[] { Configuration.class });
      Boolean loaded = (Boolean) methodHadoop1.invoke(null, new Object[] { configuration });
      if (loaded != null && loaded) {
        LOG.info("Using SnappyCodec");
        return true;
      } else {
        LOG.info("Not using SnappyCodec");
        return false;
      }
    } catch (NoSuchMethodException e) {
      Method methodHadoop2;
      try {
        methodHadoop2 = SnappyCodec.class.getMethod("isNativeCodeLoaded", new Class[] {});
      } catch (NoSuchMethodException ex) {
        LOG.info("Can not determine if SnappyCodec is loaded.");
        return false;
      } catch (SecurityException ex) {
        LOG.error("Not allowed.", ex);
        return false;
      }
      Boolean loaded;
      try {
        loaded = (Boolean) methodHadoop2.invoke(null);
        if (loaded != null && loaded) {
          LOG.info("Using SnappyCodec");
          return true;
        } else {
          LOG.info("Not using SnappyCodec");
          return false;
        }
      } catch (Exception ex) {
        LOG.info("Unknown error while trying to determine if SnappyCodec is loaded.", ex);
        return false;
      }
    } catch (SecurityException e) {
      LOG.error("Not allowed.", e);
      return false;
    } catch (Exception e) {
      LOG.info("Unknown error while trying to determine if SnappyCodec is loaded.", e);
      return false;
    }
  }

  @Override
  public void finishBulkMutate(final String bulkId, boolean apply, boolean blockUntilComplete) throws IOException {
    final String table = _tableContext.getTable();
    final String shard = _shardContext.getShard();

    final BulkEntry bulkEntry = _bulkWriters.get(bulkId);
    if (bulkEntry == null) {
      LOG.info("Shard [{2}/{3}] Id [{0}] Nothing to apply.", bulkId, apply, table, shard);
      return;
    }
    LOG.info("Shard [{2}/{3}] Id [{0}] Finishing bulk mutate apply [{1}]", bulkId, apply, table, shard);
    bulkEntry._writer.close();

    Configuration configuration = _tableContext.getConfiguration();
    final Path path = bulkEntry._path;
    final FileSystem fileSystem = path.getFileSystem(configuration);

    if (!apply) {
      fileSystem.delete(path, false);
      Path parent = path.getParent();
      removeParentIfLastFile(fileSystem, parent);
    } else {
      Runnable runnable = new Runnable() {
        @Override
        public void run() {
          try {
            process(new IndexAction() {
              private Path _sorted;

              @Override
              public void performMutate(IndexSearcherCloseable searcher, IndexWriter writer) throws IOException {
                Configuration configuration = _tableContext.getConfiguration();

                SequenceFile.Sorter sorter = new Sorter(fileSystem, Text.class, RowMutationWritable.class,
                    configuration);

                _sorted = new Path(path.getParent(), shard + ".sorted.seq");

                LOG.info("Shard [{2}/{3}] Id [{4}] Sorting mutates path [{0}] sorted path [{1}]", path, _sorted, table,
                    shard, bulkId);
                sorter.sort(path, _sorted);

                LOG.info("Shard [{1}/{2}] Id [{3}] Applying mutates sorted path [{0}]", _sorted, table, shard, bulkId);
                Reader reader = new SequenceFile.Reader(fileSystem, _sorted, configuration);

                Text key = new Text();
                RowMutationWritable value = new RowMutationWritable();

                Text last = null;
                List<RowMutation> list = new ArrayList<RowMutation>();
                while (reader.next(key, value)) {
                  if (!key.equals(last)) {
                    flushMutates(searcher, writer, list);
                    last = new Text(key);
                    list.clear();
                  }
                  list.add(value.getRowMutation().deepCopy());
                }
                flushMutates(searcher, writer, list);
                reader.close();
                LOG.info("Shard [{0}/{1}] Id [{2}] Finished applying mutates starting commit.", table, shard, bulkId);
              }

              private void flushMutates(IndexSearcherCloseable searcher, IndexWriter writer, List<RowMutation> list)
                  throws IOException {
                if (!list.isEmpty()) {
                  List<RowMutation> reduceMutates;
                  try {
                    reduceMutates = MutatableAction.reduceMutates(list);
                  } catch (BlurException e) {
                    throw new IOException(e);
                  }
                  for (RowMutation mutation : reduceMutates) {
                    MutatableAction mutatableAction = new MutatableAction(_shardContext);
                    mutatableAction.mutate(mutation);
                    mutatableAction.performMutate(searcher, writer);
                  }
                }
              }

              private void cleanupFiles() throws IOException {
                fileSystem.delete(path, false);
                fileSystem.delete(_sorted, false);
                Path parent = path.getParent();
                removeParentIfLastFile(fileSystem, parent);
              }

              @Override
              public void doPreRollback(IndexWriter writer) throws IOException {

              }

              @Override
              public void doPreCommit(IndexSearcherCloseable indexSearcher, IndexWriter writer) throws IOException {

              }

              @Override
              public void doPostRollback(IndexWriter writer) throws IOException {
                cleanupFiles();
              }

              @Override
              public void doPostCommit(IndexWriter writer) throws IOException {
                cleanupFiles();
              }
            });
          } catch (IOException e) {
            LOG.error("Shard [{0}/{1}] Id [{2}] Unknown error while trying to finish the bulk updates.", table, shard,
                bulkId, e);
          }
        }
      };
      if (blockUntilComplete) {
        runnable.run();
      } else {
        Thread thread = new Thread(runnable);
        thread.setName("Bulk Finishing Thread Table [" + table + "] Shard [" + shard + "] BulkId [" + bulkId + "]");
        thread.start();
      }
    }
  }

  @Override
  public void addBulkMutate(String bulkId, RowMutation mutation) throws IOException {
    BulkEntry bulkEntry = _bulkWriters.get(bulkId);
    if (bulkEntry == null) {
      bulkEntry = startBulkMutate(bulkId);
    }
    RowMutationWritable rowMutationWritable = new RowMutationWritable();
    rowMutationWritable.setRowMutation(mutation);
    synchronized (bulkEntry._writer) {
      bulkEntry._writer.append(getKey(mutation), rowMutationWritable);
    }
  }

  private Text getKey(RowMutation mutation) {
    return new Text(mutation.getRowId());
  }

  private static void removeParentIfLastFile(final FileSystem fileSystem, Path parent) throws IOException {
    FileStatus[] listStatus = fileSystem.listStatus(parent);
    if (listStatus != null) {
      if (listStatus.length == 0) {
        if (!fileSystem.delete(parent, false)) {
          if (fileSystem.exists(parent)) {
            LOG.error("Could not remove parent directory [{0}]", parent);
          }
        }
      }
    }
  }

  @Override
  public long getRecordCount() throws IOException {
    IndexSearcherCloseable searcher = getIndexSearcher(false);
    try {
      return searcher.getIndexReader().numDocs();
    } finally {
      if (searcher != null) {
        searcher.close();
      }
    }
  }

  @Override
  public long getRowCount() throws IOException {
    IndexSearcherCloseable searcher = getIndexSearcher(false);
    try {
      return getRowCount(searcher);
    } finally {
      if (searcher != null) {
        searcher.close();
      }
    }
  }

  protected long getRowCount(IndexSearcherCloseable searcher) throws IOException {
    TopDocs topDocs = searcher.search(new TermQuery(BlurUtil.PRIME_DOC_TERM), 1);
    return topDocs.totalHits;
  }

  @Override
  public long getIndexMemoryUsage() throws IOException {
    return 0;
  }

  @Override
  public long getSegmentCount() throws IOException {
    IndexSearcherCloseable indexSearcherClosable = getIndexSearcher(false);
    try {
      IndexReader indexReader = indexSearcherClosable.getIndexReader();
      IndexReaderContext context = indexReader.getContext();
      return context.leaves().size();
    } finally {
      indexSearcherClosable.close();
    }
  }

}
