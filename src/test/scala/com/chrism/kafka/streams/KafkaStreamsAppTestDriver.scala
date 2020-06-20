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
package com.chrism.kafka.streams

import org.apache.kafka.streams.{KeyValue, TestInputTopic, TestOutputTopic, TopologyTestDriver}

import scala.collection.JavaConverters._
import scala.collection.mutable

final class KafkaStreamsAppTestDriver[KIn, VIn, KOut, VOut] private[streams] (
  val app: KafkaStreamsAppLike[KIn, VIn, KOut, VOut])
    extends AutoCloseable {

  private[this] var _closed: Boolean = false
  private[this] var _driver: TopologyTestDriver = _
  private[this] var _inputTopics: Map[String, TestInputTopic[KIn, VIn]] = _
  private[this] var _outputTopics: Map[String, TestOutputTopic[KOut, VOut]] = _

  private[this] def setup(): Unit =
    if (_driver == null) {
      if (_closed) {
        throw new IllegalStateException("The application ahs already been closed!")
      }

      import KafkaStreamsTestUtils.implicits._

      _driver = new TopologyTestDriver(app.buildTopology(), app.topologyConfig.toJProperties)
      _inputTopics = app.createInputTopics(_driver)
      _outputTopics = app.createOutputTopics(_driver)
    }

  def sendKeyValue(topic: String, k: KIn, v: VIn): Unit = inputTopic(topic).pipeInput(k, v)

  def sendValue(topic: String, v: VIn): Unit = inputTopic(topic).pipeInput(v)

  def sendKeyValues(topic: String)(kv: (KIn, VIn), more: (KIn, VIn)*): Unit = sendKeyValues(topic, kv +: more)

  def sendKeyValues(topic: String, kvs: Seq[(KIn, VIn)]): Unit =
    inputTopic(topic).pipeKeyValueList(mutable.Buffer(kvs: _*).map(_.asKeyValue).asJava)

  def sendValues(topic: String)(v: VIn, more: VIn*): Unit = sendValues(topic, v +: more)

  def sendValues(topic: String, values: Seq[VIn]): Unit =
    inputTopic(topic).pipeValueList(mutable.Buffer(values: _*).asJava)

  def consumeKeyValue(topic: String): (KOut, VOut) = outputTopic(topic).readKeyValue().asTuple

  def consumeKeyValues(topic: String): Seq[(KOut, VOut)] =
    outputTopic(topic).readKeyValuesToList().asScala.map(_.asTuple)

  def driver(): TopologyTestDriver = {
    setup()
    _driver
  }

  def inputTopics(): Map[String, TestInputTopic[KIn, VIn]] = {
    setup()
    _inputTopics
  }

  def inputTopic(name: String): TestInputTopic[KIn, VIn] = inputTopics()(name)

  def outputTopics(): Map[String, TestOutputTopic[KOut, VOut]] = {
    setup()
    _outputTopics
  }

  def outputTopic(name: String): TestOutputTopic[KOut, VOut] = outputTopics()(name)

  override def close(): Unit =
    if (!_closed) {
      _driver.close()
      _driver = null
      _closed = true
    }

  private[this] implicit final class PairOps[K, V](kv: (K, V)) {

    def asKeyValue: KeyValue[K, V] = KeyValue.pair(kv._1, kv._2)
  }

  private[this] implicit final class KeyValueOps[K, V](kv: KeyValue[K, V]) {

    def asTuple: (K, V) = (kv.key, kv.value)
  }
}
