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

import com.chrism.commons.util.ProductUtils
import com.chrism.shell.{CombinedEnvironmentKey, EnvironmentVariableKey, SystemPropertyKey}

sealed abstract class CommandLineOption protected (
  val shortOpt: Option[String],
  val longOpt: Option[String],
  val required: Boolean,
  val argRange: ArgumentRange,
  val description: Option[String],
  val envVarKey: Option[EnvironmentVariableKey],
  val sysPropKey: Option[SystemPropertyKey])
    extends Product
    with Serializable {

  import CommandLineOption.{LongOptionPrefix, ShortOptionPrefix}

  @transient
  final lazy val prefixedShortOpt: Option[String] = shortOpt.map(ShortOptionPrefix + _)

  @transient
  final lazy val prefixedLongOpt: Option[String] = longOpt.map(LongOptionPrefix + _)

  @transient
  final lazy val delimitedOptionNames: String = CommandLineOption.delimitOptions(shortOpt, longOpt)

  @transient
  final lazy val delimitedPrefixedOptionNames: String =
    CommandLineOption.delimitOptions(prefixedShortOpt, prefixedLongOpt)

  final def hasArgs: Boolean = argRange.hasArgs

  def copy(
    shortOpt: Option[String] = shortOpt,
    longOpt: Option[String] = longOpt,
    required: Boolean = required,
    argRange: ArgumentRange = argRange,
    description: Option[String] = description,
    envVarKey: Option[EnvironmentVariableKey] = envVarKey,
    sysPropKey: Option[SystemPropertyKey] = sysPropKey
  ): CommandLineOption =
    CommandLineOption(shortOpt, longOpt, required, argRange, description, envVarKey, sysPropKey)

  override final def productElement(n: Int): Any =
    n match {
      case 0 => shortOpt
      case 1 => longOpt
      case 2 => required
      case 3 => argRange
      case 4 => description
      case 5 => envVarKey
      case 6 => sysPropKey
      case i => throw new IndexOutOfBoundsException(s"There is no element at $i")
    }

  override final val productArity: Int = 7

  override final def canEqual(that: Any): Boolean = that.isInstanceOf[CommandLineOption]

  override final def equals(that: Any): Boolean = ProductUtils.productEquals(this, that)

  override final def hashCode(): Int = ProductUtils.productHashCode(this)

  override final def toString: String = ProductUtils.productToString(this)
}

object CommandLineOption {

  val ShortOptionPrefix: String = "-"
  val LongOptionPrefix: String = "--"

  def builder: Builder = Builder()

  private def apply(
    shortOpt: Option[String],
    longOpt: Option[String],
    required: Boolean,
    argRange: ArgumentRange,
    description: Option[String],
    envVarKey: Option[EnvironmentVariableKey],
    sysPropKey: Option[SystemPropertyKey]
  ): CommandLineOption = {
    require(shortOpt != HelpOption.shortOpt, s"${HelpOption.shortOpt.get} is reserved short option")
    require(longOpt != HelpOption.longOpt, s"${HelpOption.longOpt.get} is reserved long option")

    Impl(shortOpt, longOpt, required, argRange, description, envVarKey, sysPropKey)
  }

  private def delimitOptions(shortOpt: Option[String], longOpt: Option[String]): String =
    (shortOpt, longOpt) match {
      case (Some(s), Some(l)) => s"$s|$l"
      case (Some(s), _)       => s
      case (_, Some(l))       => l
      case _ =>
        throw new IllegalStateException("This instance is somehow constructed with neither short name nor long name")
    }

  private[this] final case class Impl(
    override val shortOpt: Option[String],
    override val longOpt: Option[String],
    override val required: Boolean,
    override val argRange: ArgumentRange,
    override val description: Option[String],
    override val envVarKey: Option[EnvironmentVariableKey],
    override val sysPropKey: Option[SystemPropertyKey])
      extends CommandLineOption(shortOpt, longOpt, required, argRange, description, envVarKey, sysPropKey)

  final class Builder private () {

    private[this] var shortOpt: String = _
    private[this] var longOpt: String = _
    private[this] var _required: Boolean = false
    private[this] var _desc: String = _
    private[this] var argRange: ArgumentRange = ZeroArgument
    private[this] var envVarKey: EnvironmentVariableKey = _
    private[this] var sysPropKey: SystemPropertyKey = _

    def build(): CommandLineOption = {
      require(shortOpt != null || longOpt != null, "Either short or long option must be specified!")

      CommandLineOption(
        Option(shortOpt),
        Option(longOpt),
        _required,
        argRange,
        Option(_desc),
        Option(envVarKey),
        Option(sysPropKey))
    }

    def shortOption(opt: String): this.type = {
      OptionValidator.validateShortOption(opt)
      shortOpt = opt
      this
    }

    def longOption(opt: String): this.type = {
      OptionValidator.validateLongOption(opt)
      longOpt = opt
      this
    }

    def required(req: Boolean): this.type = {
      _required = req
      this
    }

    def description(desc: String): this.type = {
      _desc = desc
      this
    }

    def argumentRange(range: ArgumentRange): this.type = {
      argRange = range
      this
    }

    def argumentRange(min: Int, max: Int): this.type = argumentRange(ArgumentRange(min, max))

    def argumentRange(fixedRange: Int): this.type = argumentRange(FixedArgumentRange(fixedRange))

    def environmentVariableKey(key: EnvironmentVariableKey): this.type = {
      envVarKey = key
      this
    }

    def systemPropertyKey(key: SystemPropertyKey): this.type = {
      sysPropKey = key
      this
    }

    def combinedEnvironmentKey(key: CombinedEnvironmentKey): this.type = {
      envVarKey = key.envVar
      sysPropKey = key.sysProp
      this
    }
  }

  private[this] object Builder {

    def apply(): Builder = new Builder()
  }
}

case object HelpOption
    extends CommandLineOption(Some("h"), Some("help"), false, ZeroArgument, Some("Prints this help text"), None, None) {

  override def copy(
    shortOpt: Option[String] = shortOpt,
    longOpt: Option[String] = longOpt,
    required: Boolean = required,
    argRange: ArgumentRange = argRange,
    description: Option[String] = description,
    envVarKey: Option[EnvironmentVariableKey] = envVarKey,
    sysPropKey: Option[SystemPropertyKey] = sysPropKey
  ): CommandLineOption =
    throw new UnsupportedOperationException("Help option is not copyable!")
}
