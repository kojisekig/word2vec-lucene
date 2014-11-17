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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Scanner;

import com.rondhuit.commons.IOUtils;

public abstract class AbstractVectorsReader {

  static final int N = 40;

  protected final Charset ENCODING = Charset.forName("UTF-8");
  protected int words, size;
  protected String[] vocab;
  protected float[][] matrix;
  protected final String file;
  protected Scanner scanner;
  
  protected AbstractVectorsReader(String file){
    this.file = file;
  }
  
  protected void readVectorFile() throws IOException {
    System.err.printf("reading %s file. please wait...\n", file);
    
    InputStream is = null;
    Reader r = null;
    BufferedReader br = null;
    try{
      is = new FileInputStream(file);
      r = new InputStreamReader(is, ENCODING);
      br = new BufferedReader(r);
      
      String line = br.readLine();
      words = Integer.parseInt(line.split("\\s+")[0].trim());
      size = Integer.parseInt(line.split("\\s+")[1].trim());
      
      vocab = new String[words];
      matrix = new float[words][];

      for(int i = 0; i < words; i++){
        line = br.readLine().trim();
        String[] params = line.split("\\s+");
        vocab[i] = params[0];
        matrix[i] = new float[size];
        double len = 0;
        for(int j = 0; j < size; j++){
          matrix[i][j] = Float.parseFloat(params[j + 1]);
          len += matrix[i][j] * matrix[i][j];
        }
        len = Math.sqrt(len);
        for(int j = 0; j < size; j++){
          matrix[i][j] /= len;
        }
      }
    }
    catch(IOException e){
      IOUtils.closeQuietly(br);
      IOUtils.closeQuietly(r);
      IOUtils.closeQuietly(is);
    }
  }
  
  protected String[] nextWords(int n, String msg){
    System.out.println(msg + " ('q' to break): ");
    String[] words = new String[n];

    for(int i = 0; i < n; i++){
      String word = nextWord();
      if(word == null) return null;
      words[i] = word;
    }
    
    return words;
  }
  
  protected String nextWord(){
    String word = scanner.next();
    return word == null || word.length() == 0 || word.equals("q") ? null : word;
  }
  
  protected abstract Result getTargetVector();
  
  final protected void execute() throws IOException {
    readVectorFile();
    
    try{
      scanner = new Scanner(System.in);
      Result result = null;
      while((result = getTargetVector()) != null){
        
        double[] bestd = new double[N];
        String[] bestw = new String[N];
        next_word: for(int i = 0; i < words; i++){
          for(int bi : result.bi){
            if(i == bi) continue next_word;
          }
          double dist = 0;
          for(int j = 0; j < size; j++){
            dist += result.vec[j] * matrix[i][j];
          }
          for(int j = 0; j < N; j++){
            if (dist > bestd[j]) {
              for (int k = N - 1; k > j; k--) {
                bestd[k] = bestd[k - 1];
                bestw[k] = bestw[k - 1];
              }
              bestd[j] = dist;
              bestw[j] = vocab[i];
              break;
            }
          }
        }

        System.out.printf("\n                                              Word       Cosine distance\n------------------------------------------------------------------------\n");
        for (int j = 0; j < N; j++)
          System.out.printf("%50s\t\t%f\n", bestw[j], bestd[j]);
      }
    }
    finally{
      scanner.close();
    }
  }
  
  protected static class Result {

    float[] vec;
    int[] bi;
    
    public Result(float[] vec, int[] bi){
      this.vec = vec;
      this.bi = bi;
    }
  }
}
