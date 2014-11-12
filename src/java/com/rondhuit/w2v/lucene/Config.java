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

package com.rondhuit.w2v.lucene;

public class Config {

  static final int DEF_ITER            = 5;
  static final int DEF_WINDOW          = 5;
  static final int DEF_MIN_COUNT       = 5;
  static final int DEF_NEGATIVE        = 5;
  static final int DEF_LAYER1_SIZE     = 100;
  static final int DEF_NUM_THREADS     = 12;
  static final float DEF_SAMPLE        = 0.001f;
  static final String DEF_ANALYZER     = "org.apache.lucene.analysis.core.WhitespaceAnalyzer";
  
  private String indexDir, outputFile, field, analyzer = DEF_ANALYZER;
  private int iter = DEF_ITER, window = DEF_WINDOW, minCount = DEF_MIN_COUNT, negative = DEF_NEGATIVE,
      layer1Size = DEF_LAYER1_SIZE, classes, numThreads = DEF_NUM_THREADS;
  private boolean hs, cbow;
  private float sample = DEF_SAMPLE, alpha = 0.025f;

  public Config setIndexDir(String indexDir){
    this.indexDir = indexDir;
    return this;
  }
  
  public String getIndexDir(){
    return indexDir;
  }

  public Config setOutputFile(String outputFile){
    this.outputFile = outputFile;
    return this;
  }
  
  public String getOutputFile(){
    return outputFile;
  }

  public Config setField(String field){
    this.field = field;
    return this;
  }
  
  public String getField(){
    return field;
  }

  public Config setAnalyzer(String analyzer){
    this.analyzer = analyzer;
    return this;
  }
  
  public String getAnalyzer(){
    return analyzer;
  }
  
  public Config setIter(int iter){
    this.iter = iter;
    return this;
  }
  
  public int getIter(){
    return iter;
  }
  
  public Config setWindow(int window){
    this.window = window;
    return this;
  }
  
  public int getWindow(){
    return window;
  }
  
  public Config setMinCount(int minCount){
    this.minCount = minCount;
    return this;
  }
  
  public int getMinCount(){
    return minCount;
  }
  
  public Config setNegative(int negative){
    this.negative = negative;
    return this;
  }
  
  public int getNegative(){
    return negative;
  }
  
  public Config setLayer1Size(int layer1Size){
    this.layer1Size = layer1Size;
    return this;
  }
  
  public int getLayer1Size(){
    return layer1Size;
  }
  
  public Config setClasses(int classes){
    this.classes = classes;
    return this;
  }
  
  public int getClasses(){
    return classes;
  }
  
  public Config setNumThreads(int numThreads){
    this.numThreads = numThreads;
    return this;
  }
  
  public int getNumThreads(){
    return numThreads;
  }
  
  public Config setUseHierarchicalSoftmax(boolean hs){
    this.hs = hs;
    return this;
  }
  
  public boolean useHierarchicalSoftmax(){
    return hs;
  }
  
  public Config setUseContinuousBagOfWords(boolean cbow){
    this.cbow = cbow;
    return this;
  }
  
  public boolean useContinuousBagOfWords(){
    return cbow;
  }
  
  public Config setSample(float sample){
    this.sample = sample;
    return this;
  }
  
  public float getSample(){
    return sample;
  }
  
  public Config setAlpha(float alpha){
    this.alpha = alpha;
    return this;
  }
  
  public float getAlpha(){
    return alpha;
  }
}
