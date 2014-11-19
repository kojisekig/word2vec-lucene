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

import com.rondhuit.w2v.TextFileConfig;
import com.rondhuit.w2v.TextFileCorpusFactory;
import com.rondhuit.w2v.Word2vec;

public class TextFileCreateVectors extends AbstractCreateVectors {

  @Override
  protected void localUsage(){
    paramDesc("-input <file>", "Use text data from <file> to train the model");
    System.err.printf("\nExamples:\n");
    System.err.printf("java %s -input data.txt -output vec.txt -size 200 -window 5 -sample 0.0001 -negative 5 -hs 0 -binary -cbow -iter 3\n\n",
        TextFileCreateVectors.class.getName());
  }
  
  void execute(String[] args) throws IOException {
    if(args.length <= 1) usage();
    
    TextFileConfig config = new TextFileConfig();

    setConfig(args, config);
    int i;
    if((i = argPos("-input", args)) >= 0) config.setInputFile(args[i + 1]);
    
    Word2vec w2v = new Word2vec(config);
    System.err.printf("Starting training using text file %s\n", config.getInputFile());
    w2v.trainModel(new TextFileCorpusFactory());
  }

  public static void main(String[] args) throws IOException {
    new TextFileCreateVectors().execute(args);
  }
}
