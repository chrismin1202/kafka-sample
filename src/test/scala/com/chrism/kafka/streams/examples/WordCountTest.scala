/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.chrism.kafka.streams.examples

import com.chrism.commons.FunTestSuite
import com.chrism.kafka.KafkaTestSuiteLike
import com.chrism.kafka.streams.KafkaStreamsTestSuiteLike

final class WordCountTest extends FunTestSuite with KafkaTestSuiteLike with KafkaStreamsTestSuiteLike {

  test("tokenizing text") {
    WordCount.tokenize("Word1 Word2 Word3") should contain theSameElementsInOrderAs Seq("word1", "word2", "word3")
  }

  test("streaming example: word count") {
    withRunningKafka() { conf =>
      withRunningKafkaStreamsApp(WordCount(bootstrapServers = conf.bootstrapServers)) { driver =>
        Seq(
          "The path of the righteous man is beset on all sides",
          "by the inequities of the selfish and the tyranny of evil men",
          "Blessed is he who",
          "in the name of charity and good will",
          "shepherds the weak through the valley of darkness",
          "for he is truly his brother's keeper",
          "and the finder of lost children",
          "And I will strike down upon thee with great vengeance",
          "And furious Anger",
          "those who attempt to poison and destroy my brothers",
          "And you will know my name is the Lord",
          "when I lay my vengeance upon thee",
        ).foreach(driver.sendValue(WordCount.InputTopic.topic, _))

        val wordCounts = driver
          .consumeKeyValues(WordCount.OutputTopic.topic)
          .groupBy(_._1)
          .mapValues(_.map(_._2).max)
          .toSeq
          .sortBy(_._2)(Ordering[Long].reverse)
          .filter(_._2 >= 3L)

        wordCounts should contain theSameElementsAs Seq(
          "the" -> 10L,
          "and" -> 7L,
          "of" -> 6L,
          "is" -> 4L,
          "my" -> 3L,
          "will" -> 3L,
        )
      }
    }
  }
}
