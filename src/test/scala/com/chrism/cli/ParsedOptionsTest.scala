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
import com.chrism.config.{SystemProperty, SystemPropertyFunTestSuiteLike}
import com.chrism.shell.{DelimitedSystemPropertyKey, SystemPropertyKey}

final class ParsedOptionsTest extends FunTestSuite with SystemPropertyFunTestSuiteLike {

  test("missing required option results in MissingRequiredOptionException") {
    val args = Array("--o1", "a1", "a2")
    val opt1 = CommandLineOption.builder.longOption("o1").argumentRange(2).build()
    val opt2 = CommandLineOption.builder.shortOption("o2").required(true).build()
    val opts = CommandLineOptions.builder
      .add(opt1)
      .add(opt2)
      .build()

    intercept[MissingRequiredOptionException] {
      ParsedOptions(args, opts).stringArgOfOrNone(opt2)
    }

    // argument range is checked first in this case
    intercept[UnsupportedOperationException] {
      ParsedOptions(args, opts).stringArgOf(opt2)
    }
  }

  test("ArgumentParsingException is thrown when the number of arguments is not within the range") {
    val args = Array("--o1", "a1", "a2")
    val o1 = CommandLineOption.builder.longOption("o1").argumentRange(3, 5).build()
    val opts = CommandLineOptions.builder
      .add(o1)
      .build()

    intercept[ArgumentParsingException] {
      ParsedOptions(args, opts).stringArgsOf(o1)
    }
  }

  test("parsing arguments with --") {
    val args = Array("--o1", "a1", "a2", "-o2", "-o3", "a", "--o4", "--", "pass", "through")
    val opt1 = CommandLineOption.builder.longOption("o1").argumentRange(1, 2).build()
    val opt2 = CommandLineOption.builder.shortOption("o2").build()
    val opt3 = CommandLineOption.builder.shortOption("o3").argumentRange(1).required(true).build()
    val opt4 = CommandLineOption.builder.longOption("o4").argumentRange(0, 4).required(true).build()
    val opt5 = CommandLineOption.builder.longOption("o5").build()
    val opts = CommandLineOptions.builder
      .add(opt1)
      .add(opt2)
      .add(opt3)
      .add(opt4)
      .add(opt5)
      .build()

    val parsed = ParsedOptions(args, opts)
    parsed.stringArgsOf(opt1) should contain theSameElementsInOrderAs Seq("a1", "a2")
    assert(parsed.exists(opt2))
    assert(parsed.stringArgOf(opt3) === "a")
    parsed.stringArgsOf(opt4) shouldBe empty
    parsed.passThroughArgs should contain theSameElementsInOrderAs Seq("pass", "through")
  }

  test("checking whether a boolean switch is specified in args") {
    val args = Array("--bool", "--", "pass", "through")
    val switch = CommandLineOption.builder.longOption("bool").argumentRange(ZeroArgument).build()
    val opts = CommandLineOptions.builder.add(switch).build()

    val parsed = ParsedOptions(args, opts)
    assert(parsed.exists(switch))
  }

  test("parsing single string argument") {
    val args = Array("-s1", "arg11", "arg12", "-s2", "arg21")

    val s1 = CommandLineOption.builder.shortOption("s1").argumentRange(ArgumentRange(1, 10)).build()
    val s2 = CommandLineOption.builder.shortOption("s2").argumentRange(1).build()
    val opts = CommandLineOptions.builder.add(s1).add(s2).build()

    val parsed = ParsedOptions(args, opts)

    intercept[UnsupportedOperationException] {
      parsed.stringArgOf(s1)
    }
    assert(parsed.stringArgOfOrNone(s1).isEmpty)

    assert(parsed.stringArgOf(s2) === "arg21")
    assertOption("arg21", parsed.stringArgOfOrNone(s2))
  }

  test("parsing multiple string arguments") {
    val args = Array("-s1", "arg11", "arg12", "-s2", "arg21")

    val s1 = CommandLineOption.builder.shortOption("s1").argumentRange(ArgumentRange(1, 10)).build()
    val s2 = CommandLineOption.builder.shortOption("s2").argumentRange(1).build()
    val opts = CommandLineOptions.builder.add(s1).add(s2).build()

    val parsed = ParsedOptions(args, opts)

    parsed.stringArgsOf(s1) should contain theSameElementsAs Seq("arg11", "arg12")
    parsed.stringArgsOfOrNone(s1).getOrFail() should contain theSameElementsAs Seq("arg11", "arg12")

    parsed.stringArgsOf(s2) should contain theSameElementsAs Seq("arg21")
    parsed.stringArgsOfOrNone(s2).getOrFail() should contain theSameElementsAs Seq("arg21")
  }

