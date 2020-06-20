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
import com.chrism.net.{HostPort, LocalHostPort}
import com.chrism.util.PropertiesBuilder
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer

final class ProducerTest extends FunTestSuite {

  test("requiring at least 1 bootstrap server when building KeyValueProducer") {
    intercept[IllegalArgumentException] {
      KeyValueProducer
        .builder[String, DummyKafkaRequest, StringSerializer, DummyKafkaRequestSerializer.type]()
        .build()
    }
  }

  test("requiring client.id to be non-blank if specified when building KeyValueProducer") {
    intercept[IllegalArgumentException] {
      KeyValueProducer
        .builder[String, DummyKafkaRequest, StringSerializer, DummyKafkaRequestSerializer.type]()
        .addBootstrapServer("host", 666)
        .clientId("")
        .build()
    }
  }

  test("requiring at least 1 bootstrap server when building KeylessProducer") {
    intercept[IllegalArgumentException] {
      KeylessProducer
        .builder[DummyKafkaRequest, DummyKafkaRequestSerializer.type]()
        .build()
    }
  }

  test("requiring client.id to be non-blank if specified when building KeylessProducer") {
    intercept[IllegalArgumentException] {
      KeylessProducer
        .builder[DummyKafkaRequest, DummyKafkaRequestSerializer.type]()
        .addBootstrapServer("host", 666)
        .clientId("")
        .build()
    }
  }

  test("adding multiple ProducerConfigs") {
    val servers = Seq(LocalHostPort(999), LocalHostPort(9291), HostPort("host1", 921), HostPort("host2", 922))
    val configs = PropertiesBuilder()
      .addOrOverwrite(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers.mkString(","))
      .addOrOverwrite(ProducerConfig.MAX_BLOCK_MS_CONFIG, 100)
      .buildAsImmutableProperties

    val p = KeylessProducer
      .builder[DummyKafkaRequest, DummyKafkaRequestSerializer.type]()
      .addBootstrapServer("host", 666) // should be overwritten
      .addProducerProperties(configs)
      .build()

    val expectedServersConfig = servers.map(_.formatted).sorted.mkString(",")
    assert(p.producerProps.stringValueOf(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG) === expectedServersConfig)
    assert(p.producerProps.intValueOf(ProducerConfig.MAX_BLOCK_MS_CONFIG) === 100)
  }

  test("KeyValueProducer: equals/hashCode") {
    val p1 = KeyValueProducer
      .builder[String, DummyKafkaRequest, StringSerializer, DummyKafkaRequestSerializer.type]()
      .addBootstrapServer("host", 666)
      .build()
    val p2 = KeyValueProducer
      .builder[String, DummyKafkaRequest, StringSerializer, DummyKafkaRequestSerializer.type]()
      .addBootstrapServer("host", 666)
      .build()
    assert(p1 === p2)
    assert(p1.hashCode() === p2.hashCode())

    val p3 = KeylessProducer
      .builder[DummyKafkaRequest, DummyKafkaRequestSerializer.type]()
      .addBootstrapServer("host", 666)
      .build()

    // cast to supertype
    val sp1 = p1.asInstanceOf[Producer[DummyKafkaRequest, DummyKafkaRequestSerializer.type]]
    val sp2 = p2.asInstanceOf[Producer[DummyKafkaRequest, DummyKafkaRequestSerializer.type]]
    val sp3 = p3.asInstanceOf[Producer[DummyKafkaRequest, DummyKafkaRequestSerializer.type]]
    assert(sp1 !== sp3)
    assert(sp3 !== sp1)
    assert(sp2 !== sp3)
    assert(sp3 !== sp2)
  }

  test("KeylessProducer: equals/hashCode") {
    val p1 = KeylessProducer
      .builder[DummyKafkaRequest, DummyKafkaRequestSerializer.type]()
      .addBootstrapServer("host", 666)
      .build()
    val p2 = KeylessProducer
      .builder[DummyKafkaRequest, DummyKafkaRequestSerializer.type]()
      .addBootstrapServer("host", 666)
      .build()
    assert(p1 === p2)
    assert(p1.hashCode() === p2.hashCode())

    val p3 = KeyValueProducer
      .builder[String, DummyKafkaRequest, StringSerializer, DummyKafkaRequestSerializer.type]()
      .addBootstrapServer("host", 666)
      .build()

    // cast to supertype
    val sp1 = p1.asInstanceOf[Producer[DummyKafkaRequest, DummyKafkaRequestSerializer.type]]
    val sp2 = p2.asInstanceOf[Producer[DummyKafkaRequest, DummyKafkaRequestSerializer.type]]
    val sp3 = p3.asInstanceOf[Producer[DummyKafkaRequest, DummyKafkaRequestSerializer.type]]
    assert(sp1 !== sp3)
    assert(sp3 !== sp1)
    assert(sp2 !== sp3)
    assert(sp3 !== sp2)
  }
}
