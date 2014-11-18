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
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rondhuit.commons.IOUtils;
import com.rondhuit.w2v.lucene.Config;

public class Word2vec {

  static final int VOCAB_MAX_SIZE      = 30000000;
  static final int EXP_TABLE_SIZE      = 1000;
  static final int MAX_EXP             = 6;
  static final int TABLE_SIZE          = 100000000;
  static final int MAX_SENTENCE_LENGTH = 1000;
  static final Charset ENCODING = Charset.forName("UTF-8");

  long timeStart;
  static double[] syn0, syn1, syn1neg;
  int[] table;
  
  private final Config config;
  
  static final double[] expTable = new double[EXP_TABLE_SIZE + 1];
  
  static{
    for (int i = 0; i < EXP_TABLE_SIZE; i++) {
      expTable[i] = Math.exp((i / (double)EXP_TABLE_SIZE * 2 - 1) * MAX_EXP); // Precompute the exp() table
      expTable[i] = expTable[i] / (expTable[i] + 1);                          // Precompute f(x) = x / (x + 1)
    }
  }
  
  private static Logger logger = LoggerFactory.getLogger(Word2vec.class);
  
  public Word2vec(Config config){
    this.config = config;
  }
  
  public Config getConfig(){
    return config;
  }
  
  static class TrainModelThread extends Thread {
    final Word2vec vec;
    final Corpus corpus;
    final Config config;
    float alpha;
    final float startingAlpha;
    final int id, trainWords, vocabSize;
    final long timeStart;
    final int[] table;
    final VocabWord[] vocab;
    static int wordCountActual = 0;
    
    public TrainModelThread(Word2vec vec, Corpus corpus, Config config, int id){
      this.vec = vec;
      this.corpus = corpus;
      this.config = config;
      this.alpha = config.getAlpha();
      this.startingAlpha = alpha;
      this.id = id;
      this.table = vec.table;
      this.trainWords = corpus.getTrainWords();
      this.timeStart = vec.timeStart;
      this.vocabSize = corpus.getVocabSize();
      this.vocab = corpus.getVocab();
    }
    