  test("parsing single integer argument") {
    val args = Array("-n", "666")

    val opt = CommandLineOption.builder.shortOption("n").argumentRange(1).build()
    val opts = CommandLineOptions.builder.add(opt).build()

    val parsed = ParsedOptions(args, opts)
    assert(parsed.intArgOf(opt) === 666)
    assertOption(666, parsed.intArgOfOrNone(opt))
    assert(parsed.longArgOf(opt) === 666L)
    assertOption(666L, parsed.longArgOfOrNone(opt))
  }

  test("parsing typed argument") {
    val args = Array("-b", "false")

    val opt = CommandLineOption.builder.shortOption("b").argumentRange(1).build()
    val opts = CommandLineOptions.builder.add(opt).build()

    val parsed = ParsedOptions(args, opts)
    val convert = (b: String) => b.toBoolean
    assert(parsed.typedArgOf(opt, convert) === false)
    assertOption(false, parsed.typedArgOfOrNone(opt, convert))
  }

  test("applying same conversion to all arguments") {
    val args = Array("-bs", "false", "true", "false", "true")

    val opt = CommandLineOption.builder.shortOption("bs").argumentRange(1, 4).build()
    val opts = CommandLineOptions.builder.add(opt).build()

    val parsed = ParsedOptions(args, opts)
    val convert = (b: String) => b.toBoolean
    val expected = Seq(false, true, false, true)
    parsed.typedArgsOf(opt, convert) should contain theSameElementsInOrderAs expected
    parsed.typedArgsOfOrNone(opt, convert).getOrFail() should contain theSameElementsInOrderAs expected
  }

  test("supplementing arguments with system property") {
    withSysProps(
      SystemProperty.ofInt("chrism.arg.num", 3),
      SystemProperty("chrism.arg.str", "s"),
      SystemProperty("chrism.arg.seq", "1 2 3"),
    ) {
      val args = "-s1 arg11 arg12 -s2 arg21".split(" ")

      val s1 = CommandLineOption.builder.shortOption("s1").argumentRange(ArgumentRange(1, 10)).build()
      val s2 = CommandLineOption.builder.shortOption("s2").argumentRange(1).build()
      val num = CommandLineOption.builder
        .longOption("num")
        .required(true)
        .argumentRange(1)
        .systemPropertyKey(SystemPropertyKey("chrism.arg.num"))
        .build()
      val str = CommandLineOption.builder
        .longOption("str")
        .required(true)
        .argumentRange(1)
        .systemPropertyKey(SystemPropertyKey("chrism.arg.str"))
        .build()
      val seq = CommandLineOption.builder
        .longOption("seq")
        .required(true)
        .argumentRange(1, 5)
        .systemPropertyKey(DelimitedSystemPropertyKey("chrism.arg.seq"))
        .build()
      val missing = CommandLineOption.builder
        .longOption("missing")
        .required(true)
        .argumentRange(1)
        .systemPropertyKey(DelimitedSystemPropertyKey("chrism.arg.missing"))
        .build()
      val opts = CommandLineOptions.builder.add(s1).add(s2).add(num).add(str).add(seq).add(missing).build()

      val parsed = ParsedOptions(args, opts)
      parsed.stringArgsOf(s1) should contain theSameElementsInOrderAs Seq("arg11", "arg12")
      assert(parsed.stringArgOf(s2) === "arg21")
      assert(parsed.intArgOf(num) === 3)
      assert(parsed.stringArgOf(str) === "s")
      parsed.typedArgsOf(seq, _.toInt) should contain theSameElementsInOrderAs Seq(1, 2, 3)

      intercept[MissingRequiredOptionException] {
        parsed.stringArgOf(missing)
      }
    }
  }

