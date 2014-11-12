#!/bin/bash
#
# Copyright (c) 2014 RONDHUIT Co.,Ltd.
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#

LUCENE_JAR=$(ls lib/lucene-core-*.jar)
LUCENE_JAR=${LUCENE_JAR}:$(ls lib/lucene-analyzers-common-*.jar)
RHCOM_JAR=$(ls lib/RONDHUIT-COMMONS-*.jar)
SLF4J_JAR=$(ls lib/slf4j-api-*.jar)
SLF4J_JAR=${SLF4J_JAR}:$(ls lib/slf4j-jdk14-*.jar)

java -cp ${LUCENE_JAR}:${RHCOM_JAR}:${SLF4J_JAR}:bin com.rondhuit.w2v.lucene.demo.CreateVectors -index solrhome/collection1/data/index -output vectors.txt -field body -cbow 1 -size 200 -window 8 -negative 25 -sample 0.0001 -threads 4 -iter 15 -min-count 5
