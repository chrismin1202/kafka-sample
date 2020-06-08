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
package com.chrism.kafka

import com.chrism.commons.FunTestSuite
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

final class KafkaTestSuiteTest extends FunTestSuite with KafkaTestSuiteLike {

  test("producing and consuming fixed number of messages") {
    withRunningKafka() { conf =>
      val producer = KeylessProducer
        .builder[String, StringSerializer]()
        .connectionConfiguration(conf)
        .build()
      val consumer = KeylessConsumer
        .builder[String, StringDeserializer]()
        .connectionConfiguration(conf)
        .build()
      val topic = "test_fixed"
      val messages = (1 to 10)
        .map { i =>
          val message = s"Message $i"
          producer.send(topic, message)
          message
        }

      consumer.consumeMessagesFromTopic(topic, messages.size) match {
        case Some(cm) => cm should contain theSameElementsAs messages
        case _        => fail("Failed to consume messages!")
      }
    }
  }

  test("producing and consuming any number of messages") {
    withRunningKafka() { conf =>
      val producer = KeylessProducer
        .builder[String, StringSerializer]()
        .connectionConfiguration(conf)
        .build()
      val consumer = KeylessConsumer
        .builder[String, StringDeserializer]()
        .connectionConfiguration(conf)
        .build()
      val topic = "test_any"
      val messages = (1 to 20)
        .map { i =>
          val message = s"Message $i"
          producer.send(topic, message)
          message
        }

      var attempts = 0
      var consumedMessages = Vector.empty[String]
      while (attempts < 50 && consumedMessages.size < messages.size) {
        consumer.consumeAnyMessagesFromTopic(topic) match {
          case Some(cm) => consumedMessages = consumedMessages ++ cm
          case _        =>
        }
        attempts += 1
      }
      consumedMessages should contain theSameElementsAs messages
    }
  }
}
