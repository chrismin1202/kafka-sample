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

final class OptionValidatorTest extends FunTestSuite {

  test("validating short option: blank") {
    intercept[InvalidOptionException] {
      OptionValidator.validateShortOption("")
    }
  }

  test("validating short option: containing non-alphanumeric character") {
    intercept[InvalidOptionException] {
      OptionValidator.validateShortOption("%")
    }
  }

  test("validating long option: blank") {
    intercept[InvalidOptionException] {
      OptionValidator.validateLongOption("")
    }
  }

  test("validating long option: can contain hyphens but cannot start with hyphen") {
    // no exception should be thrown
    OptionValidator.validateLongOption("a-b")

    intercept[InvalidOptionException] {
      OptionValidator.validateLongOption("-b")
    }
  }
}
