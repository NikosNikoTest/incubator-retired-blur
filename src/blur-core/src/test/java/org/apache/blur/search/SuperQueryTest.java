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

import static junit.framework.Assert.assertEquals;
import static org.apache.blur.lucene.LuceneConstant.LUCENE_VERSION;
import static org.apache.blur.utils.BlurConstants.PRIME_DOC;
import static org.apache.blur.utils.BlurConstants.PRIME_DOC_VALUE;
import static org.apache.blur.utils.BlurConstants.ROW_ID;
import static org.apache.blur.utils.BlurUtil.newColumn;
import static org.apache.blur.utils.BlurUtil.newRecord;
import static org.apache.blur.utils.BlurUtil.newRow;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLongArray;

import org.apache.blur.analysis.BlurAnalyzer;
import org.apache.blur.index.IndexWriter;
import org.apache.blur.lucene.search.FacetQuery;
import org.apache.blur.lucene.search.SuperQuery;
import org.apache.blur.thrift.generated.ScoreType;
import org.apache.blur.utils.RowIndexWriter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;


public class SuperQueryTest {

  private static final String PERSON_NAME = "person.name";
  private static final String ADDRESS_STREET = "address.street";

  private static final String STREET = "street";
  private static final String ADDRESS = "address";
  private static final String PERSON = "person";
  private static final String NAME = "name";

  private static final String NAME1 = "jon";
  private static final String NAME2 = "jane";
  private static final String STREET2 = "main st";
  private static final String STREET1 = "main";

  @Test
  public void testSimpleSuperQuery() throws CorruptIndexException, IOException, InterruptedException {
    BooleanQuery booleanQuery = new BooleanQuery();
    booleanQuery.add(wrapSuper(new TermQuery(new Term(PERSON_NAME, NAME1))), Occur.MUST);
    booleanQuery.add(wrapSuper(new TermQuery(new Term(ADDRESS_STREET, STREET1))), Occur.MUST);

    Directory directory = createIndex();
    IndexReader reader = IndexReader.open(directory);
    printAll(new Term(PERSON_NAME, NAME1), reader);
    printAll(new Term(ADDRESS_STREET, STREET1), reader);
    printAll(new Term(PRIME_DOC, PRIME_DOC_VALUE), reader);
    IndexSearcher searcher = new IndexSearcher(reader);
    TopDocs topDocs = searcher.search(booleanQuery, 10);
    assertEquals(2, topDocs.totalHits);
    assertEquals("1", searcher.doc(topDocs.scoreDocs[0].doc).get(ROW_ID));
    assertEquals("3", searcher.doc(topDocs.scoreDocs[1].doc).get(ROW_ID));
  }

  @Test
  public void testAggregateScoreTypes() throws Exception {
    IndexSearcher searcher = createSearcher();
    BooleanQuery booleanQuery = new BooleanQuery();
    booleanQuery.add(wrapSuper(PERSON_NAME, NAME1, ScoreType.AGGREGATE), Occur.SHOULD);
    booleanQuery.add(wrapSuper(ADDRESS_STREET, STREET1, ScoreType.AGGREGATE), Occur.MUST);
    TopDocs topDocs = searcher.search(booleanQuery, 10);
    printTopDocs(topDocs);
    assertEquals(3, topDocs.totalHits);
    assertEquals(3.30, topDocs.scoreDocs[0].score, 0.01);
    assertEquals(2.20, topDocs.scoreDocs[1].score, 0.01);
    assertEquals(0.55, topDocs.scoreDocs[2].score, 0.01);
  }

  @Test
  public void testBestScoreTypes() throws Exception {
    IndexSearcher searcher = createSearcher();
    BooleanQuery booleanQuery = new BooleanQuery();
    booleanQuery.add(wrapSuper(PERSON_NAME, NAME1, ScoreType.BEST), Occur.SHOULD);
    booleanQuery.add(wrapSuper(ADDRESS_STREET, STREET1, ScoreType.BEST), Occur.MUST);
    TopDocs topDocs = searcher.search(booleanQuery, 10);
    assertEquals(3, topDocs.totalHits);
    printTopDocs(topDocs);
    assertEquals(2.20, topDocs.scoreDocs[0].score, 0.01);
    assertEquals(2.20, topDocs.scoreDocs[1].score, 0.01);
    assertEquals(0.55, topDocs.scoreDocs[2].score, 0.01);
  }

  private void printTopDocs(TopDocs topDocs) {
    for (int i = 0; i < topDocs.totalHits; i++) {
      System.out.println("doc " + i + " score " + topDocs.scoreDocs[i].score);
    }

  }

