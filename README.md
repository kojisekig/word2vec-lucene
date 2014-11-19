word2vec-lucene
===============

This tool extracts word vectors from Lucene index.

# strength and weakness
## strength
* You don't need to provide a text file besides Lucene index.
* You don't need to normalize text. Normalization has already been done in the index or Analyzer does it for you when processing.
* You can use a part of index rather than the whole of it by specifying a filter query.

## weakness (known limitations)
* You need to provide a Lucene index as a text corpus.
* You need to set field to be processed. The field must be indexed and stored.


# HOW TO USE

In this section, you'll know how to use demo environment provided in this project.

## prepare Apache Solr

Download Apache Solr 4.10.2 (recommended) and unzip the downloaded file in an appropriate directory. Go to example directory and launch Solr with solr.solr.home property.

    $ cd solr-4.10.2/example
    $ java -Dsolr.solr.home=${word2vec-lucene}/solrhome -jar start.jar

## for people who like text8
### prepare text8.xml file
Execute ant to prepare sample input text8.xml file.

    $ ant t8-solr

### index text8.xml into Solr
Index text8.xml file to Solr.

    $ ./optimize.sh collection1 text8.xml

### create vectors.txt file
Once you got Lucene index, you can now create vectors.txt file.

    $ ./demo-word2vec.sh collection1

## for people who has PDF file
If you have Lucene in Action book PDF file, post the file to Solr.

    $ ./solrcell.sh LuceneInAction.pdf

## for people who prefer Japanese text
### prepare livedoor news corpus
Download livedoor news corpus from RONDHUIT site and unzip it in an appropriate directory.

    $ cd ${word2vec-lucene}
    $ mkdir work
    $ cd work
    $ wget http://www.rondhuit.com/download/livedoor-news-data.tar.gz
    $ tar xvzf livedoor-news-data.tar.gz
    $ cd ..

### index livedoor news corpus into Solr
Index livedoor news corpus xml files to Solr.

    $ ./optimize.sh ldcc work/*.xml

### create vectors.txt file
Once you got Lucene index, you can now create vectors.txt file.

    $ ./demo-word2vec.sh ldcc org.apache.lucene.analysis.ja.JapaneseAnalyzer

## compute distance among word vectors
Once you got word vectors file vectors.txt, you can find most similar 40 words to the word you specified.

    $ ./demo-distance.sh
    cat
    Word: cat  Position in vocabulary: 2601

                                                  Word      Cosine distance
    ------------------------------------------------------------------------
                                                   rat          0.591972
                                                  cats          0.587605
                                                 hyena          0.583455
                                              squirrel          0.580696
                                                  dogs          0.568277
                                                   dog          0.556022


Or, you can compute vector operations e.g. vector('paris') - vector('france') + vector('italy') or vector('king') - vector('man') + vector('woman')

    $ ./demo-analogy.sh
    france paris italy
    man king woman
