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

final class ImmutablePropertiesTest extends FunTestSuite {

  import PropertiesUtils.implicits._

  test("converting to java.util.Properties") {
    val props = ImmutableProperties(
      Map(
        "k" -> "v",
        1 -> 2,
        3L -> 4L,
        false -> true,
      ))

    val jProps = props.toJProperties
    assert(jProps.getString("k") === "v")
    assert(jProps.getInt(1) === 2)
    assert(jProps.getLong(3L) === 4L)
    assert(jProps.getBoolean(false) === true)
  }

  test("appending additional properties via PropertiesBuilder does not alter the instance") {
    val props = ImmutableProperties(
      Map(
        "k" -> "v",
        1 -> 2,
        3L -> 4L,
        false -> true,
      ))

    val builder = PropertiesBuilder(props) += (5.0 -> 6.0f)
    val newProps = builder.buildAsImmutableProperties
    assert(props.size !== newProps.size)
    assert(props !== newProps)
  }
}
