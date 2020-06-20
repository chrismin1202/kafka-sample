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

trait CommandLineSupport {

  def commandLineOptions: CommandLineOptions

  /** Override if there is validation logic, e.g., checking illegal argument combination.
    * An appropriate exception should be thrown when there is an issue with options.
    *
    * @param parsedOptions an instance of [[ParsedOptions]] to validate
    * @return the validated instance of [[ParsedOptions]]
    */
  def validate(parsedOptions: ParsedOptions): ParsedOptions = parsedOptions

  final def parseCommandLineOptions(args: Array[String]): ParsedOptions =
    validate(ParsedOptions(args, commandLineOptions))

  final def printHelp(): Unit =
    parseCommandLineOptions(Array("--help")).printHelp()
}
