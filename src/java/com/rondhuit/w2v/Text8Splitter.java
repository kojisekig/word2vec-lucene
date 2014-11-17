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

import java.io.FileReader;
import java.io.PrintStream;
import java.io.Reader;

import com.rondhuit.commons.IOUtils;

/**
 * This program splits text8, which consits of a very long single text line, into a multi-lines file.
 *
 */
public class Text8Splitter {
  
  static final int MAX_WORDS = 1000;

  public static void main(String[] args) throws Exception {
    if(args.length != 2) usage();
    
    final String fin = args[0];
    final String fout = args[1];

    Reader r = null;
    PrintStream ps = null;
    try{
      r = new FileReader(fin);
      ps = new PrintStream(fout);
      int c = r.read();
      int counter = 0;
      while(c != -1){
        ps.print((char)c);
        if(c == ' '){
          if(++counter >= MAX_WORDS){
            counter = 0;
            ps.println();   // print CR
          }
        }
        c = r.read();
      }
    }
    finally{
      IOUtils.closeQuietly(r);
      IOUtils.closeQuietly(ps);
    }
    
  }

  static void usage(){
    System.err.printf("Usage: java %s text8 text8.txt\n", Text8Splitter.class.getName());
    System.exit(0);
  }
}
