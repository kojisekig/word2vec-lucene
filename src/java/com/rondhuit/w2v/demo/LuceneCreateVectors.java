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

package com.rondhuit.w2v.demo;

import java.io.IOException;

import com.rondhuit.w2v.Word2vec;
import com.rondhuit.w2v.lucene.LuceneIndexConfig;
import com.rondhuit.w2v.lucene.LuceneIndexCorpusFactory;

public class LuceneCreateVectors extends AbstractCreateVectors {

  @Override
  protected void localUsage(){
    paramDesc("-index <dir>", "Set Lucene index <dir> to train the model");
    paramDesc("-analyzer <Lucene Analyzer class>", "Set Lucene Analyzer class name; default is org.apache.lucene.analysis.core.WhitespaceAnalyzer");
    paramDesc("-field <field name>", "Set Lucene field name to be analyzed");
    System.err.printf("\nExamples:\n");
    System.err.printf("java %s -index index -output vec.txt -size 200 -window 5 -sample 0.0001 -negative 5 -hs 0 -binary -cbow -iter 3\n\n",
        LuceneCreateVectors.class.getName());
  }
  
  void execute(String[] args) throws IOException {
    if(args.length <= 1) usage();
    
    LuceneIndexConfig config = new LuceneIndexConfig();

    setConfig(args, config);
    int i;
    if((i = argPos("-index", args)) >= 0) config.setIndexDir(args[i + 1]);
    if((i = argPos("-analyzer", args)) >= 0) config.setAnalyzer(args[i + 1]);
    if((i = argPos("-field", args)) >= 0) config.setField(args[i + 1]);
    
    Word2vec w2v = new Word2vec(config);
    System.err.printf("Starting training using Lucene index %s\n", config.getIndexDir());
    w2v.trainModel(new LuceneIndexCorpusFactory());
  }

  public static void main(String[] args) throws IOException {
    new LuceneCreateVectors().execute(args);
  }
}
