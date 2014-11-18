/*
 *  Copyright (c) 2014 RONDHUIT Co.,Ltd.
 *  
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.rondhuit.w2v.lucene;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import com.rondhuit.w2v.Corpus;

public class LuceneIndexCorpus extends Corpus {

  private IndexReader reader;
  private final String field;
  private TopDocs topDocs;
  private final Analyzer analyzer;
  int tdPos;

  public LuceneIndexCorpus(Config config) throws IOException {
    super(config);

    field = config.getField();
    analyzer = loadAnalyzer(config.getAnalyzer());
    Directory dir = FSDirectory.open(new File(config.getIndexDir()));
    reader = DirectoryReader.open(dir);
  }

  static Analyzer loadAnalyzer(String fqcn){
    try {
      return (Analyzer)Class.forName(fqcn).newInstance();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public LuceneIndexCorpus(Corpus cloneSrc) throws IOException {
    super(cloneSrc);

    LuceneIndexCorpus lic = (LuceneIndexCorpus)cloneSrc;

    config = lic.config;
    reader = lic.reader;
    field = lic.field;
    topDocs = lic.topDocs;
    analyzer = loadAnalyzer(config.getAnalyzer());
  }

  @Override
  public void learnVocab() throws IOException {
    super.learnVocab();

    final String field = config.getField();
    final Terms terms = MultiFields.getTerms(reader, field);
    final BytesRef maxTerm = terms.getMax();
    final BytesRef minTerm = terms.getMin();
    Query q = new TermRangeQuery(field, minTerm, maxTerm, true, true);
    IndexSearcher searcher = new IndexSearcher(reader);
    topDocs = searcher.search(q, Integer.MAX_VALUE);

    TermsEnum termsEnum = null;
    termsEnum = terms.iterator(termsEnum);

    termsEnum.seekCeil(new BytesRef());
    BytesRef term = termsEnum.term();
    while(term != null){
      int p = addWordToVocab(term.utf8ToString());
      vocab[p].setCn((int)termsEnum.totalTermFreq());
      term = termsEnum.next();
    }
  }

  TokenStream tokenStream = null;
  CharTermAttribute termAtt = null;
  String[] values = new String[]{};
  int valPos = 0;

  @Override
  public void rewind(int numThreads, int id) {
    super.rewind(numThreads, id);
    tdPos = topDocs.totalHits / numThreads * id;
  }

  @Override
  public String nextWord() throws IOException {
    
    while(true){
      // check the tokenStream first
      if(tokenStream != null && tokenStream.incrementToken()){
        return new String(termAtt.buffer(), 0, termAtt.length());
      }

      if(tokenStream != null)
        tokenStream.close();
      if(valPos < values.length){
        tokenStream = analyzer.tokenStream(field, values[valPos++]);
        termAtt = tokenStream.getAttribute(CharTermAttribute.class);
        tokenStream.reset();
        eoc = false;
        return null;
      }
      else{
        if(tdPos >= topDocs.totalHits){
          tokenStream = null;
          eoc = true;
          return null;   // end of index == end of corpus
        }
        Document doc = reader.document(topDocs.scoreDocs[tdPos++].doc);
        values = doc.getValues(field);   // This method returns an empty array when there are no matching fields.
                                         // It never returns null.
        valPos = 0;
        tokenStream = null;
      }
    }
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }
}
