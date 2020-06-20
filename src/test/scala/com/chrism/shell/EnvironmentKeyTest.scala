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
package com.chrism.shell

import com.chrism.commons.FunTestSuite
import com.chrism.config.{SystemProperty, SystemPropertyFunTestSuiteLike}

final class EnvironmentKeyTest extends FunTestSuite with SystemPropertyFunTestSuiteLike {

  test("extracting Java system property values") {
    withSysProps(
      SystemProperty("chrism.str", "Saul Goodman"),
      SystemProperty.ofInt("chrism.num", 1),
      SystemProperty.ofBoolean("chrism.bool", value = false),
    ) {
      assert(SystemPropertyKey("chrism.str").rawValue() === "Saul Goodman")
      assert(SystemPropertyKey("chrism.num").intValue() === 1)
      assert(SystemPropertyKey("chrism.bool").booleanValue() === false)
    }
  }

  test("environment variable can be absent as long as Java system property values exist") {
    withSysProps(
      SystemProperty("chrism.str", "James McGill"),
      SystemProperty.ofInt("chrism.num", 2),
      SystemProperty.ofDouble("chrism.double", 3.0),
    ) {
      assert(CombinedEnvironmentKey.of("CHRISM_STR", "chrism.str").rawValue() === "James McGill")
      assert(CombinedEnvironmentKey.of("CHRISM_NUM", "chrism.num").intValue() === 2)
      assertDouble(3.0, CombinedEnvironmentKey.of("CHRISM_DOUBLE", "chrism.double").doubleValue())
    }
  }
}
