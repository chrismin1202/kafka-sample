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
package com.chrism.cli

import com.chrism.commons.FunTestSuite

final class ArgumentRangeTest extends FunTestSuite {

  test("requiring that min is non-negative") {
    intercept[IllegalArgumentException] {
      ArgumentRange(-1, 1)
    }
  }

  test("requiring that max is non-negative") {
    intercept[IllegalArgumentException] {
      ArgumentRange(0, -1)
    }
  }

  test("requiring that the maximum number of arguments is at least the minimum") {
    intercept[IllegalArgumentException] {
      ArgumentRange(2, 1)
    }
  }

  test("returning ZeroArgument from ArgumentRange factory") {
    assert(ArgumentRange(0, 0) eq ZeroArgument)
  }

  test("returning OneArgument from ArgumentRange factory") {
    assert(ArgumentRange(1, 1) eq OneArgument)
  }

  test("returning FixedArgumentRange from ArgumentRange factory") {
    val range = ArgumentRange(2, 2)
    assert(range === FixedArgumentRange(2))
  }

  test("toString returns mathematical range for variable argument range") {
    assert(ArgumentRange(1, 2).toString === "[1,2]")
  }

  test("ZeroArgument.toString returns None") {
    assert(ZeroArgument.toString === "None")
  }

  test("toString returns the number of arguments for FixedArgumentRange") {
    assert(ArgumentRange(1, 1).toString === "1")
  }
}