    public void run(){
      final int iter = config.getIter();
      final int layer1Size = config.getLayer1Size();
      final int numThreads = config.getNumThreads();
      final int window = config.getWindow();
      final int negative = config.getNegative();
      final boolean cbow = config.useContinuousBagOfWords();
      final boolean hs = config.useHierarchicalSoftmax();
      final float sample = config.getSample();
      
      try{
        int word = 0, sentence_length = 0, sentence_position = 0, a, b, c, d, last_word, l1, l2, target;
        int[] sen = new int[MAX_SENTENCE_LENGTH + 1];
        long cw;
        long word_count = 0, last_word_count = 0;
        long label, local_iter = iter;
        long next_random = id;
        double f, g;
        long timeNow;
        double[] neu1 = new double[layer1Size];
        double[] neu1e = new double[layer1Size];

        corpus.rewind(numThreads, id);
        while(true){
          if (word_count - last_word_count > 10000) {
            wordCountActual += word_count - last_word_count;
            last_word_count = word_count;
            timeNow = System.currentTimeMillis();
            System.err.printf("%cAlpha: %f  Progress: %.2f%%  Words/thread/sec: %.2fk  ", 13, alpha,
                wordCountActual / (float)(iter * trainWords + 1) * 100,
                wordCountActual / (float)(timeNow - timeStart + 1));
            System.err.flush();
            alpha = startingAlpha * (1 - wordCountActual / (float)(iter * trainWords + 1));
            if (alpha < startingAlpha * 0.0001) alpha = startingAlpha * 0.0001F;
          }
          if (sentence_length == 0) {
            while(true){
              word = corpus.readWordIndex();
              if(word == -2) break;                // EOF
              if(word == -1) continue;
              word_count++;
              if (word == -3) break;
              // The subsampling randomly discards frequent words while keeping the ranking same
              if (sample > 0) {
                double ran = (Math.sqrt(vocab[word].cn / (sample * trainWords)) + 1) * (sample * trainWords) / vocab[word].cn;
                next_random = nextRandom(next_random);
                if (ran < (next_random & 0xFFFF) / (double)65536) continue;
              }
              sen[sentence_length] = word;
              sentence_length++;
              if (sentence_length >= MAX_SENTENCE_LENGTH) break;
            }
            sentence_position = 0;
          }
          if(word == -2 /* eof? */ || (word_count > trainWords / numThreads)){
            wordCountActual += word_count - last_word_count;
            local_iter--;
            if (local_iter == 0) break;
            word_count = 0;
            last_word_count = 0;
            sentence_length = 0;
            corpus.rewind(numThreads, id);
            continue;
          }
          word = sen[sentence_position];
          if (word == -1) continue;
          for (c = 0; c < layer1Size; c++) neu1[c] = 0;
          for (c = 0; c < layer1Size; c++) neu1e[c] = 0;
          next_random = nextRandom(next_random);
          b = (int)next_random % window;
          if (cbow) {  //train the cbow architecture
            // in -> hidden
            cw = 0;
            for (a = b; a < window * 2 + 1 - b; a++) if (a != window) {
              c = sentence_position - window + a;
              if (c < 0) continue;
              if (c >= sentence_length) continue;
              last_word = sen[c];
              if (last_word == -1) continue;
              for (c = 0; c < layer1Size; c++) neu1[c] += syn0[c + last_word * layer1Size];
              cw++;
            }
            if(cw != 0) {
              for (c = 0; c < layer1Size; c++) neu1[c] /= cw;
              if (hs) for (d = 0; d < vocab[word].codelen; d++) {
                f = 0;
                l2 = vocab[word].point[d] * layer1Size;
                // Propagate hidden -> output
                for (c = 0; c < layer1Size; c++) f += neu1[c] * syn1[c + l2];
                if (f <= -MAX_EXP) continue;
                else if (f >= MAX_EXP) continue;
                else f = expTable[(int)((f + MAX_EXP) * (EXP_TABLE_SIZE / MAX_EXP / 2))];
                // 'g' is the gradient multiplied by the learning rate
                g = (1 - vocab[word].code[d] - f) * alpha;
                // Propagate errors output -> hidden
                for (c = 0; c < layer1Size; c++) neu1e[c] += g * syn1[c + l2];
                // Learn weights hidden -> output
                for (c = 0; c < layer1Size; c++) syn1[c + l2] += g * neu1[c];
              }
              // NEGATIVE SAMPLING
              if (negative > 0) for (d = 0; d < negative + 1; d++) {
                if (d == 0) {
                  target = word;
                  label = 1;
                } else {
                  next_random = nextRandom(next_random);
                  target = table[Math.abs((int)((next_random >> 16) % TABLE_SIZE))];
                  if (target == 0) target = Math.abs((int)(next_random % (vocabSize - 1) + 1));
                  if (target == word) continue;
                  label = 0;
                }
                l2 = target * layer1Size;
                f = 0;
                for (c = 0; c < layer1Size; c++) f += neu1[c] * syn1neg[c + l2];
                if (f > MAX_EXP) g = (label - 1) * alpha;
                else if (f < -MAX_EXP) g = (label - 0) * alpha;
                else g = (label - expTable[(int)((f + MAX_EXP) * (EXP_TABLE_SIZE / MAX_EXP / 2))]) * alpha;
                for (c = 0; c < layer1Size; c++) neu1e[c] += g * syn1neg[c + l2];
                for (c = 0; c < layer1Size; c++) syn1neg[c + l2] += g * neu1[c];
              }
              // hidden -> in
              for (a = b; a < window * 2 + 1 - b; a++) if (a != window) {
                c = sentence_position - window + a;
                if (c < 0) continue;
                if (c >= sentence_length) continue;
                last_word = sen[c];
                if (last_word == -1) continue;
                for (c = 0; c < layer1Size; c++) syn0[c + last_word * layer1Size] += neu1e[c];
              }
            }
          } else {  //train skip-gram
            for (a = b; a < window * 2 + 1 - b; a++) if (a != window) {
              c = sentence_position - window + a;
              if (c < 0) continue;
              if (c >= sentence_length) continue;
              last_word = sen[c];
              if (last_word == -1) continue;
              l1 = last_word * layer1Size;
              for (c = 0; c < layer1Size; c++) neu1e[c] = 0;
              // HIERARCHICAL SOFTMAX
              if (hs) for (d = 0; d < vocab[word].codelen; d++) {
                f = 0;
                l2 = vocab[word].point[d] * layer1Size;
                // Propagate hidden -> output
                for (c = 0; c < layer1Size; c++) f += syn0[c + l1] * syn1[c + l2];
                if (f <= -MAX_EXP) continue;
                else if (f >= MAX_EXP) continue;
                else f = expTable[(int)((f + MAX_EXP) * (EXP_TABLE_SIZE / MAX_EXP / 2))];
                // 'g' is the gradient multiplied by the learning rate
                g = (1 - vocab[word].code[d] - f) * alpha;
                // Propagate errors output -> hidden
                for (c = 0; c < layer1Size; c++) neu1e[c] += g * syn1[c + l2];
                // Learn weights hidden -> output
                for (c = 0; c < layer1Size; c++) syn1[c + l2] += g * syn0[c + l1];
              }
              // NEGATIVE SAMPLING
              if (negative > 0) for (d = 0; d < negative + 1; d++) {
                if (d == 0) {
                  target = word;
                  label = 1;
                } else {
                  next_random = nextRandom(next_random);
                  target = table[Math.abs((int)((next_random >> 16) % TABLE_SIZE))];
                  if (target == 0) target = Math.abs((int)(next_random % (vocabSize - 1) + 1));
                  if (target == word) continue;
                  label = 0;
                }
                l2 = target * layer1Size;
                f = 0;
                for (c = 0; c < layer1Size; c++) f += syn0[c + l1] * syn1neg[c + l2];
                if (f > MAX_EXP) g = (label - 1) * alpha;
                else if (f < -MAX_EXP) g = (label - 0) * alpha;
                else g = (label - expTable[(int)((f + MAX_EXP) * (EXP_TABLE_SIZE / MAX_EXP / 2))]) * alpha;
                for (c = 0; c < layer1Size; c++) neu1e[c] += g * syn1neg[c + l2];
                for (c = 0; c < layer1Size; c++) syn1neg[c + l2] += g * syn0[c + l1];
              }
              // Learn weights input -> hidden
              for (c = 0; c < layer1Size; c++) syn0[c + l1] += neu1e[c];
            }
          }
          sentence_position++;
          if (sentence_position >= sentence_length) {
            sentence_length = 0;
            continue;
          }
        }
      }
      catch(IOException e){
        throw new RuntimeException(e);
      }
      // exit from thread
      synchronized (vec) {
        vec.threadCount--;
        vec.notify();
      }
    }
  }
  
