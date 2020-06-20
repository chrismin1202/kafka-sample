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
package com.chrism.config

import com.chrism.commons.FunTestSuite

final class SystemPropertyTest extends FunTestSuite {

  test("instantiating primitive types") {
    assert(SystemProperty.ofBoolean("bool", value = false).value.toBoolean === false)
    assert(SystemProperty.ofInt("int", 1).value.toInt === 1)
    assert(SystemProperty.ofLong("long", 2L).value.toLong === 2L)
    assertFloat(3.0f, SystemProperty.ofFloat("float", 3.0f).value.toFloat)
    assertDouble(4.0, SystemProperty.ofDouble("double", 4.0).value.toDouble)
  }

  test("parsing 1 property: simple name-value pair without any space") {
    val rawProp = "chrism.key=val"
    val expectedProp = SystemProperty("chrism.key", "val")
    assertOption(expectedProp, SystemProperty.parse(rawProp))

    val expectedProps = Seq(expectedProp)
    SystemProperty.parseAll(rawProp) should contain theSameElementsInOrderAs expectedProps
  }

  test("parsing 1 property: with spaces between name and value") {
    val rawProp = "-Dchrism.key : val"
    val expectedProp = SystemProperty("chrism.key", "val")
    assertOption(expectedProp, SystemProperty.parse(rawProp))

    val expectedProps = Seq(expectedProp)
    SystemProperty.parseAll(rawProp) should contain theSameElementsInOrderAs expectedProps
  }

  test("parsing 1 property: with spaces within property value") {
    val rawProp = "-Dchrism.key=val1.1 val1.2 val1.3  "
    val expectedProp = SystemProperty("chrism.key", "val1.1 val1.2 val1.3")
    assertOption(expectedProp, SystemProperty.parse(rawProp))

    val expectedProps = Seq(expectedProp)
    SystemProperty.parseAll(rawProp) should contain theSameElementsInOrderAs expectedProps
  }

  test("parsing 1 property: with spaces within property value as well as between name and value") {
    val rawProp = " -Dchrism.key : val1.1 val1.2 val1.3\t"
    val expectedProp = SystemProperty("chrism.key", "val1.1 val1.2 val1.3")
    assertOption(expectedProp, SystemProperty.parse(rawProp))

    val expectedProps = Seq(expectedProp)
    SystemProperty.parseAll(rawProp) should contain theSameElementsInOrderAs expectedProps
  }

  test("parsing multiple properties: without any spaces within property values") {
    val actual = SystemProperty.parseAll("chrism.key1=val1 -Dchrism.key2=val2  chrism.key3:val3\n-Dchrism.key4:val4")
    val expected = Seq(
      SystemProperty("chrism.key1", "val1"),
      SystemProperty("chrism.key2", "val2"),
      SystemProperty("chrism.key3", "val3"),
      SystemProperty("chrism.key4", "val4"),
    )
    actual should contain theSameElementsInOrderAs expected
  }

  test("parsing multiple properties: with spaces within property values") {
    val actual = SystemProperty.parseAll(
      "chrism.key1=val1.1 val1.2\t-Dchrism.key2=val2.1 val2.2\nchrism.key3:val3  -Dchrism.key4:val4.1 val4.2  ")
    val expected = Seq(
      SystemProperty("chrism.key1", "val1.1 val1.2"),
      SystemProperty("chrism.key2", "val2.1 val2.2"),
      SystemProperty("chrism.key3", "val3"),
      SystemProperty("chrism.key4", "val4.1 val4.2"),
    )
    actual should contain theSameElementsInOrderAs expected
  }

  test("parsing multiple properties: with spaces within property values as well as between name and value") {
    val actual = SystemProperty.parseAll(
      "  \tchrism.key1=val1.1 val1.2   \t-Dchrism.key2=val2.1 val2.2 \n -Dchrism.key3:val3.1 val3.2  ")
    val expected = Seq(
      SystemProperty("chrism.key1", "val1.1 val1.2"),
      SystemProperty("chrism.key2", "val2.1 val2.2"),
      SystemProperty("chrism.key3", "val3.1 val3.2"),
    )
    actual should contain theSameElementsInOrderAs expected
  }

  test("formatting property") {
    val actual = SystemProperty("chrism.key1", "val1.1 val1.2").formatted
    val expected = "-Dchrism.key1=val1.1 val1.2"
    assert(actual === expected)
  }

  test("sorting SystemProperty instances") {
    val props = Seq(
      SystemProperty("k3", "v3"),
      SystemProperty("k2", "v2"),
      SystemProperty("k4", "v4"),
      SystemProperty("k1", "v1"),
    )
    props.sorted should contain theSameElementsInOrderAs Seq(
      SystemProperty("k1", "v1"),
      SystemProperty("k2", "v2"),
      SystemProperty("k3", "v3"),
      SystemProperty("k4", "v4"),
    )
  }
}