  @Test
  public void testConstantScoreTypes() throws Exception {
    IndexSearcher searcher = createSearcher();
    BooleanQuery booleanQuery = new BooleanQuery();
    booleanQuery.add(wrapSuper(PERSON_NAME, NAME1, ScoreType.CONSTANT), Occur.SHOULD);
    booleanQuery.add(wrapSuper(ADDRESS_STREET, STREET1, ScoreType.CONSTANT), Occur.MUST);
    TopDocs topDocs = searcher.search(booleanQuery, 10);
    assertEquals(3, topDocs.totalHits);
    printTopDocs(topDocs);
    assertEquals(2.0, topDocs.scoreDocs[0].score, 0.01);
    assertEquals(2.0, topDocs.scoreDocs[1].score, 0.01);
    assertEquals(0.5, topDocs.scoreDocs[2].score, 0.01);
  }

  @Test
  public void testSuperScoreTypes() throws Exception {
    IndexSearcher searcher = createSearcher();
    BooleanQuery booleanQuery = new BooleanQuery();
    booleanQuery.add(wrapSuper(PERSON_NAME, NAME1, ScoreType.SUPER), Occur.SHOULD);
    booleanQuery.add(wrapSuper(ADDRESS_STREET, STREET1, ScoreType.SUPER), Occur.MUST);
    TopDocs topDocs = searcher.search(booleanQuery, 10);
    assertEquals(3, topDocs.totalHits);
    printTopDocs(topDocs);
    assertEquals(3.10, topDocs.scoreDocs[0].score, 0.01);
    assertEquals(3.00, topDocs.scoreDocs[1].score, 0.01);
    assertEquals(0.75, topDocs.scoreDocs[2].score, 0.01);
  }

  @Test
  public void testSuperScoreTypesWithFacet() throws Exception {
    IndexSearcher searcher = createSearcher();
    BooleanQuery booleanQuery = new BooleanQuery();
    booleanQuery.add(wrapSuper(PERSON_NAME, NAME1, ScoreType.SUPER), Occur.SHOULD);
    booleanQuery.add(wrapSuper(ADDRESS_STREET, STREET1, ScoreType.SUPER), Occur.MUST);

    BooleanQuery f1 = new BooleanQuery();
    f1.add(new TermQuery(new Term(PERSON_NAME, NAME1)), Occur.MUST);
    f1.add(new TermQuery(new Term(PERSON_NAME, NAME2)), Occur.MUST);

    Query[] facets = new Query[] { new SuperQuery(f1, ScoreType.CONSTANT) };
    AtomicLongArray counts = new AtomicLongArray(facets.length);
    FacetQuery query = new FacetQuery(booleanQuery, facets, counts);

    TopDocs topDocs = searcher.search(query, 10);
    assertEquals(3, topDocs.totalHits);
    printTopDocs(topDocs);
    assertEquals(3.10, topDocs.scoreDocs[0].score, 0.01);
    assertEquals(3.00, topDocs.scoreDocs[1].score, 0.01);
    assertEquals(0.75, topDocs.scoreDocs[2].score, 0.01);
  }

  private void printAll(Term term, IndexReader reader) throws IOException {
    TermDocs termDocs = reader.termDocs(term);
    while (termDocs.next()) {
      System.out.println(term + "=>" + termDocs.doc());
    }
  }

  private static IndexSearcher createSearcher() throws Exception {
    Directory directory = createIndex();
    IndexReader reader = IndexReader.open(directory);
    return new IndexSearcher(reader);
  }

  public static Directory createIndex() throws CorruptIndexException, LockObtainFailedException, IOException {
    Directory directory = new RAMDirectory();
    IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(LUCENE_VERSION, new StandardAnalyzer(LUCENE_VERSION)));
    BlurAnalyzer analyzer = new BlurAnalyzer(new StandardAnalyzer(LUCENE_VERSION));
    RowIndexWriter indexWriter = new RowIndexWriter(writer, analyzer);
    indexWriter.replace(
        false,
        newRow("1", newRecord(PERSON, UUID.randomUUID().toString(), newColumn(NAME, NAME1)), newRecord(PERSON, UUID.randomUUID().toString(), newColumn(NAME, NAME1)),
            newRecord(ADDRESS, UUID.randomUUID().toString(), newColumn(STREET, STREET1))));
    indexWriter.replace(false,
        newRow("2", newRecord(PERSON, UUID.randomUUID().toString(), newColumn(NAME, NAME2)), newRecord(ADDRESS, UUID.randomUUID().toString(), newColumn(STREET, STREET1))));
    indexWriter.replace(false,
        newRow("3", newRecord(PERSON, UUID.randomUUID().toString(), newColumn(NAME, NAME1)), newRecord(ADDRESS, UUID.randomUUID().toString(), newColumn(STREET, STREET2))));
    ;
    writer.close();
    return directory;
  }

  private Query wrapSuper(Query query) {
    return new SuperQuery(query, ScoreType.AGGREGATE);
  }

  private Query wrapSuper(String field, String value, ScoreType scoreType) {
    return new SuperQuery(new TermQuery(new Term(field, value)), scoreType);
  }

}