  int threadCount;
  
  public void trainModel(CorpusFactory corpusFactory) throws IOException {
    System.err.printf("Starting training using Lucene index %s\n", config.getIndexDir());

    final int layer1Size = config.getLayer1Size();
    Corpus corpus = corpusFactory.create(config);

    corpus.learnVocab();
    corpus.sortVocab();
    final int vocabSize = corpus.getVocabSize();
    final VocabWord[] vocab = corpus.getVocab();
    logger.info("Vocab size: {}\n", vocabSize);
    logger.info("Words in train file: {}\n", corpus.getTrainWords());

    if(config.getOutputFile() == null) return;

    initNet(corpus);
    if(config.getNegative() > 0)
      initUnigramTable(corpus);

    timeStart = System.currentTimeMillis();

    threadCount = config.getNumThreads();
    for(int i = 0; i < config.getNumThreads(); i++){
      new TrainModelThread(this, corpusFactory.create(corpus), config, i).start();
    }
    synchronized (this) {
      while(threadCount > 0){
        try {
          wait();
        }
        catch (InterruptedException ignored) {}
      }
    }

    OutputStream os = null;
    Writer w = null;
    PrintWriter pw = null;

    try{
      os = new FileOutputStream(config.getOutputFile());
      w = new OutputStreamWriter(os, ENCODING);
      pw = new PrintWriter(w);
      
      if(config.getClasses() == 0){
        // Save the word vectors
        logger.info("now saving the word vectors to the file {}", config.getOutputFile());
        pw.printf("%d %d\n", vocabSize, layer1Size);
        for(int i = 0; i < vocabSize; i++){
          pw.print(vocab[i].word);
          for(int j = 0; j < layer1Size; j++){
            pw.printf(" %f", syn0[i * layer1Size + j]);
          }
          pw.println();
        }
      }
      else{
        // Run K-means on the word vectors
        logger.info("now computing K-means clustering (K={})", config.getClasses());
        final int MAX_ITER = 10;
        final int clcn = config.getClasses();
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
              cent[layer1Size * cl[k] + l] += syn0[k * layer1Size + l];
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
                x += cent[layer1Size * l + j] * syn0[k * layer1Size + j];
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
        logger.info("now saving the result of K-means clustering to the file {}", config.getOutputFile());
        for(int i = 0; i < vocabSize; i++){
          pw.printf("%s %d\n", vocab[i].word, cl[i]);
        }
      }
    }
    finally{
      corpus.close();
      IOUtils.closeQuietly(pw);
      IOUtils.closeQuietly(w);
      IOUtils.closeQuietly(os);
    }
  }

