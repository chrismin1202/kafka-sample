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

final class CommandLineOptionsLikeTest extends FunTestSuite {

  test("excluding options when stacking sub-traits") {
    val excludableOption = CommandLineOption.builder.longOption("c").build()

    trait A extends CommandLineOptionsLike {

      override protected def optionSet: Set[CommandLineOption] =
        super.optionSet +
          CommandLineOption.builder.longOption("a").build()
    }

    trait B extends CommandLineOptionsLike {

      override protected def optionSet: Set[CommandLineOption] =
        super.optionSet +
          CommandLineOption.builder.longOption("b").build()
    }

    trait C extends CommandLineOptionsLike {

      override protected def optionSet: Set[CommandLineOption] =
        super.optionSet +
          excludableOption +
          CommandLineOption.builder.longOption("d").build()
    }

    object AllOptions extends CommandLineOptionsLike with A with B with C {

      override protected def optionSet: Set[CommandLineOption] =
        super.optionSet +
          CommandLineOption.builder.longOption("e").build() +
          CommandLineOption.builder.longOption("f").build()

      override protected def exclusions: Set[CommandLineOption => Boolean] =
        super.exclusions +
          ((o: CommandLineOption) => o.longOpt.contains("c"))
    }

    val options = AllOptions.commandLineOptions.opts
    options should have size 6
    options.flatMap(_.longOpt) should contain theSameElementsAs Set("a", "b", "d", "e", "f", "help")
    assert(AllOptions.contains(excludableOption) === false)
  }
}
