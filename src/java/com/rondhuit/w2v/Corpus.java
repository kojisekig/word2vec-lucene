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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.rondhuit.w2v.lucene.Config;

public abstract class Corpus {

  protected Config config;
  protected int vocabSize;
  protected int vocabMaxSize = 1000;
  protected VocabWord[] vocab;
  protected Map<String, Integer> vocabIndexMap;
  protected boolean eoc = true;    // end of corpus                                                                                           

  public Corpus(Config config) throws IOException {
    this.config = config;
  }

  public Corpus(Corpus cloneSrc) throws IOException {
  }

  public boolean endOfCorpus(){
    return eoc;
  }

  public void learnVocab()  throws IOException {
    vocab = new VocabWord[vocabMaxSize];
    vocabIndexMap = new HashMap<String, Integer>();
    vocabSize = 0;
  }

  /**                                                                                                                                         
   * Adds a word to the vocabulary                                                                                                            
   * @param word                                                                                                                              
   * @return                                                                                                                                  
   */
  protected int addWordToVocab(String word){
    vocab[vocabSize] = new VocabWord(word);
    vocabSize++;

    // Reallocate memory if needed                                                                                                            
    if(vocabSize + 2 >= vocabMaxSize){
      vocabMaxSize += 1000;
      VocabWord[] temp = new VocabWord[vocabMaxSize];
      System.arraycopy(vocab, 0, temp, 0, vocabSize);
      vocab = temp;
    }
    vocabIndexMap.put(word, vocabSize - 1);
    return vocabSize - 1;
  }

  public int getVocabSize(){
    return vocabSize;
  }

  public VocabWord[] getVocab(){
    return vocab;
  }

  public Map<String, Integer> getVocabIndexMap(){
    return vocabIndexMap;
  }

  /**                                                                                                                                         
   *                                                                                   
   */
  public void rewind(int numThreads, int id){
    eoc = false;
  }

  /**
   * Read the next word from the corpus
   * @return next word that is read from the corpus. null will be returned
   * @throws IOException
   */
  public abstract String nextWord() throws IOException;

  /**                                                                                                                                         
   * Close the corpus and it cannot be read any more.                                                                                         
   */
  public abstract void close() throws IOException;
}