  /**
   * Used later for sorting by word counts
   *
   */
  static class VocabWordComparator implements Comparator<VocabWord> {
    @Override
    public int compare(VocabWord o1, VocabWord o2) {
      return o2.cn - o1.cn;
    }
  }
  
  void initUnigramTable(Corpus corpus){
    final int vocabSize = corpus.getVocabSize();
    final VocabWord[] vocab = corpus.getVocab();
    long trainWordsPow = 0;
    double d1, power = 0.75;
    table = new int[TABLE_SIZE];
    for(int i = 0; i < vocabSize; i++){
      trainWordsPow += Math.pow(vocab[i].cn, power);
    }
    int i = 0;
    d1 = Math.pow(vocab[i].cn, power) / (double)trainWordsPow;
    for(int j = 0; j < TABLE_SIZE; j++){
      table[j] = i;
      if((double)j / (double)TABLE_SIZE > d1) {
        i++;
        d1 += Math.pow(vocab[i].cn, power) / (double)trainWordsPow;
      }
      if (i >= vocabSize)
        i = vocabSize - 1;
    }
  }
  
  void initNet(Corpus corpus){
    final int layer1Size = config.getLayer1Size();
    final int vocabSize = corpus.getVocabSize();
    
    syn0 = posixMemAlign128(vocabSize * layer1Size);

    if(config.useHierarchicalSoftmax()){
      syn1 = posixMemAlign128(vocabSize * layer1Size);
      for(int i = 0; i < vocabSize; i++){
        for(int j = 0; j < layer1Size; j++){
          syn1[i * layer1Size + j] = 0;
        }
      }
    }

    if(config.getNegative() > 0){
      syn1neg = posixMemAlign128(vocabSize * layer1Size);
      for(int i = 0; i < vocabSize; i++){
        for(int j = 0; j < layer1Size; j++){
          syn1neg[i * layer1Size + j] = 0;
        }
      }
    }

    long nextRandom = 1;
    for(int i = 0; i < vocabSize; i++){
      for(int j = 0; j < layer1Size; j++){
        nextRandom = nextRandom(nextRandom);
        syn0[i * layer1Size + j] = (((nextRandom & 0xFFFF) / (double)65536) - 0.5) / layer1Size;
      }
    }
    corpus.createBinaryTree();
  }
  
  static double[] posixMemAlign128(int size){
    final int surplus = size % 128;
    if(surplus > 0){
      int div = size / 128;
      return new double[(div + 1) * 128];
    }
    return new double[size];
  }
  
  static long nextRandom(long nextRandom){
    return nextRandom * 25214903917L + 11;
  }
}
