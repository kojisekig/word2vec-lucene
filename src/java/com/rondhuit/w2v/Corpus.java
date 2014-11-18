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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rondhuit.w2v.Word2vec.VocabWordComparator;
import com.rondhuit.w2v.lucene.Config;

public abstract class Corpus {

  protected Config config;
  protected int trainWords = 0;
  protected int vocabSize;
  protected int vocabMaxSize = 1000;
  protected VocabWord[] vocab;
  protected Map<String, Integer> vocabIndexMap;
  protected boolean eoc = true;    // end of corpus                                                                                           

  public Corpus(Config config) throws IOException {
    this.config = config;
  }

  public Corpus(Corpus cloneSrc) throws IOException {
    trainWords = cloneSrc.trainWords;
    vocabSize = cloneSrc.vocabSize;
    vocab = cloneSrc.vocab;
    vocabIndexMap = cloneSrc.vocabIndexMap;
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

  public int getTrainWords(){
    return trainWords;
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

  public int readWordIndex() throws IOException {
    String word = nextWord();
    return word == null ? -2 : searchVocab(word);
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

  /**
   * Returns position of a word in the vocabulary; if the word is not found, returns -1
   * @param word
   * @return
   */
  int searchVocab(String word){
    Integer pos = vocabIndexMap.get(word);
    return pos == null ? -1 : pos.intValue();
  }

  /**
   * Sorts the vocabulary by frequency using word counts
   */
  void sortVocab(){
    List<VocabWord> list = new ArrayList<VocabWord>(vocabSize);
    for(int i = 0; i < vocabSize; i++){
      list.add(vocab[i]);
    }
    Collections.sort(list, new VocabWordComparator());
    
    // re-build vocabIndexMap
    vocabIndexMap.clear();
    final int size = vocabSize;
    trainWords = 0;
    for(int i = 0; i < size; i++){
      // Words occuring less than min_count times will be discarded from the vocab
      if(list.get(i).cn < config.getMinCount()){
        vocabSize--;
      }
      else{
        // Hash will be re-computed, as after the sorting it is not actual
        setVocabIndexMap(list.get(i), i);
      }
    }

    vocab = new VocabWord[vocabSize];
    for(int i = 0; i < vocabSize; i++){
      vocab[i] = new VocabWord(list.get(i).word);
      vocab[i].cn = list.get(i).cn;
    }
  }
  
  void setVocabIndexMap(VocabWord src, int pos){
    vocabIndexMap.put(src.word, pos);
    trainWords += src.cn;
  }

  /**
   * Create binary Huffman tree using the word counts. 
   * Frequent words will have short uniqe binary codes
   */
  void createBinaryTree() {
    int[] point = new int[VocabWord.MAX_CODE_LENGTH];
    char[] code = new char[VocabWord.MAX_CODE_LENGTH];
    int[] count = new int[vocabSize * 2 + 1];
    char[] binary = new char[vocabSize * 2 + 1];
    int[] parentNode = new int[vocabSize * 2 + 1];
    
    for(int i = 0; i < vocabSize; i++)
      count[i] = vocab[i].cn;
    for(int i = vocabSize; i < vocabSize * 2; i++)
      count[i] = Integer.MAX_VALUE;
    int pos1 = vocabSize - 1;
    int pos2 = vocabSize;
    // Following algorithm constructs the Huffman tree by adding one node at a time
    int min1i, min2i;
    for(int i = 0; i < vocabSize - 1; i++) {
      // First, find two smallest nodes 'min1, min2'
      if (pos1 >= 0) {
        if (count[pos1] < count[pos2]) {
          min1i = pos1;
          pos1--;
        } else {
          min1i = pos2;
          pos2++;
        }
      } else {
        min1i = pos2;
        pos2++;
      }
      if (pos1 >= 0) {
        if (count[pos1] < count[pos2]) {
          min2i = pos1;
          pos1--;
        } else {
          min2i = pos2;
          pos2++;
        }
      } else {
        min2i = pos2;
        pos2++;
      }
      count[vocabSize + i] = count[min1i] + count[min2i];
      parentNode[min1i] = vocabSize + i;
      parentNode[min2i] = vocabSize + i;
      binary[min2i] = 1;
    }
    // Now assign binary code to each vocabulary word
    for(int j = 0; j < vocabSize; j++){
      int k = j;
      int i = 0;
      while (true) {
        code[i] = binary[k];
        point[i] = k;
        i++;
        k = parentNode[k];
        if(k == vocabSize * 2 - 2) break;
      }
      vocab[j].codelen = i;
      vocab[j].point[0] = vocabSize - 2;
      for(k = 0; k < i; k++) {
        vocab[j].code[i - k - 1] = code[k];
        vocab[j].point[i - k] = point[k] - vocabSize;
      }
    }
  }
}
