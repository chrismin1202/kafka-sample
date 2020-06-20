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

final class CommandLineOptionsTest extends FunTestSuite {

  test("2 options with the same short option name cannot be added") {
    intercept[IllegalArgumentException] {
      CommandLineOptions.builder
        .add(CommandLineOption.builder.shortOption("s").longOption("lo1").build())
        .add(CommandLineOption.builder.shortOption("s").longOption("lo2").build())
    }
  }

  test("2 options with the same long option name cannot be added") {
    intercept[IllegalArgumentException] {
      CommandLineOptions.builder
        .add(CommandLineOption.builder.shortOption("s1").longOption("lo").build())
        .add(CommandLineOption.builder.shortOption("s2").longOption("lo").build())
    }
  }

  test("HelpOption is added by default") {
    val opts = CommandLineOptions.builder
      .add(CommandLineOption.builder.shortOption("s").longOption("lo").build())
      .build()
    assert(opts.contains(HelpOption))
  }

  test("accessing option that does not exist") {
    val opt = CommandLineOption.builder.shortOption("s").longOption("lo").build()
    val opts = CommandLineOptions.builder.add(opt).build()

    assert(opts("s") === opt)
    assert(opts("lo") === opt)

    assertOption(opt, opts.get("s"))
    assertOption(opt, opts.get("lo"))

    intercept[NoSuchElementException] {
      opts("missing-option")
    }

    assert(opts.get("missing-option").isEmpty)
  }

  test("finding CommandLineOption by short name") {
    val opt1 = CommandLineOption.builder.shortOption("s1").build()
    val opt2 = CommandLineOption.builder.shortOption("s2").build()
    val opt3 = CommandLineOption.builder.shortOption("s3").build()
    val opts = CommandLineOptions.builder
      .add(opt1)
      .add(opt2)
      .add(opt3)
      .build()
    assertOption(opt1, opts.get("s1"))
    assertOption(opt2, opts.get("s2"))
    assertOption(opt3, opts.get("s3"))
  }

  test("finding CommandLineOption by long name") {
    val opt1 = CommandLineOption.builder.longOption("lo1").build()
    val opt2 = CommandLineOption.builder.longOption("lo2").build()
    val opt3 = CommandLineOption.builder.longOption("lo3").build()
    val opts = CommandLineOptions.builder
      .add(opt1)
      .add(opt2)
      .add(opt3)
      .build()
    assertOption(opt1, opts.get("lo1"))
    assertOption(opt2, opts.get("lo2"))
    assertOption(opt3, opts.get("lo3"))
  }

  test("equals/hashCode") {
    val opts1 = CommandLineOptions.builder
      .add(CommandLineOption.builder.shortOption("s1").build())
      .add(CommandLineOption.builder.longOption("lo2").build())
      .add(CommandLineOption.builder.shortOption("s3").longOption("lo3").build())
      .build()
    // identical options added in different order
    val opts2 = CommandLineOptions.builder
      .add(CommandLineOption.builder.shortOption("s3").longOption("lo3").build())
      .add(CommandLineOption.builder.shortOption("s1").build())
      .add(CommandLineOption.builder.longOption("lo2").build())
      .build()
    assert(opts1 === opts2)
    assert(opts1.hashCode() === opts2.hashCode())
  }
}
