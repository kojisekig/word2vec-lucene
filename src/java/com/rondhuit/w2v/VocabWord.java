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

public class VocabWord {
  
  public static final int MAX_CODE_LENGTH     = 40;

  int cn, codelen;
  int[] point;
  String word;
  char[] code;
  
  public VocabWord(String word){
    this.word = word;
    cn = 0;
    point = new int[MAX_CODE_LENGTH];
    code = new char[MAX_CODE_LENGTH];
  }
  
  public void setCn(int cn){
    this.cn = cn;
  }
  
  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("[%s] cn=%d, codelen=%d, ", word, cn, codelen));
    sb.append("code=(");
    for(int i = 0; i < codelen; i++){
      if(i>0) sb.append(',');
      sb.append(code[i]);
    }
    sb.append("), point=(");
    for(int i = 0; i < codelen; i++){
      if(i>0) sb.append(',');
      sb.append(point[i]);
    }
    sb.append(")");
    return sb.toString();
  }
}
