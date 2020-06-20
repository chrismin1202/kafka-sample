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

final class ArgumentParsingException private[cli] (args: scala.collection.Seq[String], msg: String)
    extends RuntimeException(s"""$msg
                                |Arguments: [${args.mkString(" ")}]""".stripMargin)

final class InvalidOptionException private[cli] (msg: String) extends IllegalArgumentException(msg)

final class MissingRequiredOptionException private[cli] (opt: CommandLineOption, args: scala.collection.Seq[String])
    extends RuntimeException(
      s"The argument [${args.mkString(" ")}] is missing the required option ${opt.delimitedOptionNames}")

final class MissingRequiredArgumentException private[cli] (
  opt: CommandLineOption,
  args: scala.collection.Seq[String],
  parsedArgs: scala.collection.Seq[String] = Seq.empty)
    extends RuntimeException(
      "The option " + opt.delimitedOptionNames + " requires " + opt.argRange + " argument(s), " +
        "but the argument [" + args.mkString(" ") + "] had " +
        MissingRequiredArgumentException.sizeOf(parsedArgs) + " argument(s)")

private[this] object MissingRequiredArgumentException {

  private def sizeOf(parsedArgs: scala.collection.Seq[String]): Int =
    if (parsedArgs == null || parsedArgs.isEmpty) 0
    else parsedArgs.size
}

final class MissingOptionException(opt: CommandLineOption, args: scala.collection.Seq[String])
    extends NoSuchElementException(s"The option ${opt.delimitedOptionNames} is missing in [${args.mkString(" ")}]")

final class IncompatibleOptionsException(o1: CommandLineOption, o2: CommandLineOption, moreOptions: CommandLineOption*)
    extends IllegalArgumentException(
      "The options " +
        (o1 +: o2 +: moreOptions).map(_.delimitedOptionNames).mkString(", ") +
        " are not compatible with one another!")
