package org.apache.blur;

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

import static org.apache.blur.utils.BlurConstants.BLUR_CONTROLLER_BIND_PORT;
import static org.apache.blur.utils.BlurConstants.BLUR_GUI_CONTROLLER_PORT;
import static org.apache.blur.utils.BlurConstants.BLUR_GUI_SHARD_PORT;
import static org.apache.blur.utils.BlurConstants.BLUR_SHARD_BIND_PORT;
import static org.apache.blur.utils.BlurConstants.BLUR_SHARD_BLOCKCACHE_DIRECT_MEMORY_ALLOCATION;
import static org.apache.blur.utils.BlurConstants.BLUR_SHARD_BLOCKCACHE_SLAB_COUNT;
import static org.apache.blur.utils.BlurConstants.BLUR_SHARD_HOSTNAME;
import static org.apache.blur.utils.BlurConstants.BLUR_SHARD_SAFEMODEDELAY;
import static org.apache.blur.utils.BlurConstants.BLUR_ZOOKEEPER_CONNECTION;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.blur.log.Log;
import org.apache.blur.log.LogFactory;
import org.apache.blur.manager.clusterstatus.ZookeeperPathConstants;
import org.apache.blur.store.buffer.BufferStore;
import org.apache.blur.thirdparty.thrift_0_9_0.TException;
import org.apache.blur.thirdparty.thrift_0_9_0.transport.TTransportException;
import org.apache.blur.thrift.BlurClient;
import org.apache.blur.thrift.BlurClientManager;
import org.apache.blur.thrift.Connection;
import org.apache.blur.thrift.ThriftBlurControllerServer;
import org.apache.blur.thrift.ThriftBlurShardServer;
import org.apache.blur.thrift.ThriftServer;
import org.apache.blur.thrift.generated.Blur.Client;
import org.apache.blur.thrift.generated.Blur.Iface;
import org.apache.blur.thrift.generated.BlurException;
import org.apache.blur.thrift.generated.BlurQuery;
import org.apache.blur.thrift.generated.BlurResults;
import org.apache.blur.thrift.generated.Column;
import org.apache.blur.thrift.generated.Record;
import org.apache.blur.thrift.generated.Row;
import org.apache.blur.thrift.generated.RowMutation;
import org.apache.blur.thrift.generated.TableDescriptor;
import org.apache.blur.thrift.util.BlurThriftHelper;
import org.apache.blur.utils.BlurUtil;
import org.apache.blur.zookeeper.ZooKeeperClient;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;

public abstract class MiniCluster {

  private static Log LOG = LogFactory.getLog(MiniCluster.class);
  private static MiniDFSCluster cluster;
  private static Thread serverThread;
  private static String zkConnectionString = "localhost:21810";
  private static ZooKeeperServerMainEmbedded zooKeeperServerMain;
  private static List<ThriftServer> controllers = new ArrayList<ThriftServer>();
  private static List<ThriftServer> shards = new ArrayList<ThriftServer>();
  private static String controllerConnectionStr;

  public static void main(String[] args) throws IOException, InterruptedException, KeeperException, BlurException,
      TException {
    startDfs("./tmp/hdfs");
    startZooKeeper("./tmp/zk");
    startControllers(1);
    startShards(1);

    // Run the controllers/shards on custom ports.
    // BlurConfiguration conf = new BlurConfiguration(false);
    // conf.setInt(BLUR_CONTROLLER_BIND_PORT, 40001);
    // conf.setInt(BLUR_SHARD_BIND_PORT, 40002);
    // startControllers(conf, 1);
    // startShards(conf, 1);

    try {
      Iface client = BlurClient.getClient(getControllerConnectionStr());
      createTable("test", client);
      long start = System.nanoTime();
      for (int i = 0; i < 1000; i++) {
        long now = System.nanoTime();
        if (start + 5000000000L < now) {
          System.out.println("Total [" + i + "]");
          start = now;
        }
        addRow("test", i, client);
      }

      // This waits for all the data to become visible.
      Thread.sleep(2000);

      for (int i = 0; i < 1000; i++) {
        searchRow("test", i, client);
      }

    } finally {
      stopShards();
      stopControllers();
      shutdownZooKeeper();
      shutdownDfs();
    }
  }

