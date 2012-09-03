package com.nearinfinity.blur.utils;

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
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReader.ReaderClosedListener;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.util.OpenBitSet;

import com.nearinfinity.blur.log.Log;
import com.nearinfinity.blur.log.LogFactory;

public class PrimeDocCache {

  private static final Log LOG = LogFactory.getLog(PrimeDocCache.class);

  public static final OpenBitSet EMPTY_BIT_SET = new OpenBitSet();

  private static Map<Object, OpenBitSet> primeDocMap = new ConcurrentHashMap<Object, OpenBitSet>();

  /**
   * The way this method is called via warm up methods the likelihood of
   * creating multiple bitsets during a race condition is very low, that's why
   * this method is not synced.
   */
  public static OpenBitSet getPrimeDocBitSet(IndexReader reader) throws IOException {
    Object key = reader.getCoreCacheKey();
    OpenBitSet bitSet = primeDocMap.get(key);
    if (bitSet == null) {
      reader.addReaderClosedListener(new ReaderClosedListener() {
        @Override
        public void onClose(IndexReader reader) {
          Object key = reader.getCoreCacheKey();
          LOG.debug("Current size [" + primeDocMap.size() + "] Prime Doc BitSet removing for segment [" + reader + "]");
          primeDocMap.remove(key);
        }
      });
      LOG.debug("Prime Doc BitSet missing for segment [" + reader + "] current size [" + primeDocMap.size() + "]");
      bitSet = new OpenBitSet(reader.maxDoc());
      primeDocMap.put(key, bitSet);
      TermDocs termDocs = reader.termDocs(BlurConstants.PRIME_DOC_TERM);
      while (termDocs.next()) {
        bitSet.set(termDocs.doc());
      }
      termDocs.close();
    }
    return bitSet;
  }

}
