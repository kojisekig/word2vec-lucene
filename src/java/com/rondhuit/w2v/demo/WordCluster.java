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

import com.rondhuit.w2v.KMeansClustering;
import com.rondhuit.w2v.VectorsReader;

public final class WordCluster {

  static void usage(){
    System.err.printf("Usage: java %s <vector-file> <k> <out-file>\n", WordCluster.class.getName());
    System.err.println("\t<vector-file> contains word projections in the text format\n");
    System.err.println("\t<k> number of clustering\n");
    System.err.println("\t<out-file> output file\n");
    System.exit(0);
  }

  public static void main(String[] args) throws Exception {
    if(args.length < 3) usage();

    final String vectorFile = args[0];
    final int k = Integer.parseInt(args[1]);
    final String outFile = args[2];
    final VectorsReader vectorsReader = new VectorsReader(vectorFile);
    vectorsReader.readVectorFile();
    
    KMeansClustering kmc = new KMeansClustering(vectorsReader, k, outFile);
    kmc.clustering();
  }
}