  public static void startBlurCluster(String path, int controllerCount, int shardCount) {
    startDfs(path + "/hdfs");
    startZooKeeper(path + "/zk");
    setupBuffers();
    startControllers(controllerCount);
    startShards(shardCount);
  }

  private static void setupBuffers() {
    BufferStore.init(16, 16);
  }

  public static void shutdownBlurCluster() {
    stopShards();
    stopControllers();
    shutdownZooKeeper();
    shutdownDfs();
  }

  private static void createTable(String test, Iface client) throws BlurException, TException, IOException {
    final TableDescriptor descriptor = new TableDescriptor();
    descriptor.setName(test);
    descriptor.setShardCount(7);
    descriptor.setTableUri(getFileSystemUri() + "/blur/" + test);
    client.createTable(descriptor);
  }

  public static String getControllerConnectionStr() {
    return controllerConnectionStr;
  }

  private static void addRow(String table, int i, Iface client) throws BlurException, TException {
    Row row = new Row();
    row.setId(Integer.toString(i));
    Record record = new Record();
    record.setRecordId(Integer.toString(i));
    record.setFamily("test");
    record.addToColumns(new Column("test", Integer.toString(i)));
    row.addToRecords(record);
    RowMutation rowMutation = BlurUtil.toRowMutation(table, row);
    rowMutation.setWal(false);
    client.mutate(rowMutation);
  }

  private static void searchRow(String table, int i, Iface client) throws BlurException, TException {
    BlurQuery blurQuery = BlurThriftHelper.newSimpleQuery("test.test:" + i);
    System.out.println("Running [" + blurQuery + "]");
    BlurResults results = client.query(table, blurQuery);
    if (results.getTotalResults() != 1L) {
      throw new RuntimeException("we got a problem here.");
    }
  }

  public static void stopControllers() {
    for (ThriftServer s : controllers) {
      s.close();
    }
  }

  public static void stopShards() {
    for (ThriftServer s : shards) {
      s.close();
    }
  }

  public static void startControllers(int num) {
    BlurConfiguration configuration = getBlurConfiguration();
    startControllers(configuration, num);
  }

  private static BlurConfiguration getBlurConfiguration(BlurConfiguration overrides) {
    BlurConfiguration conf = getBlurConfiguration();

    for (Map.Entry<String, String> over : overrides.getProperties().entrySet()) {
      conf.set(over.getKey().toString(), over.getValue().toString());
    }
    return conf;
  }

  private static BlurConfiguration getBlurConfiguration() {
    BlurConfiguration configuration;
    try {
      configuration = new BlurConfiguration();
    } catch (IOException e) {
      LOG.error(e);
      throw new RuntimeException(e);
    }
    configuration.set(BLUR_ZOOKEEPER_CONNECTION, getZkConnectionString());
    configuration.set(BLUR_SHARD_BLOCKCACHE_DIRECT_MEMORY_ALLOCATION, "false");
    configuration.set(BLUR_SHARD_BLOCKCACHE_SLAB_COUNT, "0");
    configuration.setLong(BLUR_SHARD_SAFEMODEDELAY, 5000);
    configuration.setInt(BLUR_GUI_CONTROLLER_PORT, -1);
    configuration.setInt(BLUR_GUI_SHARD_PORT, -1);

    return configuration;
  }

  public static void startControllers(BlurConfiguration configuration, int num) {
    StringBuilder builder = new StringBuilder();
    BlurConfiguration localConf = getBlurConfiguration(configuration);
    int controllerPort = localConf.getInt(BLUR_CONTROLLER_BIND_PORT, 40010);
    for (int i = 0; i < num; i++) {
      try {
        ThriftServer server = ThriftBlurControllerServer.createServer(i, localConf);
        controllers.add(server);
        Connection connection = new Connection("localhost", controllerPort + i);
        if (builder.length() != 0) {
          builder.append(',');
        }
        builder.append(connection.getConnectionStr());
        startServer(server, connection);
      } catch (Exception e) {
        LOG.error(e);
        throw new RuntimeException(e);
      }
    }
    controllerConnectionStr = builder.toString();
  }

