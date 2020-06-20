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

final class SysPropsTest extends FunTestSuite {

  test("get: MissingSystemPropertyException is thrown when the property is missing") {
    intercept[MissingSystemPropertyException] {
      SysProps.get("name that does not exist")
    }
  }

  test("getSeq: MissingSystemPropertyException is thrown when the property is missing") {
    intercept[MissingSystemPropertyException] {
      SysProps.getSeq("name that does not exist")
    }
  }

  test("getOrNone: None is returned when missing property") {
    assert(SysProps.getOrNone("name that does not exist").isEmpty)
  }

  test("getSeqOrNone: None is returned when missing property") {
    assert(SysProps.getSeqOrNone("name that does not exist").isEmpty)
  }

  test("getAs*: missing property") {
    val name = "chrism.intProp"
    assert(SysProps.getAsOrNone(name)(_.toInt).isEmpty)
    intercept[MissingSystemPropertyException] {
      SysProps.getAs(name)(_.toInt)
    }
    assert(SysProps.getAsOrDefault(name, 666)(_.toInt) === 666)
  }

  test("getAsSeq*: missing property") {
    val name = "chrism.arrayProp"
    assert(SysProps.getAsSeqOrNone(name)(_.toInt).isEmpty)
    intercept[MissingSystemPropertyException] {
      SysProps.getAsSeq(name)(_.toInt)
    }
  }

  test("getAs*: existing property") {
    val name = "chrism.longProp"
    val prop = 100L

    SysProps.addIfNotExists(name, prop.toString)

    val convert = (v: String) => v.toLong
    assertOption(prop, SysProps.getAsOrNone(name)(convert))
    assert(SysProps.getAs(name)(convert) === prop)

    val timestampOrDefault = SysProps.getAsOrDefault(name, 200L)(convert)
    assert(timestampOrDefault === prop)

    SysProps.remove(name)
  }

  test("getAsSeq*: existing property") {
    val name = "chrism.arrayProp"
    val prop = Seq(1, 2, 3, 4)

    SysProps.convertThenAddDelimitedIfNotExists(name, prop)()

    val convert = (v: String) => v.toLong
    SysProps.getAsSeqOrNone(name)(convert).getOrFail() should contain theSameElementsInOrderAs prop
    SysProps.getAsSeq(name)(convert) should contain theSameElementsInOrderAs prop

    SysProps.remove(name)
  }

  test("overwriting existing system property") {
    val name = "chrism.prop"

    val originalValue = "value"
    SysProps.addOrOverwrite(name, originalValue)
    assert(SysProps.get(name) === originalValue)

    val newValue = "new value"
    // addIfNotExists should not overwrite
    SysProps.addIfNotExists(name, newValue)
    assert(SysProps.get(name) === originalValue)
    // whereas addOrOverwrite should
    SysProps.addOrOverwrite(name, newValue)
    assert(SysProps.get(name) === newValue)
  }

  test("manipulating system property at runtime") {
    val name = "chrism.prop1"
    val value = "value1"

    // make sure that the property does not exist yet
    assert(SysProps.getOrNone(name).isEmpty)

    // add the property
    SysProps.addIfNotExists(name, value)

    // the value now exists
    assert(SysProps.get(name) === value)

    // un-pollute sys.props by removing the value
    SysProps.remove(name)
    assert(SysProps.getOrNone(name).isEmpty)
  }

  test("findAll: starts with a certain prefix") {
    val props = Seq(
      ("chrism.findAll.prop1", "value1"),
      ("chrism.findAll.prop2", "value2"),
      ("chrism.findAll.prop3", "value3"),
    )

    // make sure that the properties with the same name do not exist yet
    assert(props.map(_._1).flatMap(SysProps.getOrNone).isEmpty)

    // add the property
    props.foreach(p => SysProps.addIfNotExists(p._1, p._2))

    SysProps.findAll(_._1.startsWith("chrism.findAll")) should contain theSameElementsAs props

    // un-pollute sys.props by removing the value
    props.map(_._1).foreach(SysProps.remove)

    // make sure that all properties are removed
    assert(props.map(_._1).flatMap(SysProps.getOrNone).isEmpty)
  }
}
