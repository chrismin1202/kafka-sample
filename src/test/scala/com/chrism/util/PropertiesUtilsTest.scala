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

final class PropertiesUtilsTest extends FunTestSuite {

  import PropertiesUtils.implicits._

  test("handling AnyVal subtypes") {
    val props = new ju.Properties()
    props += 1 -> 4
    assert(props.getInt(1) === 4)

    intercept[NoSuchElementException] {
      props.getInt(2)
    }
    assert(props.getIntOrNone(2).isEmpty)

    props += 2 -> 7L
    assert(props.getLong(2) === 7L)

    props += "k" -> false
    assert(props.getBoolean("k") === false)
  }
}
