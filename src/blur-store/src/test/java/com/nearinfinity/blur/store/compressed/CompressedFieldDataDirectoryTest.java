package com.nearinfinity.blur.store.compressed;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
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
import static com.nearinfinity.blur.lucene.LuceneConstant.LUCENE_VERSION;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;

public class CompressedFieldDataDirectoryTest {

  private static final CompressionCodec COMPRESSION_CODEC = CompressedFieldDataDirectory.DEFAULT_COMPRESSION;

  @Test
  public void testCompressedFieldDataDirectoryBasic() throws CorruptIndexException, IOException {
    RAMDirectory dir = new RAMDirectory();
    CompressedFieldDataDirectory directory = new CompressedFieldDataDirectory(dir, COMPRESSION_CODEC);
    IndexWriterConfig config = new IndexWriterConfig(LUCENE_VERSION, new KeywordAnalyzer());
    TieredMergePolicy mergePolicy = (TieredMergePolicy) config.getMergePolicy();
    mergePolicy.setUseCompoundFile(false);
    IndexWriter writer = new IndexWriter(directory, config);
    addDocs(writer, 0, 10);
    writer.close();
    testFetches(directory);
  }

  @Test
  public void testCompressedFieldDataDirectoryTransition() throws CorruptIndexException, LockObtainFailedException, IOException {
    RAMDirectory dir = new RAMDirectory();

    IndexWriterConfig config = new IndexWriterConfig(LUCENE_VERSION, new KeywordAnalyzer());
    TieredMergePolicy mergePolicy = (TieredMergePolicy) config.getMergePolicy();
    mergePolicy.setUseCompoundFile(false);
    IndexWriter writer = new IndexWriter(dir, config);

    addDocs(writer, 0, 5);
    writer.close();

    CompressedFieldDataDirectory directory = new CompressedFieldDataDirectory(dir, COMPRESSION_CODEC);
    config = new IndexWriterConfig(LUCENE_VERSION, new KeywordAnalyzer());
    mergePolicy = (TieredMergePolicy) config.getMergePolicy();
    mergePolicy.setUseCompoundFile(false);
    writer = new IndexWriter(directory, config);
    addDocs(writer, 5, 5);
    writer.close();
    testFetches(directory);
  }

  @Test
  public void testCompressedFieldDataDirectoryMixedBlockSize() throws CorruptIndexException, LockObtainFailedException, IOException {
    RAMDirectory dir = new RAMDirectory();
    IndexWriterConfig config = new IndexWriterConfig(LUCENE_VERSION, new KeywordAnalyzer());
    TieredMergePolicy mergePolicy = (TieredMergePolicy) config.getMergePolicy();
    mergePolicy.setUseCompoundFile(false);
    IndexWriter writer = new IndexWriter(dir, config);
    addDocs(writer, 0, 5);
    writer.close();

    CompressedFieldDataDirectory directory1 = new CompressedFieldDataDirectory(dir, COMPRESSION_CODEC, 2);
    config = new IndexWriterConfig(LUCENE_VERSION, new KeywordAnalyzer());
    mergePolicy = (TieredMergePolicy) config.getMergePolicy();
    mergePolicy.setUseCompoundFile(false);
    writer = new IndexWriter(directory1, config);
    addDocs(writer, 5, 2);
    writer.close();

    CompressedFieldDataDirectory directory2 = new CompressedFieldDataDirectory(dir, COMPRESSION_CODEC, 4);
    config = new IndexWriterConfig(LUCENE_VERSION, new KeywordAnalyzer());
    mergePolicy = (TieredMergePolicy) config.getMergePolicy();
    mergePolicy.setUseCompoundFile(false);
    writer = new IndexWriter(directory2, config);
    addDocs(writer, 7, 3);
    writer.close();
    testFetches(directory2);
    testFileLengths(directory2);
  }

  private void testFileLengths(Directory dir) throws IOException {
    String[] listAll = dir.listAll();
    for (String name : listAll) {
      IndexInput input = dir.openInput(name);
      assertEquals(input.length(), dir.fileLength(name));
      input.close();
    }

  }

  private void testFetches(Directory directory) throws CorruptIndexException, IOException {
    IndexReader reader = IndexReader.open(directory);
    for (int i = 0; i < reader.maxDoc(); i++) {
      String id = Integer.toString(i);
      Document document = reader.document(i);
      assertEquals(id, document.get("id"));
    }
  }

  private void addDocs(IndexWriter writer, int starting, int amount) throws CorruptIndexException, IOException {
    for (int i = 0; i < amount; i++) {
      int index = starting + i;
      writer.addDocument(getDoc(index));
    }
  }

  private Document getDoc(int index) {
    Document document = new Document();
    document.add(new Field("id", Integer.toString(index), Store.YES, Index.NOT_ANALYZED_NO_NORMS));
    return document;
  }

}
