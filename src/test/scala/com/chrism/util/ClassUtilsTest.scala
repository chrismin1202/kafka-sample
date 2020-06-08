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

import com.chrism.commons.FunTestSuite

final class ClassUtilsTest extends FunTestSuite {

  test("equality between class and its companion object") {
    assert(ClassUtils.nameEquals(getClass, ClassUtilsTest.getClass))
  }

  test("dropping $ from object") {
    val objClazz = ClassUtilsTest.getClass
    assert(objClazz.getName === "com.chrism.util.ClassUtilsTest$")
    assert(ClassUtils.classNameOf(objClazz) === "com.chrism.util.ClassUtilsTest")
  }

  test("reflectively retrieving Class from its name") {
    val clazz = ClassUtils.fromName("com.chrism.util.ClassUtilsTest")
    assert(ClassUtils.nameEquals(clazz, getClass))
  }

  test("reflectively retrieving `object` instance") {
    val obj = ClassUtils.objectInstanceOf[ClassUtilsTest.type](ClassUtilsTest.getClass)
    assert(obj.concatenate("Los ", "Pollos ", "Hermanos") === "Los Pollos Hermanos")
  }
}

private[this] object ClassUtilsTest {

  private def concatenate(a: String, more: String*): String = (a +: more).mkString("")
}
