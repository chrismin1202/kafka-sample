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
import org.apache.kafka.common.serialization.StringDeserializer

final class ConsumerTest extends FunTestSuite {

  test("requiring at least 1 bootstrap server when building DefaultConsumer") {
    intercept[IllegalArgumentException] {
      DefaultConsumer
        .builder[String, DummyKafkaRequest, StringDeserializer, DummyKafkaRequestDeserializer.type]()
        .build()
    }
  }

  test("requiring at least 1 bootstrap server when building KeylessConsumer") {
    intercept[IllegalArgumentException] {
      KeylessConsumer
        .builder[DummyKafkaRequest, DummyKafkaRequestDeserializer.type]()
        .build()
    }
  }

  test("DefaultConsumer: equals/hashCode") {
    val c1 = DefaultConsumer
      .builder[String, DummyKafkaRequest, StringDeserializer, DummyKafkaRequestDeserializer.type]()
      .addBootstrapServer("host", 666)
      .build()
    val c2 = DefaultConsumer
      .builder[String, DummyKafkaRequest, StringDeserializer, DummyKafkaRequestDeserializer.type]()
      .addBootstrapServer("host", 666)
      .build()
    assert(c1 === c2)
    assert(c1.hashCode() === c2.hashCode())
  }

  test("KeylessConsumer: equals/hashCode") {
    val c1 = KeylessConsumer
      .builder[DummyKafkaRequest, DummyKafkaRequestDeserializer.type]()
      .addBootstrapServer("host", 666)
      .build()
    val c2 = KeylessConsumer
      .builder[DummyKafkaRequest, DummyKafkaRequestDeserializer.type]()
      .addBootstrapServer("host", 666)
      .build()
    assert(c1 === c2)
    assert(c1.hashCode() === c2.hashCode())
  }
}
