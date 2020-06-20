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
import com.chrism.shell.SystemPropertyKey

final class CommandLineOptionTest extends FunTestSuite {

  test("leading whitespace is not allowed") {
    intercept[IllegalArgumentException] {
      CommandLineOption.builder.shortOption(" s")
    }

    intercept[IllegalArgumentException] {
      CommandLineOption.builder.longOption(" l-o")
    }
  }

  test("trailing whitespace is not allowed") {
    intercept[IllegalArgumentException] {
      CommandLineOption.builder.shortOption("s ")
    }

    intercept[IllegalArgumentException] {
      CommandLineOption.builder.longOption("l-o ")
    }
  }

  test("either short or long option must be specified") {
    intercept[IllegalArgumentException] {
      CommandLineOption.builder.build()
    }

    intercept[IllegalArgumentException] {
      CommandLineOption.builder.shortOption(null).longOption(null).build()
    }
  }

  test("building CommandLineOption with only short option") {
    val opt = CommandLineOption.builder.shortOption("s").build()
    assertOption("s", opt.shortOpt)
    assert(opt.longOpt.isEmpty)
  }

  test("building CommandLineOption with only long option") {
    val opt = CommandLineOption.builder.longOption("long-option").build()
    assert(opt.shortOpt.isEmpty)
    assertOption("long-option", opt.longOpt)
  }

  test("-h is a reserved short name") {
    intercept[IllegalArgumentException] {
      CommandLineOption.builder.shortOption("h").build()
    }
  }

  test("--help is a reserved long name") {
    intercept[IllegalArgumentException] {
      CommandLineOption.builder.longOption("help").build()
    }
  }

  test("copying an instance") {
    val opt = CommandLineOption.builder
      .longOption("long-option")
      .description("A long option with a lot of arguments")
      .argumentRange(Int.MaxValue)
      .build()

    val copied = opt.copy(
      shortOpt = Some("s"),
      required = true,
      argRange = FixedArgumentRange(4),
      description = Some("A long option with exactly 4 arguments"),
      sysPropKey = Some(SystemPropertyKey("chrism.s"))
    )
    val expected = CommandLineOption.builder
      .shortOption("s")
      .longOption("long-option")
      .description("A long option with exactly 4 arguments")
      .argumentRange(4)
      .required(true)
      .systemPropertyKey(SystemPropertyKey("chrism.s"))
      .build()
    assert(copied === expected)
  }

  test("HelpOption is not copyable") {
    intercept[UnsupportedOperationException] {
      HelpOption.copy()
    }
  }

  test("equals/hashCode") {
    val opt1 = CommandLineOption.builder
      .shortOption("s")
      .longOption("long-option")
      .required(true)
      .description("A long option with a lot of arguments")
      .argumentRange(Int.MaxValue)
      .build()
    val opt2 = CommandLineOption.builder
      .shortOption("s")
      .longOption("long-option")
      .required(true)
      .description("A long option with a lot of arguments")
      .argumentRange(Int.MaxValue)
      .build()
    assert(opt1 === opt2)
    assert(opt1.hashCode() === opt2.hashCode())
  }
}