  public static void startShards(int num) {
    BlurConfiguration configuration = getBlurConfiguration();
    startShards(configuration, num);
  }

  public static void startShards(final BlurConfiguration configuration, int num) {
    final BlurConfiguration localConf = getBlurConfiguration(configuration);
    ExecutorService executorService = Executors.newFixedThreadPool(num);
    List<Future<ThriftServer>> futures = new ArrayList<Future<ThriftServer>>();
    for (int i = 0; i < num; i++) {
      final int index = i;
      futures.add(executorService.submit(new Callable<ThriftServer>() {
        @Override
        public ThriftServer call() throws Exception {
          return ThriftBlurShardServer.createServer(index, localConf);
        }
      }));
    }
    int shardPort = localConf.getInt(BLUR_SHARD_BIND_PORT, 40020);
    for (int i = 0; i < num; i++) {
      try {
        ThriftServer server = futures.get(i).get();
        shards.add(server);
        Connection connection = new Connection("localhost", shardPort + i);
        startServer(server, connection);
      } catch (Exception e) {
        LOG.error(e);
        throw new RuntimeException(e);
      }
    }
  }

  public static void killShardServer(int shardServer) throws IOException, InterruptedException, KeeperException {
    killShardServer(getBlurConfiguration(), shardServer);
  }

  public static void killShardServer(final BlurConfiguration configuration, int shardServer) throws IOException,
      InterruptedException, KeeperException {
    final BlurConfiguration localConf = getBlurConfiguration(configuration);
    int shardPort = localConf.getInt(BLUR_SHARD_BIND_PORT, 40020);
    String nodeNameHostname = ThriftServer.getNodeName(configuration, BLUR_SHARD_HOSTNAME);
    String nodeName = nodeNameHostname + ":" + (shardPort + shardServer);
    ZooKeeper zk = new ZooKeeperClient(getZkConnectionString(), 30000, new Watcher() {
      @Override
      public void process(WatchedEvent event) {

      }
    });
    String onlineShardsPath = ZookeeperPathConstants
        .getOnlineShardsPath(org.apache.blur.utils.BlurConstants.BLUR_CLUSTER);
    String path = onlineShardsPath + "/" + nodeName;
    zk.delete(path, -1);
    zk.close();
  }

