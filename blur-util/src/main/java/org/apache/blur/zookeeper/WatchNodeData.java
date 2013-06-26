package org.apache.blur.zookeeper;

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
import java.io.Closeable;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.blur.log.Log;
import org.apache.blur.log.LogFactory;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Stat;

public class WatchNodeData implements Closeable {

  private final static Log LOG = LogFactory.getLog(WatchNodeData.class);
  private final ZooKeeper _zooKeeper;
  private final String _path;
  private final Object _lock = new Object();
  private final AtomicBoolean _running = new AtomicBoolean(true);
  private long _delay = TimeUnit.SECONDS.toMillis(3);
  private byte[] _data;
  private final String instance = UUID.randomUUID().toString();
  private Thread _doubleCheckThread;
  private Thread _watchThread;

  public static abstract class OnChange {
    public abstract void action(byte[] data);
  }

  public WatchNodeData(ZooKeeper zooKeeper, String path) {
    _zooKeeper = zooKeeper;
    _path = path;
    LOG.debug("Creating watch [{0}]", instance);
  }

  public WatchNodeData watch(final OnChange onChange) {
    _watchThread = new Thread(new Runnable() {

      @Override
      public void run() {
        Watcher watcher = new Watcher() {
          @Override
          public void process(WatchedEvent event) {
            synchronized (_lock) {
              _lock.notify();
            }
          }
        };
        startDoubleCheckThread();
        while (_running.get()) {
          synchronized (_lock) {
            try {
              Stat stat = _zooKeeper.exists(_path, false);
              if (stat == null) {
                LOG.debug("Path [{0}] not found.", _path);
                return;
              }
              byte[] data = _zooKeeper.getData(_path, watcher, stat);
              try {
                onChange.action(data);
                _data = data;
              } catch (Throwable t) {
                LOG.error("Unknown error during onchange action [" + this + "].", t);
              }
              _lock.wait();
            } catch (KeeperException e) {
              if (!_running.get()) {
                LOG.info("Error [{0}]", e.getMessage());
                return;
              }
              LOG.error("Unknown error", e);
              throw new RuntimeException(e);
            } catch (InterruptedException e) {
              return;
            }
          }
        }
      }
    });
    _watchThread.setName("Watch Data [" + _path + "][" + instance + "]");
    _watchThread.setDaemon(true);
    _watchThread.start();
    return this;
  }

  private void startDoubleCheckThread() {
    _doubleCheckThread = new Thread(new Runnable() {

      @Override
      public void run() {
        while (_running.get()) {
          try {
            synchronized (_running) {
              _running.wait(_delay);
            }
            if (!_running.get()) {
              return;
            }
            Stat stat = _zooKeeper.exists(_path, false);
            if (stat == null) {
              LOG.debug("Path [{0}] not found.", _path);
              synchronized (_lock) {
                _lock.notify();
              }
              return;
            }

            byte[] data = _zooKeeper.getData(_path, false, stat);
            if (!isCorrect(data)) {
              LOG.debug("Double check triggered for [" + _path + "]");
              synchronized (_lock) {
                _lock.notify();
              }
            }
          } catch (KeeperException e) {
            if (!_running.get()) {
              LOG.info("Error [{0}]", e.getMessage());
              return;
            }
            if (e.code() == Code.SESSIONEXPIRED) {
              LOG.warn("Session expired for [" + _path + "] [" + instance + "]");
              return;
            }
            LOG.error("Unknown error", e);
            throw new RuntimeException(e);
          } catch (InterruptedException e) {
            return;
          }
        }
      }
    });
    _doubleCheckThread.setName("Poll Watch Data [" + _path + "][" + instance + "]");
    _doubleCheckThread.setDaemon(true);
    _doubleCheckThread.start();
  }

  protected boolean isCorrect(byte[] data) {
    if (data == null && _data == null) {
      return true;
    }
    if (data == null || _data == null) {
      return false;
    }
    return Arrays.equals(data, _data);
  }

  public void close() {
    if (_running.get()) {
      LOG.debug("Closing [{0}]", instance);
      _running.set(false);
      if (_doubleCheckThread != null) {
        _doubleCheckThread.interrupt();
      }
      if (_watchThread != null) {
        _watchThread.interrupt();
      }
    }
  }

}
