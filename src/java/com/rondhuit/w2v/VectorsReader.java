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

import com.rondhuit.commons.IOUtils;

public final class VectorsReader {

  public final Charset ENCODING = Charset.forName("UTF-8");
  private int words, size;
  private String[] vocab;
  private float[][] matrix;
  private final String file;

  public VectorsReader(String file){
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
  
  public int getSize(){
    return size;
  }
  
  public int getNumWords(){
    return words;
  }
  
  public String getWord(int idx){
    return vocab[idx];
  }
  
  public float getMatrixElement(int row, int column){
    return matrix[row][column];
  }
}
