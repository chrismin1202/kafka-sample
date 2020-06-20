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

import scala.collection.mutable

final class ArgumentBuilder private () {

  private val _args: mutable.Map[String, ParsedArgument] = mutable.Map.empty
  private var _passThroughArgs: Seq[String] = Seq.empty

  def build(): Array[String] = {
    val args = _args.toSeq.sortBy(_._1).flatMap(t => t._1 +: t._2.rawArgs)
    (if (_passThroughArgs.isEmpty) args else args ++ (ParsedOptions.Terminator +: _passThroughArgs)).toArray
  }

  /** Adds the given [[CommandLineOption]] with the argument(s).
    *
    * @param o    a [[CommandLineOption]] that takes in 0 or more argument
    * @param args the argument(s) associated with the given [[CommandLineOption]]
    * @throws IllegalArgumentException thrown if the given [[ParsedArgument]] contains less than the minimum
    *                                  or greater than the maximum number of arguments
    *                                  allowed by the given [[CommandLineOption]]
    *                                  or if the [[CommandLineOption]] instance is bogus
    */
  def add(o: CommandLineOption, args: ParsedArgument): this.type = {
    require(o.argRange.isWithinRange(args.rawArgs.size), s"The argument range of the option $o is ${o.argRange}!")

    val name = o.prefixedLongOpt
      .orElse(o.prefixedShortOpt)
      .getOrElse(throw new IllegalArgumentException(s"The option $o has neither long nor short option!"))
    _args += name -> args
    this
  }

  /** Adds the given [[CommandLineOption]] without any argument.
    *
    * @param o a [[CommandLineOption]] that takes in 0 or more argument
    * @throws IllegalArgumentException thrown if the given [[CommandLineOption]] requires argument(s)
    *                                  or if the [[CommandLineOption]] instance is bogus
    */
  def add(o: CommandLineOption): this.type = add(o, Argumentless)

  /** Adds all arguments from the given instance.
    * Note that if the other instance has an argument with the same name, it overwrites the one in this instance.
    * Also note that [[_passThroughArgs]] is overwritten only if that of the given instance is not empty.
    *
    * @param that another instance of [[ArgumentBuilder]] to aggregate
    */
  def addAll(that: ArgumentBuilder): this.type = {
    _args ++= that._args
    if (that._passThroughArgs.nonEmpty) {
      _passThroughArgs = _passThroughArgs
    }
    this
  }

  def passThroughArgs(args: Seq[String]): this.type = {
    _passThroughArgs = args
    this
  }

  /** Appends the pass-through arguments delimited by [[ParsedOptions.Terminator]].
    * Note that [[ParsedOptions.Terminator]] in the POSIX-compliant utility argument syntax
    * indicates the end of options.
    * Also note that this method makes a defensive copy of the given [[Array]] as arrays are mutable.
    *
    * @param args an [[Array]] of pass-through arguments to append
    */
  def passThroughArgs(args: Array[String]): this.type = passThroughArgs(Seq.empty ++ args)
}

object ArgumentBuilder {

  def apply(): ArgumentBuilder = new ArgumentBuilder()
}
