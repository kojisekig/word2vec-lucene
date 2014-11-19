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

package com.rondhuit.w2v;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.rondhuit.commons.IOUtils;

public class TextFileCorpus extends Corpus {

  static final int VOCAB_MAX_SIZE      = 30000000;
  
  private int minReduce = 1;
  private long trainFileSize;
  private RandomAccessFile raf = null;

  public TextFileCorpus(Config config) throws IOException {
    super(config);
  }

  public TextFileCorpus(Corpus cloneSrc) throws IOException {
    super(cloneSrc);
    
    TextFileConfig tfc = (TextFileConfig)cloneSrc.config;
    
    this.trainFileSize = ((TextFileCorpus)cloneSrc).trainFileSize;
    raf = new RandomAccessFile(tfc.getInputFile(), "r");
  }
  
  @Override
  public void shutdown() throws IOException {
    IOUtils.closeQuietly(raf);
  }

  @Override
  public void rewind(int numThreads, int id) throws IOException {
    super.rewind(numThreads, id);
    raf.seek(trainFileSize / numThreads * id);
  }

  @Override
  public String nextWord() throws IOException {
    return readWord(raf);
  }

  /**
   * Reduces the vocabulary by removing infrequent tokens
   */
  void reduceVocab(){
    int j = 0;
    for(int i = 0; i < vocabSize; i++){
      if(vocab[i].cn > minReduce){
        vocab[j].cn = vocab[i].cn;
        vocab[j].word = vocab[i].word;
        j++;
      }
    }
    vocabSize = j;
    vocabIndexMap.clear();
    for(int i = 0; i < vocabSize; i++){
      vocabIndexMap.put(vocab[i].word, i);
    }
    minReduce++;
  }
  
  @Override
  public void learnVocab() throws IOException {
    super.learnVocab();

    final String trainFile = ((TextFileConfig)config).getInputFile();
    trainFileSize = new File(trainFile).length();

    RandomAccessFile raf = null;
    vocabSize = 0;
    try{
      raf = new RandomAccessFile(trainFile, "r");
      while(true){
        String word = readWord(raf);
        if(word == null && eoc) break;
        trainWords++;
        if (trainWords % 100000 == 0) {
          System.err.printf("%dK%c", trainWords / 1000, 13);
        }
        int idx = searchVocab(word);
        if(idx == -1) {
          int p = addWordToVocab(word);
          vocab[p].cn = 1;
        } else vocab[idx].cn++;
        if (vocabSize > VOCAB_MAX_SIZE * 0.7)
          reduceVocab();
      }
    }
    finally {
      IOUtils.closeQuietly(raf);
    }
  }
  
  String[] wordsBuffer = new String[0];
  int wbp = wordsBuffer.length;

  /**
   * Reads a single word from a file, assuming space + tab + EOL to be word boundaries
   * @param br
   * @return null if EOF
   * @throws IOException
   */
  String readWord(RandomAccessFile raf) throws IOException {
    while(true){
      // check the buffer first
      if(wbp < wordsBuffer.length){
        return wordsBuffer[wbp++];
      }
      
      String line = raf.readLine();
      if(line == null){      // end of corpus
        eoc = true;
        return null;
      }
      line = line.trim();
      wordsBuffer = line.split("\\s+");
      wbp = 0;
      eoc = false;
      return null;
    }
  }
}
