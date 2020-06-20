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

final class ArgumentBuilderTest extends FunTestSuite {

  import ArgumentBuilderTest._

  test("building arguments: long options") {
    val args = ArgumentBuilder()
      .add(noArgOption)
      .add(singleArgOption, SingleArgument("a"))
      .add(multiArgOption, MultiArgument("a1", "a2", "a3"))
      .build()
    args should contain theSameElementsInOrderAs Seq("--multi-arg", "a1", "a2", "a3", "--no-arg", "--single-arg", "a")
  }

  test("building arguments: short options") {
    val args = ArgumentBuilder()
      .add(noArgShortOption)
      .add(singleArgShortOption, SingleArgument("a"))
      .add(multiArgShortOption, MultiArgument("a1", "a2", "a3", "a4", "a5"))
      .build()
    args should contain theSameElementsInOrderAs Seq("-ma", "a1", "a2", "a3", "a4", "a5", "-na", "-sa", "a")
  }

  test("building arguments: long options takes precedence") {
    val args = ArgumentBuilder()
      .add(noArgBothOption)
      .add(singleArgBothOption, SingleArgument("a"))
      .add(multiArgBothOption, MultiArgument("a1", "a2", "a3", "a4"))
      .build()
    args should contain theSameElementsInOrderAs Seq(
      "--multi-arg",
      "a1",
      "a2",
      "a3",
      "a4",
      "--no-arg",
      "--single-arg",
      "a",
    )
  }

  test("building arguments: argument range below minimum") {
    intercept[IllegalArgumentException] {
      ArgumentBuilder()
        .add(oneOrMoreArgOption, Argumentless)
        .build()
    }
  }

  test("building arguments: argument range above maximum") {
    intercept[IllegalArgumentException] {
      ArgumentBuilder()
        .add(oneOrMoreArgOption, MultiArgument("a1", "a4", "a3", "a4"))
        .build()
    }
  }

  test("building arguments: zero or more arguments") {
    ArgumentBuilder().add(zeroOrMoreArgOption).build() should contain theSameElementsInOrderAs Seq("--zero-more")
  }

  test("building arguments: with pass-through arguments") {
    val passThroughArgs = Array("pta1", "pta2", "pta3", "pta4")
    val builder = ArgumentBuilder()
      .add(noArgOption)
      .add(singleArgBothOption, SingleArgument("a"))
      .add(multiArgShortOption, MultiArgument("a1", "a2", "a3", "a4", "a5"))
      .passThroughArgs(passThroughArgs)
    // mutating passThroughArgs after calling .passThroughArgs() to make sure that defensive copy has been created
    passThroughArgs(0) = "pta0"

    builder.build() should contain theSameElementsInOrderAs Seq(
      "--no-arg",
      "--single-arg",
      "a",
      "-ma",
      "a1",
      "a2",
      "a3",
      "a4",
      "a5",
      "--",
      "pta1",
      "pta2",
      "pta3",
      "pta4",
    )
  }
}

private[this] object ArgumentBuilderTest {

  private val noArgOption: CommandLineOption = CommandLineOption.builder
    .longOption("no-arg")
    .build()

  private val singleArgOption: CommandLineOption = CommandLineOption.builder
    .longOption("single-arg")
    .argumentRange(1)
    .build()

  private val multiArgOption: CommandLineOption = CommandLineOption.builder
    .longOption("multi-arg")
    .argumentRange(3, 5)
    .build()

  private val noArgShortOption: CommandLineOption = CommandLineOption.builder
    .shortOption("na")
    .build()

  private val singleArgShortOption: CommandLineOption = CommandLineOption.builder
    .shortOption("sa")
    .argumentRange(1)
    .build()

  private val multiArgShortOption: CommandLineOption = CommandLineOption.builder
    .shortOption("ma")
    .argumentRange(3, 5)
    .build()

  private val noArgBothOption: CommandLineOption = CommandLineOption.builder
    .longOption("no-arg")
    .shortOption("na")
    .build()

  private val singleArgBothOption: CommandLineOption = CommandLineOption.builder
    .longOption("single-arg")
    .shortOption("sa")
    .argumentRange(1)
    .build()

  private val multiArgBothOption: CommandLineOption = CommandLineOption.builder
    .longOption("multi-arg")
    .shortOption("ma")
    .argumentRange(3, 5)
    .build()

  private val oneOrMoreArgOption: CommandLineOption = CommandLineOption.builder
    .longOption("one-more")
    .shortOption("om")
    .argumentRange(1, 3)
    .build()

  private val zeroOrMoreArgOption: CommandLineOption = CommandLineOption.builder
    .longOption("zero-more")
    .shortOption("zm")
    .argumentRange(0, 2)
    .build()
}
