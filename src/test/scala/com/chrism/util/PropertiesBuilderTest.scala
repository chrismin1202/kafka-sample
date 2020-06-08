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
package com.chrism.util

import java.{util => ju}

import com.chrism.commons.FunTestSuite

final class PropertiesBuilderTest extends FunTestSuite {

  import PropertiesUtils.implicits._

  test("building a new set of properties") {
    val builder = PropertiesBuilder() +=
      "k" -> "v" +=
      1 -> 2 +=
      false -> false

    val jProps = builder.buildAsJProperties
    assert(jProps.getString("k") === "v")
    assert(jProps.getInt(1) === 2)
    assert(jProps.getBoolean(false) === false)

    val immProps = builder.buildAsImmutableProperties
    val expected = ImmutableProperties(
      Map(
        "k" -> "v",
        1 -> 2,
        false -> false,
      ))
    assert(immProps === expected)
  }

  test("adding more properties to the given properties") {
    val originalProps = new ju.Properties()
    originalProps += "k1" -> "v1"
    originalProps += 1 -> false
    originalProps += true -> 2L

    val builder = PropertiesBuilder(originalProps) +=
      "k1" -> "v2" +=
      2L -> 4L +=
      true -> 4

    val jProps = builder.buildAsJProperties
    assert(jProps.getString("k1") === "v2")
    assert(jProps.getBoolean(1) === false)
    assert(jProps.getInt(true) === 4)
    assert(jProps.getLong(2L) === 4L)

    val immProps = builder.buildAsImmutableProperties
    val expected = ImmutableProperties(
      Map(
        "k1" -> "v2", // from v1 to v2
        1 -> false, // unchanged
        2L -> 4L, // newly added
        true -> 4, // from 2L to 4
      ))
    assert(immProps === expected)
  }

  test("adding multiple properties in one go") {
    val builder = PropertiesBuilder() +=
      "k" -> "v" +=
      1 -> 2 +=
      false -> false

    builder ++= Map("k" -> "newV", 3L -> 4.0, false -> "FALSE")

    val immProps = builder.buildAsImmutableProperties
    val expected = ImmutableProperties(
      Map(
        "k" -> "newV",
        1 -> 2,
        3L -> 4.0,
        false -> "FALSE",
      ))
    assert(immProps === expected)
  }
}
