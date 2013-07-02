package org.apache.blur.manager.results;

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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.blur.log.Log;
import org.apache.blur.log.LogFactory;
import org.apache.blur.thrift.generated.BlurException;
import org.apache.blur.thrift.generated.BlurQuery;
import org.apache.blur.thrift.generated.ErrorType;
import org.apache.blur.utils.BlurExecutorCompletionService;
import org.apache.blur.utils.ForkJoin.Merger;

public class MergerBlurResultIterable implements Merger<BlurResultIterable> {

  private static Log LOG = LogFactory.getLog(MergerBlurResultIterable.class);

  private long _minimumNumberOfResults;
  private long _maxQueryTime;
  private BlurQuery _blurQuery;

  public MergerBlurResultIterable(BlurQuery blurQuery) {
    _blurQuery = blurQuery;
    _minimumNumberOfResults = blurQuery.minimumNumberOfResults;
    _maxQueryTime = blurQuery.maxQueryTime;
  }

  @Override
  public BlurResultIterable merge(BlurExecutorCompletionService<BlurResultIterable> service) throws BlurException {
    BlurResultIterableMultiple iterable = new BlurResultIterableMultiple();
    while (service.getRemainingCount() > 0) {
      Future<BlurResultIterable> future = service.poll(_maxQueryTime, TimeUnit.MILLISECONDS, true, _blurQuery);
      if (future != null) {
        BlurResultIterable blurResultIterable = service.getResultThrowException(future, _blurQuery);
        iterable.addBlurResultIterable(blurResultIterable);
        if (iterable.getTotalResults() >= _minimumNumberOfResults) {
          // Called to stop execution of any other running queries.
          service.cancelAll();
          return iterable;
        }
      } else {
        LOG.info("Query timeout with max query time of [{2}] for query [{1}].", _maxQueryTime, _blurQuery);
        throw new BlurException("Query timeout with max query time of [" + _maxQueryTime + "] for query [" + _blurQuery
            + "].", null, ErrorType.QUERY_TIMEOUT);
      }
    }
    return iterable;
  }

}
