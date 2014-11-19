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

import com.rondhuit.w2v.AbstractClosestVectors;

public class WordAnalogy extends AbstractClosestVectors {

  protected WordAnalogy(String file) {
    super(file);
  }
  
  static void usage(){
    System.err.printf("Usage: java %s <FILE>\nwhere FILE contains word projections in the text format\n",
        WordAnalogy.class.getName());
    System.exit(0);
  }

  public static void main(String[] args) throws IOException {
    if(args.length < 1) usage();
    new WordAnalogy(args[0]).execute();
  }
  
  protected Result getTargetVector(){
    final int words = vectorsReader.getNumWords();
    final int size = vectorsReader.getSize();
    
    String[] input = null;
    while((input = nextWords(3, "Enter 3 words")) != null){
      // linear search the input word in vocabulary
      int[] bi = new int[input.length];
      int found = 0;
      for(int k = 0; k < input.length; k++){
        for(int i = 0; i < words; i++){
          if(input[k].equals(vectorsReader.getWord(i))){
            bi[k] = i;
            System.out.printf("\nWord: %s  Position in vocabulary: %d\n", input[k], bi[k]);
            found++;
          }
        }
        if(found == k){
          System.out.printf("%s : Out of dictionary word!\n", input[k]);
        }
      }
      if(found < input.length){
        continue;
      }

      float[] vec = new float[size];
      double len = 0;
      for(int j = 0; j < size; j++){
        vec[j] = vectorsReader.getMatrixElement(bi[1], j) -
            vectorsReader.getMatrixElement(bi[0], j) + vectorsReader.getMatrixElement(bi[2], j);
        len += vec[j] * vec[j];
      }
      
      len = Math.sqrt(len);
      for(int i = 0; i < size; i++){
        vec[i] /= len;
      }
      
      return new Result(vec, bi);
    }
    
    return null;
  }
}
