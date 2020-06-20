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
package com.chrism.kafka.serde

import com.chrism.commons.FunTestSuite

final class NumberSerDeTest extends FunTestSuite {

  test("serializing and deserializing scala Short") {
    val ser = ShortSerializer()
    val de = ShortDeserializer()
    val data = 1.toShort
    val serialized = ser.serialize(null, data)
    val deserialized = de.deserialize(null, serialized)
    assert(deserialized === data)
  }

  test("serializing and deserializing scala Int") {
    val ser = IntSerializer()
    val de = IntDeserializer()
    val data = 2
    val serialized = ser.serialize(null, data)
    val deserialized = de.deserialize(null, serialized)
    assert(deserialized === data)
  }

  test("serializing and deserializing scala Long") {
    val ser = LongSerializer()
    val de = LongDeserializer()
    val data = 3L
    val serialized = ser.serialize(null, data)
    val deserialized = de.deserialize(null, serialized)
    assert(deserialized === data)
  }

  test("serializing and deserializing scala Float") {
    val ser = FloatSerializer()
    val de = FloatDeserializer()
    val data = 4.0f
    val serialized = ser.serialize(null, data)
    val deserialized = de.deserialize(null, serialized)
    assertFloat(data, deserialized)
  }

  test("serializing and deserializing scala Double") {
    val ser = DoubleSerializer()
    val de = DoubleDeserializer()
    val data = 5.0
    val serialized = ser.serialize(null, data)
    val deserialized = de.deserialize(null, serialized)
    assertDouble(data, deserialized)
  }
}