  private static void startServer(final ThriftServer server, Connection connection) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          server.start();
        } catch (TTransportException e) {
          LOG.error(e);
          throw new RuntimeException(e);
        }
      }
    }).start();
    while (true) {
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        return;
      }
      try {
        Client client = BlurClientManager.newClient(connection);
        BlurClientManager.close(client);
        break;
      } catch (TException e) {
        throw new RuntimeException(e);
      } catch (IOException e) {
        LOG.info("Can not connection to [" + connection + "]");
      }
    }
  }

  public static String getZkConnectionString() {
    return zkConnectionString;
  }

  public static void startZooKeeper(String path) {
    startZooKeeper(true, path);
  }

  public static void startZooKeeper(boolean format, String path) {
    Properties properties = new Properties();
    properties.setProperty("tickTime", "2000");
    properties.setProperty("initLimit", "10");
    properties.setProperty("syncLimit", "5");

    properties.setProperty("clientPort", "21810");

    startZooKeeper(properties, format, path);
  }

  public static void startZooKeeper(Properties properties, String path) {
    startZooKeeper(properties, true, path);
  }

  private static class ZooKeeperServerMainEmbedded extends ZooKeeperServerMain {
    @Override
    public void shutdown() {
      super.shutdown();
    }
  }

  public static void startZooKeeper(final Properties properties, boolean format, String path) {
    String realPath = path + "/zk_test";
    properties.setProperty("dataDir", realPath);
    final ServerConfig serverConfig = new ServerConfig();
    QuorumPeerConfig config = new QuorumPeerConfig();
    try {
      config.parseProperties(properties);
    } catch (IOException e) {
      LOG.error(e);
      throw new RuntimeException(e);
    } catch (ConfigException e) {
      LOG.error(e);
      throw new RuntimeException(e);
    }
    serverConfig.readFrom(config);
    rm(new File(realPath));
    serverThread = new Thread(new Runnable() {

      @Override
      public void run() {
        try {
          zooKeeperServerMain = new ZooKeeperServerMainEmbedded();
          zooKeeperServerMain.runFromConfig(serverConfig);
        } catch (IOException e) {
          LOG.error(e);
        }
      }
    });
    serverThread.start();
    long s = System.nanoTime();
    while (s + 10000000000L > System.nanoTime()) {
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        LOG.error(e);
        throw new RuntimeException(e);
      }
      try {
        ZooKeeper zk = new ZooKeeper(zkConnectionString, 30000, new Watcher() {
          @Override
          public void process(WatchedEvent event) {

          }
        });
        zk.close();
        break;
      } catch (IOException e) {
        LOG.error(e);
        throw new RuntimeException(e);
      } catch (InterruptedException e) {
        LOG.error(e);
        throw new RuntimeException(e);
      }
    }
  }

  public static URI getFileSystemUri() throws IOException {
    return cluster.getFileSystem().getUri();
  }

  public static void startDfs(String path) {
    startDfs(true, path);
  }

  public static void startDfs(boolean format, String path) {
    startDfs(new Configuration(), format, path);
  }

  public static void startDfs(Configuration conf, String path) {
    startDfs(conf, true, path);
  }

  public static void startDfs(Configuration conf, boolean format, String path) {
    System.setProperty("test.build.data", path);
    try {
      cluster = new MiniDFSCluster(conf, 1, true, (String[]) null);
      cluster.waitActive();
    } catch (Exception e) {
      LOG.error("error opening file system", e);
      throw new RuntimeException(e);
    }
  }

  public static void shutdownZooKeeper() {
    zooKeeperServerMain.shutdown();
  }

  public static void shutdownDfs() {
    if (cluster != null) {
      LOG.info("Shutting down Mini DFS ");
      try {
        cluster.shutdown();
      } catch (Exception e) {
        // / Can get a java.lang.reflect.UndeclaredThrowableException thrown
        // here because of an InterruptedException. Don't let exceptions in
        // here be cause of test failure.
      }
      try {
        FileSystem fs = cluster.getFileSystem();
        if (fs != null) {
          LOG.info("Shutting down FileSystem");
          fs.close();
        }
        FileSystem.closeAll();
      } catch (IOException e) {
        LOG.error("error closing file system", e);
      }

      // This has got to be one of the worst hacks I have ever had to do.
      // This is needed to shutdown 2 thread pools that are not shutdown by
      // themselves.
      ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
      Thread[] threads = new Thread[100];
      int enumerate = threadGroup.enumerate(threads);
      for (int i = 0; i < enumerate; i++) {
        Thread thread = threads[i];
        if (thread.getName().startsWith("pool")) {
          if (thread.isAlive()) {
            thread.interrupt();
            LOG.info("Stopping ThreadPoolExecutor [" + thread.getName() + "]");
            Object target = getField(thread, "target");
            if (target != null) {
              ThreadPoolExecutor e = (ThreadPoolExecutor) getField(target, "this$0");
              if (e != null) {
                e.shutdownNow();
              }
            }
            try {
              LOG.info("Waiting for thread pool to exit [" + thread.getName() + "]");
              thread.join();
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          }
        }
      }
    }
  }

  private static Object getField(Object o, String fieldName) {
    try {
      Field field = o.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      return field.get(o);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void rm(File file) {
    if (!file.exists()) {
      return;
    }
    if (file.isDirectory()) {
      for (File f : file.listFiles()) {
        rm(f);
      }
    }
    file.delete();
  }

}
