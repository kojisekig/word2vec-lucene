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
import com.rondhuit.w2v.lucene.Config;
import com.rondhuit.w2v.lucene.LuceneIndexCorpusFactory;

public class LuceneCreateVectors {
  
  static void usage(){
    System.err.printf("WORD VECTOR estimation toolkit v 0.1c\n\n");
    System.err.printf("Options:\n");
    System.err.printf("Parameters for training:\n");
    System.err.printf("\t-index <dir>\n");
    System.err.printf("\t\tSet Lucene index <dir> to train the model\n");
    System.err.printf("\t-output <file>\n");
    System.err.printf("\t\tUse <file> to save the resulting word vectors / word clusters\n");
    System.err.printf("\t-analyzer <Lucene Analyzer class>\n");
    System.err.printf("\t\tSet Lucene Analyzer class name; default is org.apache.lucene.analysis.core.WhitespaceAnalyzer\n");
    System.err.printf("\t-field <field name>\n");
    System.err.printf("\t\tSet Lucene field name to be analyzed\n");
    System.err.printf("\t-size <int>\n");
    System.err.printf("\t\tSet size of word vectors; default is 100\n");
    System.err.printf("\t-window <int>\n");
    System.err.printf("\t\tSet max skip length between words; default is 5\n");
    System.err.printf("\t-sample <float>\n");
    System.err.printf("\t\tSet threshold for occurrence of words. Those that appear with higher frequency in the training data\n");
    System.err.printf("\t\twill be randomly down-sampled; default is 0.001, useful range is (0, 0.00001)\n");
    System.err.printf("\t-hs\n");
    System.err.printf("\t\tUse Hierarchical Softmax; default is not used\n");
    System.err.printf("\t-negative <int>\n");
    System.err.printf("\t\tNumber of negative examples; default is 5, common values are 3 - 10 (0 = not used)\n");
    System.err.printf("\t-threads <int>\n");
    System.err.printf("\t\tUse <int> threads (default 12)\n");
    System.err.printf("\t-iter <int>\n");
    System.err.printf("\t\tRun more training iterations (default 5)\n");
    System.err.printf("\t-min-count <int>\n");
    System.err.printf("\t\tThis will discard words that appear less than <int> times; default is 5\n");
    System.err.printf("\t-alpha <float>\n");
    System.err.printf("\t\tSet the starting learning rate; default is 0.025 for skip-gram and 0.05 for CBOW\n");
    System.err.printf("\t-classes <int>\n");
    System.err.printf("\t\tOutput word classes rather than word vectors; default number of classes is 0 (vectors are written)\n");
    System.err.printf("\t-cbow\n");
    System.err.printf("\t\tUse the continuous bag of words model; default is skip-gram model\n");
    System.err.printf("\nExamples:\n");
    System.err.printf("./word2vec -index index -output vec.txt -size 200 -window 5 -sample 0.0001 -negative 5 -hs 0 -binary -cbow -iter 3\n\n");
    System.exit(0);
  }
  
  static int argPos(String param, String[] args){
    return argPos(param, args, true);
  }

  static int argPos(String param, String[] args, boolean checkArgNum){
    for(int i = 0; i < args.length; i++){
      if(param.equals(args[i])){
        if(checkArgNum && (i == args.length - 1))
          throw new IllegalArgumentException(String.format("Argument missing for %s", param));
        return i;
      }
    }
    return -1;
  }

  public static void main(String[] args) throws IOException {
    if(args.length <= 1) usage();
    
    Config config = new Config();

    int i;
    if((i = argPos("-size", args)) >= 0) config.setLayer1Size(Integer.parseInt(args[i + 1]));
    if((i = argPos("-index", args)) >= 0) config.setIndexDir(args[i + 1]);
    if((i = argPos("-output", args)) >= 0) config.setOutputFile(args[i + 1]);
    if((i = argPos("-analyzer", args)) >= 0) config.setAnalyzer(args[i + 1]);
    if((i = argPos("-field", args)) >= 0) config.setField(args[i + 1]);
    if((i = argPos("-cbow", args)) >= 0) config.setUseContinuousBagOfWords(true);
    if(config.useContinuousBagOfWords()) config.setAlpha(0.05f);
    if((i = argPos("-alpha", args)) >= 0) config.setAlpha(Float.parseFloat(args[i + 1]));
    if((i = argPos("-window", args)) >= 0) config.setWindow(Integer.parseInt(args[i + 1]));
    if((i = argPos("-sample", args)) >= 0) config.setSample(Float.parseFloat(args[i + 1]));
    if((i = argPos("-hs", args)) >= 0) config.setUseHierarchicalSoftmax(true);
    if((i = argPos("-negative", args)) >= 0) config.setNegative(Integer.parseInt(args[i + 1]));
    if((i = argPos("-threads", args)) >= 0) config.setNumThreads(Integer.parseInt(args[i + 1]));
    if((i = argPos("-iter", args)) >= 0) config.setIter(Integer.parseInt(args[i + 1]));
    if((i = argPos("-min-count", args)) >= 0) config.setMinCount(Integer.parseInt(args[i + 1]));
    if((i = argPos("-classes", args)) >= 0) config.setClasses(Integer.parseInt(args[i + 1]));

    
    Word2vec w2v = new Word2vec(config);
    w2v.trainModel(new LuceneIndexCorpusFactory());
  }
}
