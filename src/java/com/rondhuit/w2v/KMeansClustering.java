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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import com.rondhuit.commons.IOUtils;

public final class KMeansClustering {
  
  static final Charset ENCODING = Charset.forName("UTF-8");
  private final VectorsReader reader;
  private final int clcn;
  private final String outFile;

  public KMeansClustering(VectorsReader reader, int k, String outFile){
    this.reader = reader;
    this.clcn = k;
    this.outFile = outFile;
  }
  
  public void clustering() throws IOException {
    final int vocabSize = reader.getNumWords();
    final int layer1Size = reader.getSize();

    OutputStream os = null;
    Writer w = null;
    PrintWriter pw = null;

    try{
      os = new FileOutputStream(outFile);
      w = new OutputStreamWriter(os, ENCODING);
      pw = new PrintWriter(w);

      // Run K-means on the word vectors
      System.err.printf("now computing K-means clustering (K=%d)\n", clcn);
      final int MAX_ITER = 10;
      final int[] centcn = new int[clcn];
      final int[] cl = new int[vocabSize];
      final int centSize = clcn * layer1Size;
      final double[] cent = new double[centSize];
      
      for(int i = 0; i < vocabSize; i++)
        cl[i] = i % clcn;
      
      for(int it = 0; it < MAX_ITER; it++) {
        for(int j = 0; j < centSize; j++)
          cent[j] = 0;
        for(int j = 0; j < clcn; j++)
          centcn[j] = 1;
        for(int k = 0; k < vocabSize; k++){
          for(int l = 0; l < layer1Size; l++){
            cent[layer1Size * cl[k] + l] += reader.getMatrixElement(k, l);
          }
          centcn[cl[k]]++;
        }
        for(int j = 0; j < clcn; j++){
          double closev = 0;
          for(int k = 0; k < layer1Size; k++){
            cent[layer1Size * j + k] /= centcn[j];
            closev += cent[layer1Size * j + k] * cent[layer1Size * j + k];
          }
          closev = Math.sqrt(closev);
          for(int k = 0; k < layer1Size; k++){
            cent[layer1Size * j + k] /= closev;
          }
        }
        for(int k = 0; k < vocabSize; k++){
          double closev = -10;
          int closeid = 0;
          for(int l = 0; l < clcn; l++) {
            double x = 0;
            for(int j = 0; j < layer1Size; j++){
              x += cent[layer1Size * l + j] * reader.getMatrixElement(k, j);
            }
            if (x > closev) {
              closev = x;
              closeid = l;
            }
          }
          cl[k] = closeid;
        }
      }
      // Save the K-means classes
      System.err.printf("now saving the result of K-means clustering to the file %s\n", outFile);
      for(int i = 0; i < vocabSize; i++){
        pw.printf("%s %d\n", reader.getWord(i), cl[i]);
      }
    }
    finally{
      IOUtils.closeQuietly(pw);
      IOUtils.closeQuietly(w);
      IOUtils.closeQuietly(os);
    }
  }
}