  test("rebuilding argument array by incorporating commandline arguments and system properties") {
    withSysProps(
      SystemProperty.ofInt("chrism.arg.single", 666),
      SystemProperty("chrism.arg.multiple", "1 2 3"),
    ) {
      val args = Array(
        "--cli1",
        "arg1",
        "-c2",
        "arg2",
        "arg3",
        "arg4",
        "--",
        "arg5",
        "arg6",
        "--cli1",
        "--single",
        "--multiple",
      )

      val single = CommandLineOption.builder
        .shortOption("s")
        .longOption("single")
        .required(true)
        .argumentRange(1)
        .systemPropertyKey(SystemPropertyKey("chrism.arg.single"))
        .build()
      val multiple = CommandLineOption.builder
        .shortOption("m")
        .longOption("multiple")
        .required(true)
        .argumentRange(1, 4)
        .systemPropertyKey(DelimitedSystemPropertyKey("chrism.arg.multiple"))
        .build()
      val cli1 = CommandLineOption.builder
        .longOption("cli1")
        .required(true)
        .argumentRange(1)
        .build()
      val c2 = CommandLineOption.builder
        .shortOption("c2")
        .required(true)
        .argumentRange(3)
        .build()

      val opts = CommandLineOptions.builder.add(single).add(multiple).add(cli1).add(c2).build()
      val parsed = ParsedOptions(args, opts)
      parsed.rebuildArgs() should contain theSameElementsInOrderAs Seq(
        "--cli1",
        "arg1",
        "--multiple",
        "1",
        "2",
        "3",
        "--single",
        "666",
        "-c2",
        "arg2",
        "arg3",
        "arg4",
        "--",
        "arg5",
        "arg6",
        "--cli1",
        "--single",
        "--multiple",
      )
    }
  }

  test("copy with compatible CommandLineOptions instance") {
    val optionsBuilder = CommandLineOptions.builder
      .add(CommandLineOption.builder.longOption("o1").argumentRange(1, 2).build())
      .add(CommandLineOption.builder.shortOption("o2").build())
      .add(CommandLineOption.builder.shortOption("o3").argumentRange(1).required(true).build())
    // create an instance with the options added so far
    val options = optionsBuilder.build()

    val parsedOpts = ParsedOptions(
      Array("--o1", "a1", "a2", "-o2", "-o3", "a", "--o4", "--", "pass", "through"),
      optionsBuilder
        .add(CommandLineOption.builder.longOption("o4").argumentRange(0, 4).required(true).build())
        .add(CommandLineOption.builder.longOption("o5").build())
        .build()
    )

    val copied =
      parsedOpts.copy(args = parsedOpts.rebuildArgs((o: CommandLineOption) => !options.contains(o)), options = options)
    copied.rebuildArgs() should contain theSameElementsInOrderAs Seq(
      "--o1",
      "a1",
      "a2",
      "-o2",
      "-o3",
      "a",
      "--",
      "pass",
      "through",
    )
  }

  test("equals/hashCode") {
    val parsedOpts1 = ParsedOptions(
      Array("--o1", "a1", "a2", "-o2", "-o3", "a", "--o4", "--", "pass", "through"),
      CommandLineOptions.builder
        .add(CommandLineOption.builder.longOption("o1").argumentRange(1, 2).build())
        .add(CommandLineOption.builder.shortOption("o2").build())
        .add(CommandLineOption.builder.shortOption("o3").argumentRange(1).required(true).build())
        .add(CommandLineOption.builder.longOption("o4").argumentRange(0, 4).required(true).build())
        .add(CommandLineOption.builder.longOption("o5").build())
        .build()
    )

    val parsedOpts2 = ParsedOptions(
      Array("--o1", "a1", "a2", "-o2", "-o3", "a", "--o4", "--", "pass", "through"),
      CommandLineOptions.builder
        .add(CommandLineOption.builder.longOption("o4").argumentRange(0, 4).required(true).build())
        .add(CommandLineOption.builder.shortOption("o2").build())
        .add(CommandLineOption.builder.shortOption("o3").argumentRange(1).required(true).build())
        .add(CommandLineOption.builder.longOption("o5").build())
        .add(CommandLineOption.builder.longOption("o1").argumentRange(1, 2).build())
        .build()
    )

    // mainly to ensure that equals/hashCode work with arrays
    assert(parsedOpts1 === parsedOpts2)
    assert(parsedOpts1.hashCode() === parsedOpts2.hashCode())
  }
}
