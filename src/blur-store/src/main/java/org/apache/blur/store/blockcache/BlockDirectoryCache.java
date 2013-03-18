package org.apache.blur.store.blockcache;

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
import static org.apache.blur.metrics.MetricsConstants.CACHE;
import static org.apache.blur.metrics.MetricsConstants.HIT;
import static org.apache.blur.metrics.MetricsConstants.MISS;
import static org.apache.blur.metrics.MetricsConstants.ORG_APACHE_BLUR;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.MetricName;

public class BlockDirectoryCache implements Cache {

  private BlockCache _blockCache;
  private AtomicInteger _counter = new AtomicInteger();
  private Map<String, Integer> _names = new ConcurrentHashMap<String, Integer>();
  private Meter hits;
  private Meter misses;

  public BlockDirectoryCache(BlockCache blockCache) {
    _blockCache = blockCache;
    hits = Metrics.newMeter(new MetricName(ORG_APACHE_BLUR, CACHE, HIT), HIT, TimeUnit.SECONDS);
    misses = Metrics.newMeter(new MetricName(ORG_APACHE_BLUR, CACHE, MISS), MISS, TimeUnit.SECONDS);
  }

  @Override
  public void delete(String name) {
    _names.remove(name);
  }

  @Override
  public void update(String name, long blockId, int blockOffset, byte[] buffer, int offset, int length) {
    Integer file = _names.get(name);
    if (file == null) {
      file = _counter.incrementAndGet();
      _names.put(name, file);
    }
    BlockCacheKey blockCacheKey = new BlockCacheKey();
    blockCacheKey.setBlock(blockId);
    blockCacheKey.setFile(file);
    _blockCache.store(blockCacheKey, blockOffset, buffer, offset, length);
  }

  @Override
  public boolean fetch(String name, long blockId, int blockOffset, byte[] b, int off, int lengthToReadInBlock) {
    Integer file = _names.get(name);
    if (file == null) {
      return false;
    }
    BlockCacheKey blockCacheKey = new BlockCacheKey();
    blockCacheKey.setBlock(blockId);
    blockCacheKey.setFile(file);
    boolean fetch = _blockCache.fetch(blockCacheKey, b, blockOffset, off, lengthToReadInBlock);
    if (fetch) {
      hits.mark();
    } else {
      misses.mark();
    }
    return fetch;
  }

  @Override
  public long size() {
    return _blockCache.getSize();
  }

  @Override
  public void renameCacheFile(String source, String dest) {
    Integer file = _names.remove(source);
    if (file != null) {
      _names.put(dest, file);
    }
  }
}
