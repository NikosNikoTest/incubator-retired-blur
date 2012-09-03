package org.apache.blur.search;

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

import static junit.framework.Assert.assertTrue;
import static org.apache.blur.lucene.LuceneConstant.LUCENE_VERSION;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import org.apache.blur.analysis.BlurAnalyzer;
import org.apache.blur.index.IndexWriter;
import org.apache.blur.lucene.search.SuperParser;
import org.apache.blur.thrift.generated.Column;
import org.apache.blur.thrift.generated.Record;
import org.apache.blur.thrift.generated.Row;
import org.apache.blur.thrift.generated.ScoreType;
import org.apache.blur.utils.RowIndexWriter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;


public class RandomSuperQueryTest {

  private static final int MOD_COLS_USED_FOR_SKIPPING = 3;
  private static final int MAX_NUM_OF_DOCS = 10000;// 10000
  private static final int MIN_NUM_COL_FAM = 3;// 3
  private static final int MAX_NUM_COL_FAM = 20;// 20
  private static final int MAX_NUM_DOCS_PER_COL_FAM = 25;// 25
  private static final int MAX_NUM_COLS = 21;// 21
  private static final int MIN_NUM_COLS = 3;// 3
  private static final int MAX_NUM_OF_WORDS = 1000;
  private static final int MOD_USED_FOR_SAMPLING = 1;//

  private Random seedGen = new Random(1);

  @Test
  public void testRandomSuperQuery() throws CorruptIndexException, IOException, InterruptedException, ParseException {
    long seed = seedGen.nextLong();

    Filter filter = new QueryWrapperFilter(new MatchAllDocsQuery());

    Random random = new Random(seed);
    Collection<String> sampler = new HashSet<String>();
    System.out.print("Creating index... ");
    System.out.flush();
    Directory directory = createIndex(random, sampler);
    IndexReader reader = IndexReader.open(directory);
    System.out.print("Running searches [" + sampler.size() + "]... ");
    System.out.flush();
    assertTrue(!sampler.isEmpty());
    IndexSearcher searcher = new IndexSearcher(reader);
    long s = System.currentTimeMillis();
    for (String str : sampler) {
      Query query = new SuperParser(LUCENE_VERSION, new StandardAnalyzer(LUCENE_VERSION), true, filter, ScoreType.AGGREGATE).parse(str);
      TopDocs topDocs = searcher.search(query, 10);
      assertTrue("seed [" + seed + "] {" + query + "} {" + s + "}", topDocs.totalHits > 0);
    }
    long e = System.currentTimeMillis();
    System.out.println("Finished in [" + (e - s) + "] ms");
  }

  private Directory createIndex(Random random, Collection<String> sampler) throws CorruptIndexException, LockObtainFailedException, IOException {
    Directory directory = new RAMDirectory();
    String[] columnFamilies = genWords(random, MIN_NUM_COL_FAM, MAX_NUM_COL_FAM, "colfam");
    Map<String, String[]> columns = new HashMap<String, String[]>();
    for (int i = 0; i < columnFamilies.length; i++) {
      columns.put(columnFamilies[i], genWords(random, MIN_NUM_COLS, MAX_NUM_COLS, "col"));
    }
    IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(LUCENE_VERSION, new StandardAnalyzer(LUCENE_VERSION)));
    RowIndexWriter indexWriter = new RowIndexWriter(writer, new BlurAnalyzer(new StandardAnalyzer(LUCENE_VERSION)));
    int numberOfDocs = random.nextInt(MAX_NUM_OF_DOCS) + 1;
    for (int i = 0; i < numberOfDocs; i++) {
      indexWriter.replace(false, generatSuperDoc(random, columns, sampler));
    }
    writer.close();
    return directory;
  }

  private String[] genWords(Random random, int min, int max, String prefix) {
    int numberOfColFam = random.nextInt(max - min) + min;
    String[] str = new String[numberOfColFam];
    for (int i = 0; i < numberOfColFam; i++) {
      str[i] = genWord(random, prefix);
    }
    return str;
  }

  private Row generatSuperDoc(Random random, Map<String, String[]> columns, Collection<String> sampler) {
    Row row = new Row().setId(Long.toString(random.nextLong()));
    StringBuilder builder = new StringBuilder();
    for (String colFam : columns.keySet()) {
      String[] cols = columns.get(colFam);
      for (int i = 0; i < random.nextInt(MAX_NUM_DOCS_PER_COL_FAM); i++) {
        Record record = new Record();
        record.setFamily(colFam);
        record.setRecordId(Long.toString(random.nextLong()));
        int staringLength = builder.length();
        for (String column : cols) {
          if (random.nextInt() % MOD_COLS_USED_FOR_SKIPPING == 0) {
            String word = genWord(random, "word");
            record.addToColumns(new Column(column, word));
            if (random.nextInt() % MOD_USED_FOR_SAMPLING == 0) {
              builder.append(" +" + colFam + "." + column + ":" + word);
            }
          }
        }
        if (builder.length() != staringLength) {
          builder.append(" nojoin.nojoin ");
        }
        row.addToRecords(record);
      }
    }
    String string = builder.toString().trim();
    if (!string.isEmpty()) {
      sampler.add(string);
    }
    return row;
  }

  private String genWord(Random random, String prefix) {
    return prefix + random.nextInt(MAX_NUM_OF_WORDS);
  }
}
