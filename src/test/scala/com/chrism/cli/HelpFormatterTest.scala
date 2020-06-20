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

final class HelpFormatterTest extends FunTestSuite {

  ignore("printing help") {
    // un-comment when debugging
    val opts = CommandLineOptions.builder
      .appName("Some Application")
      .usage("java fully.qualified.path.to.your.Class [COMMAND GOES HERE]")
      .add(
        CommandLineOption.builder
          .shortOption("s1")
          .longOption("long-option1")
          .required(true)
          .argumentRange(0, 4)
          .build())
      .add(
        CommandLineOption.builder
          .longOption("long-option2")
          .argumentRange(5)
          .description("This is a long option.")
          .build())
      .add(CommandLineOption.builder
        .shortOption("s3")
        .description("This is a short option.")
        .build())
      .build()
    val formatter = HelpFormatter(opts)
    formatter.printHelp()
  }
}
