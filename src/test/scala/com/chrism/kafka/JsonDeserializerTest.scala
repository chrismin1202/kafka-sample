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

final class JsonDeserializerTest extends FunTestSuite {

  test("deserializing request with nullable fields populated") {
    val requestJson =
      """{
        |  "string_param": "string",
        |  "nullable_string_param": "string",
        |  "int_param": 1,
        |  "nullable_int_param": 1,
        |  "boolean_param": false,
        |  "nullable_boolean_param": true
        |}""".stripMargin
    val request = deserialize(requestJson)
    val expected = DummyKafkaRequest("string", Some("string"), 1, Some(1), booleanParam = false, Some(true))
    assert(request === expected)
  }

  test("deserializing request without nullable fields populated") {
    val requestJson =
      """{
        |  "string_param": "string",
        |  "int_param": 1,
        |  "boolean_param": false
        |}""".stripMargin
    val request = deserialize(requestJson)
    val expected = DummyKafkaRequest("string", None, 1, None, booleanParam = false, None)
    assert(request === expected)
  }

  private def deserialize(json: String): DummyKafkaRequest =
    DummyKafkaRequestDeserializer.deserialize(null, json.getBytes("UTF-8"))
}
