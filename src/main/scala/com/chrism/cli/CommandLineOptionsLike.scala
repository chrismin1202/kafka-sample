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

/** The mixin trait for aggregating [[CommandLineOptions]]
  *
  * The implementer must override {{{ optionSet }}} and
  * add all options the sub-trait/class has in the following manner:
  * {{{
  *   override protected def optionSet: Set[CommandLineOption] = super.optionSet +
  *     CommandLineOption.builder.longOption("option1").build() +
  *     CommandLineOption.builder.longOption("option2").build()
  * }}}
  * Note that if the overriding trait/class is intended for further extension, it is crucial that {{{ optionSet }}} is
  * NOT overridden as {{{ final }}}.
  *
  * When mixing in a sub-trait of this trait, there can be occasions where one or more of the [[CommandLineOptions]]
  * instances added by the sub-trait do not need to be inherited.
  * If so, one can override {{{ exclusions }}} to specify exclusion rules (functions).
  * {{{
  *   trait OptionSet1 extends CommandLineOptionsLike {
  *
  *     override protected def optionSet: Set[CommandLineOption] = super.optionSet +
  *       CommandLineOption.builder.shortOption("o1").build() +
  *       CommandLineOption.builder.longOption("option2").build()
  *   }
  *
  *   trait OptionSet2 extends CommandLineOptionsLike {
  *
  *     override protected def optionSet: Set[CommandLineOption] = super.optionSet +
  *       CommandLineOption.builder.shortOption("o3").build() +
  *       CommandLineOption.builder.longOption("option4").build()
  *   }
  *
  *   object AllOptions extends CommandLineOptionsLike with OptionSet1 with OptionSet2 {
  *
  *     override protected def optionSet: Set[CommandLineOption] = super.optionSet +
  *       CommandLineOption.builder.shortOption("o5").build()
  *
  *     override protected def exclusions: Set[CommandLineOption => Boolean] = super.exclusions +
  *       ((o: CommandLineOption) => o.longOpt.contains("option2"))
  *   }
  * }}}
  */
trait CommandLineOptionsLike {

  /** Override to build [[ArgumentBuilder]] instance.
    * Note that this method should NOT be overridden as {{{ final }}} in mix-ins.
    * Also note that it is okay to return a new instance of [[ArgumentBuilder]]
    * as long as all arguments that have been added by other mix-ins are retained.
    *
    * @param parsedOptions an instance of [[ParsedOptions]]
    * @return an instance of [[ArgumentBuilder]] with relevant options added
    */
  protected def toArgumentBuilder(parsedOptions: ParsedOptions): ArgumentBuilder = ArgumentBuilder()

  /** Override to aggregate [[CommandLineOption]] instances
    *
    * @return the set of [[CommandLineOption]] instances
    */
  protected def optionSet: Set[CommandLineOption] = Set.empty

  /** Override to specify the exclusion rules
    *
    * @return the exclusion rules to apply
    */
  protected def exclusions: Set[CommandLineOption => Boolean] = Set.empty

  protected def appName: Option[String] = None

  protected def usage: Option[String] = None

  final def options: Set[CommandLineOption] = optionSet.filterNot(o => exclusions.exists(f => f(o)))

  final def contains(o: CommandLineOption): Boolean = options.contains(o)

  final def commandLineOptions: CommandLineOptions = {
    val builder = CommandLineOptions.builder
    options.foreach(builder.add)
    appName.foreach(builder.appName)
    usage.foreach(builder.usage)
    builder.build()
  }

  protected implicit final class BuilderOps(builder: ArgumentBuilder) {

    def addIfExists[A](o: Option[A])(f: (ArgumentBuilder, A) => ArgumentBuilder): ArgumentBuilder =
      o.map(add(_)(f)).getOrElse(builder)

    def add[A](a: A)(f: (ArgumentBuilder, A) => ArgumentBuilder): ArgumentBuilder = f(builder, a)
  }
}
