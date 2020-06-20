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
import com.chrism.shell.{
  CombinedEnvironmentKeyPrecedence,
  DelimitedEnvironmentVariableKey,
  DelimitedSystemPropertyKey,
  EnvVarFirst,
  SysPropFirst
}

import scala.collection.mutable
import scala.util.Try

final class ParsedOptions private (
  val args: Seq[String],
  val options: CommandLineOptions,
  precedence: CombinedEnvironmentKeyPrecedence)
    extends Product
    with Serializable {

  import ParsedOptions._

  @transient
  private[this] lazy val commandLine: CommandLine = CommandLine(args, options)

  @transient
  private[this] lazy val sources: Seq[Source] =
    precedence match {
      case EnvVarFirst  => Seq(commandLine, EnvVar, SysProp)
      case SysPropFirst => Seq(commandLine, SysProp, EnvVar)
      case other        => throw new UnsupportedOperationException(s"$other is not a supported precedence type!")
    }

  @transient
  lazy val help: Boolean = args.exists(isHelp)

  def passThroughArgs: Array[String] = commandLine.passThroughArgs.toArray

  /** Looks for the given [[CommandLineOption]] instance in [[args]] and returns true if exists; otherwise,
    * false is returned.
    * Note that this method is intended for boolean switches,
    * i.e., [[CommandLineOption]] instance with [[ZeroArgument]] as [[ArgumentRange]],
    * but it is not limited to boolean switches.
    *
    * @param opt the [[CommandLineOption]] to find
    * @return true if the given option is specified in [[args]] else false
    */
  def exists(opt: CommandLineOption): Boolean = sources.exists(_.exists(opt))

  /** Extracts and returns the string argument associated with the given [[CommandLineOption]] instance.
    *
    * @param opt the [[CommandLineOption]] to find
    * @return the string argument associated with the given option
    * @throws MissingRequiredOptionException thrown if the given [[CommandLineOption]] is required but is not found
    * @throws UnsupportedOperationException  thrown if the [[ArgumentRange]] is not [[OneArgument]]
    * @throws MissingOptionException         thrown if the option is missing in [[args]]
    * @throws ArgumentParsingException       thrown if the [[ParsedArgument]] instance is not [[SingleArgument]]
    */
  def stringArgOf(opt: CommandLineOption): String = {
    ensureArgRange(opt.argRange, OneArgument)

    stringArgOfOrNone(opt)
      .getOrElse(throw new MissingOptionException(opt, args))
  }

  /** Extracts and returns the string argument associated with the given [[CommandLineOption]] instance if exists;
    * otherwise, [[None]] is returned.
    *
    * @param opt the [[CommandLineOption]] to find
    * @return the string argument associated with the given option or [[None]]
    * @throws MissingRequiredOptionException thrown if the given [[CommandLineOption]] is required but is not found
    */
  def stringArgOfOrNone(opt: CommandLineOption): Option[String] = {
    val arg = sources.view.flatMap(_.stringArgOfOrNone(opt)).headOption
    if (arg.isEmpty && opt.required) {
      throw new MissingRequiredOptionException(opt, args)
    }
    arg
  }

  /** Extracts and returns the string arguments associated with the given [[CommandLineOption]] instance.
    *
    * @param opt the [[CommandLineOption]] to find
    * @return the string arguments associated with the given option (non-empty)
    * @throws UnsupportedOperationException  thrown if the [[ParsedArgument]] instance is not [[SingleArgument]]
    *                                        or [[MultiArgument]]
    * @throws MissingRequiredOptionException thrown if the given [[CommandLineOption]] is required but is not found
    * @throws MissingOptionException         thrown if the option is missing in [[args]]
    * @throws ArgumentParsingException       thrown if the [[ParsedArgument]] instance is not [[SingleArgument]]
    *                                        or [[MultiArgument]]
    */
  def stringArgsOf(opt: CommandLineOption): Seq[String] = {
    if (!opt.hasArgs) {
      throw new UnsupportedOperationException(s"${opt.delimitedOptionNames} does have any argument!")
    }
    stringArgsOfOrNone(opt)
      .getOrElse(throw new MissingOptionException(opt, args))
  }

  /** Extracts and returns the string arguments associated with the given [[CommandLineOption]] instance if exists;
    * otherwise, [[None]] is returned.
    *
    * @param opt the [[CommandLineOption]] to find
    * @return the string arguments associated with the given option (non-empty) or [[None]]
    * @throws MissingRequiredOptionException thrown if the given [[CommandLineOption]] is required but is not found
    */
  def stringArgsOfOrNone(opt: CommandLineOption): Option[Seq[String]] = {
    val arg = sources.view.flatMap(_.stringArgsOfOrNone(opt)).headOption
    if (arg.isEmpty && opt.required) {
      throw new MissingRequiredOptionException(opt, args)
    }
    arg
  }

  /** Extracts and returns the [[Int]] argument associated with the given [[CommandLineOption]] instance.
    *
    * @param opt the [[CommandLineOption]] to find
    * @return the [[Int]] argument associated with the given option
    * @throws UnsupportedOperationException  thrown if the [[ArgumentRange]] is not [[OneArgument]]
    * @throws MissingRequiredOptionException thrown if the given [[CommandLineOption]] is required but is not found
    * @throws MissingOptionException         thrown if the option is missing in [[args]]
    * @throws ArgumentParsingException       thrown if the [[ParsedArgument]] instance is not [[SingleArgument]]
    * @throws NumberFormatException          thrown if the string does not contain parsable integer
    */
  def intArgOf(opt: CommandLineOption): Int = stringArgOf(opt).toInt

  /** Extracts and returns the [[Int]] argument associated with the given [[CommandLineOption]] instance if exists;
    * otherwise, [[None]] is returned.
    *
    * @param opt the [[CommandLineOption]] to find
    * @return the [[Int]] argument associated with the given option or [[None]]
    * @throws MissingRequiredOptionException thrown if the given [[CommandLineOption]] is required but is not found
    */
  def intArgOfOrNone(opt: CommandLineOption): Option[Int] =
    stringArgOfOrNone(opt)
      .flatMap(s => Try(s.toInt).map(Some(_)).getOrElse(None))

  /** Extracts and returns the [[Long]] argument associated with the given [[CommandLineOption]] instance.
    *
    * @param opt the [[CommandLineOption]] to find
    * @return the [[Long]] argument associated with the given option
    * @throws UnsupportedOperationException  thrown if the [[ArgumentRange]] is not [[OneArgument]]
    * @throws MissingRequiredOptionException thrown if the given [[CommandLineOption]] is required but is not found
    * @throws MissingOptionException         thrown if the option is missing in [[args]]
    * @throws ArgumentParsingException       thrown if the [[ParsedArgument]] instance is not [[SingleArgument]]
    * @throws NumberFormatException          thrown if the string does not contain parsable integer
    */
  def longArgOf(opt: CommandLineOption): Long = stringArgOf(opt).toLong

  /** Extracts and returns the [[Long]] argument associated with the given [[CommandLineOption]] instance if exists;
    * otherwise, [[None]] is returned.
    *
    * @param opt the [[CommandLineOption]] to find
    * @return the [[Long]] argument associated with the given option or [[None]]
    * @throws MissingRequiredOptionException thrown if the given [[CommandLineOption]] is required but is not found
    */
  def longArgOfOrNone(opt: CommandLineOption): Option[Long] =
    stringArgOfOrNone(opt)
      .flatMap(s => Try(s.toLong).map(Some(_)).getOrElse(None))

  /** Extracts and returns the argument associated with the given [[CommandLineOption]] instance
    * as an instance of [[T]].
    *
    * @param opt     the [[CommandLineOption]] to find
    * @param convert the function for converting raw string argument to [[T]]
    * @return the argument associated with the given option as an instance of [[T]]
    * @throws UnsupportedOperationException  thrown if the [[ArgumentRange]] is not [[OneArgument]]
    * @throws MissingRequiredOptionException thrown if the given [[CommandLineOption]] is required but is not found
    * @throws MissingOptionException         thrown if the option is missing in [[args]]
    * @throws ArgumentParsingException       thrown if the [[ParsedArgument]] instance is not [[SingleArgument]]
    */
  def typedArgOf[T](opt: CommandLineOption, convert: String => T): T = convert(stringArgOf(opt))

  /** Extracts and returns the argument associated with the given [[CommandLineOption]] instance
    * as an instance of [[T]] if exists; otherwise, [[None]] is returned.
    *
    * @param opt     the [[CommandLineOption]] to find
    * @param convert the function for converting raw string argument to [[T]]
    * @return the argument associated with the given option as an instance of [[T]] or [[None]]
    * @throws MissingRequiredOptionException thrown if the given [[CommandLineOption]] is required but is not found
    */
  def typedArgOfOrNone[T](opt: CommandLineOption, convert: String => T): Option[T] =
    stringArgOfOrNone(opt).map(convert)

  /** Extracts and returns the argument associated with the given [[CommandLineOption]] instance
    * as an instance of [[T]] if exists; otherwise, [[None]] is returned.
    *
    * @param opt     the [[CommandLineOption]] to find
    * @param convert the function for converting raw string argument to [[Option]] of [[T]]
    * @return the argument associated with the given option as an instance of [[T]] or [[None]]
    * @throws MissingRequiredOptionException thrown if the given [[CommandLineOption]] is required but is not found
    */
  def optionalTypedArgOfOrNone[T](opt: CommandLineOption, convert: String => Option[T]): Option[T] =
    stringArgOfOrNone(opt).flatMap(convert)

  /** Extracts and returns the arguments associated with the given [[CommandLineOption]] instance
    * as a sequence of [[T]].
    *
    * @param opt     the [[CommandLineOption]] to find
    * @param convert the function for converting raw string argument to [[T]]
    * @return the arguments associated with the given option as a sequence of [[T]] (non-empty)
    * @throws MissingRequiredOptionException thrown if the given [[CommandLineOption]] is required but is not found
    * @throws MissingOptionException         thrown if the option is missing in [[args]]
    * @throws ArgumentParsingException       thrown if the [[ParsedArgument]] instance is not [[SingleArgument]]
    */
  def typedArgsOf[T](opt: CommandLineOption, convert: String => T): Seq[T] = stringArgsOf(opt).map(convert)

  /** Extracts and returns the arguments associated with the given [[CommandLineOption]] instance
    * as a sequence of [[T]].
    *
    * @param opt     the [[CommandLineOption]] to find
    * @param convert the function for converting raw string argument to [[Option]] of [[T]]
    * @return the arguments associated with the given option as a sequence of [[T]] (non-empty)
    * @throws MissingRequiredOptionException thrown if the given [[CommandLineOption]] is required but is not found
    * @throws MissingOptionException         thrown if the option is missing in [[args]]
    * @throws ArgumentParsingException       thrown if the [[ParsedArgument]] instance is not [[SingleArgument]]
    */
  def optionalTypedArgsOf[T](opt: CommandLineOption, convert: String => Option[T]): Seq[T] =
    stringArgsOf(opt).flatMap(convert(_))

  /** Extracts and returns the arguments associated with the given [[CommandLineOption]] instance
    * as a sequence of [[T]] if exists; otherwise, [[None]] is returned.
    *
    * @param opt     the [[CommandLineOption]] to find
    * @param convert the function for converting raw string argument to [[T]]
    * @return the arguments associated with the given option as a sequence of [[T]] (non-empty) or [[None]]
    * @throws MissingRequiredOptionException thrown if the given [[CommandLineOption]] is required but is not found
    */
  def typedArgsOfOrNone[T](opt: CommandLineOption, convert: String => T): Option[Seq[T]] =
    stringArgsOfOrNone(opt).map(_.map(convert))

  /** Extracts and returns the arguments associated with the given [[CommandLineOption]] instance
    * as a sequence of [[T]] if exists; otherwise, [[None]] is returned.
    *
    * @param opt     the [[CommandLineOption]] to find
    * @param convert the function for converting raw string argument to [[Option]] of [[T]]
    * @return the arguments associated with the given option as a sequence of [[T]] (non-empty) or [[None]]
    * @throws MissingRequiredOptionException thrown if the given [[CommandLineOption]] is required but is not found
    */
  def optionalTypedArgsOfOrNone[T](opt: CommandLineOption, convert: String => Option[T]): Option[Seq[T]] =
    stringArgsOfOrNone(opt).map(_.flatMap(convert(_))).filter(_.nonEmpty)

  def printHelp(formatter: Option[HelpFormatter] = None): Unit =
    formatter.getOrElse(HelpFormatter(options)).printHelp()

  /** Rebuilds (or manipulates) the argument array by combining [[args]] and other options
    * specified via environment variable or system property.
    *
    * Note that this method uses long option names by default unless [[CommandLineOption]] is constructed with
    * short option only.
    * Also note that the arguments are sorted in the lexicographic order of the option names, i.e., all long options
    * and its arguments will precede short options and its arguments as long options are prefixed with double-hyphens.
    *
    * @param exclusions the exclusion rules if any
    * @return the raw argument array that can be passed in to another application that accepts the same set of arguments
    */
  def rebuildArgs(exclusions: ExclusionRule*): Array[String] = {
    val rebuiltArgs = options.opts
      .filter(o => o != HelpOption && !exclusions.exists(f => f(o)) && exists(o))
      .map(o => (o, stringArgsOfOrNone(o)))
      .map(t =>
        t._1.prefixedLongOpt
          .orElse(t._1.prefixedShortOpt)
          .getOrElse(throw new InvalidOptionException(s"${t._1} is missing both long and short options!")) +:
          t._2.getOrElse(Seq.empty))
      .toVector
      // sort to be deterministic
      .sortBy(_.head)
      .flatten

    val withPassThroughArgs: Seq[String] =
      if (commandLine.passThroughArgs.isEmpty) rebuiltArgs
      else (rebuiltArgs :+ Terminator) ++ commandLine.passThroughArgs

    withPassThroughArgs.toArray
  }

  def copy(
    args: Seq[String] = args,
    options: CommandLineOptions = options,
    precedence: CombinedEnvironmentKeyPrecedence = precedence
  ): ParsedOptions =
    new ParsedOptions(args, options, precedence)

  override def productElement(n: Int): Any =
    n match {
      case 0 => args
      case 1 => options
      case 2 => precedence
      case i => throw new IndexOutOfBoundsException(s"There is no element at $i")
    }

  override val productArity: Int = 3

  override def canEqual(that: Any): Boolean = that.isInstanceOf[ParsedOptions]

  override def equals(that: Any): Boolean = ProductUtils.productEquals(this, that)

  override def hashCode(): Int = ProductUtils.productHashCode(this)

  override def toString: String = ProductUtils.productToString(this)
}

object ParsedOptions {

  private type ExclusionRule = CommandLineOption => Boolean

  val Terminator: String = "--"
  private[this] val HelpShort: String = HelpOption.shortOpt.map(CommandLineOption.ShortOptionPrefix + _).get
  private[this] val HelpLong: String = HelpOption.longOpt.map(CommandLineOption.LongOptionPrefix + _).get

  def apply(
    args: Array[String],
    options: CommandLineOptions,
    precedence: CombinedEnvironmentKeyPrecedence = EnvVarFirst
  ): ParsedOptions =
    new ParsedOptions(
      Seq(args: _*), // making a defensive copy of args as Array is inherently mutable
      options,
      precedence)

  private def isHelp(token: String): Boolean = token == HelpShort || token == HelpLong

  private def mapToParsedArgument(
    args: Seq[String],
    opt: CommandLineOption,
    optArgs: scala.collection.Seq[String]
  ): ParsedArgument =
    optArgs match {
      case null =>
        if (opt.hasArgs) {
          throw new MissingRequiredArgumentException(opt, args)
        }
        Argumentless
      case empty if empty.isEmpty =>
        if (opt.argRange.min != 0) {
          throw new MissingRequiredArgumentException(opt, args, empty)
        }
        MultiArgument(Seq.empty)
      case nonEmpty =>
        opt.argRange match {
          case ZeroArgument =>
            throw new ArgumentParsingException(args, s"Expected no argument, but ${nonEmpty.size} argument(s) parsed!")
          case OneArgument =>
            if (nonEmpty.size != 1) {
              throw new ArgumentParsingException(args, s"Expected 1 argument, but ${nonEmpty.size} argument(s) parsed!")
            }
            SingleArgument(nonEmpty.head)
          case range =>
            if (!range.isWithinRange(nonEmpty.size)) {
              throw new ArgumentParsingException(
                args,
                s"Expected ${opt.argRange} argument(s), but ${nonEmpty.size} argument(s) parsed!")
            }
            MultiArgument(nonEmpty)
        }
    }

  private def ensureArgRange(range: ArgumentRange, expectedRange: ArgumentRange): Unit =
    if (range != expectedRange) {
      throw new UnsupportedOperationException(s"Expected the argument range of $expectedRange, but $range was found!")
    }

  private sealed trait Source extends Product with Serializable {

    /** Looks for the given [[CommandLineOption]] instance and returns true if exists; otherwise,
      * false is returned.
      * Note that this method is intended for boolean switches,
      * i.e., [[CommandLineOption]] instance with [[ZeroArgument]] as [[ArgumentRange]],
      * but it is not limited to boolean switches.
      *
      * @param opt the [[CommandLineOption]] to find
      * @return true if the given option is specified else false
      */
    def exists(opt: CommandLineOption): Boolean

    /** Extracts and returns the string argument associated with the given [[CommandLineOption]] instance if exists;
      * otherwise, [[None]] is returned.
      *
      * @param opt the [[CommandLineOption]] to find
      * @return the string argument associated with the given option or [[None]]
      */
    def stringArgOfOrNone(opt: CommandLineOption): Option[String]

    /** Extracts and returns the string arguments associated with the given [[CommandLineOption]] instance if exists;
      * otherwise, [[None]] is returned.
      *
      * @param opt the [[CommandLineOption]] to find
      * @return the string arguments associated with the given option (non-empty) or [[None]]
      */
    def stringArgsOfOrNone(opt: CommandLineOption): Option[Seq[String]]

    protected final def validateArgs(opt: CommandLineOption, args: Seq[String]): Boolean =
      opt.argRange.isWithinRange(args.size)
  }

  private final class CommandLine private (val args: Seq[String], val options: CommandLineOptions) extends Source {

    @transient
    private[this] var _parsedOptions: Map[CommandLineOption, ParsedArgument] = _
    private[this] var _passThroughArgs: Seq[String] = _

    @transient
    lazy val parsedOptions: Map[CommandLineOption, ParsedArgument] = {
      if (_parsedOptions == null) {
        parse()
      }
      _parsedOptions
    }

    @transient
    lazy val passThroughArgs: Seq[String] = {
      if (_passThroughArgs == null) {
        parse()
      }
      _passThroughArgs
    }

    override def exists(opt: CommandLineOption): Boolean = parsedOptions.exists(_._1 == opt)

    override def stringArgOfOrNone(opt: CommandLineOption): Option[String] =
      parsedOptions
        .get(opt)
        .collect({
          case single: SingleArgument => single.arg
        })

    override def stringArgsOfOrNone(opt: CommandLineOption): Option[Seq[String]] =
      parsedOptions
        .get(opt)
        .collect({
          case single: SingleArgument => Seq(single.arg)
          case multi: MultiArgument   => multi.args
        })
        .filter(validateArgs(opt, _))

    /** Lazily parses the arguments. */
    private[this] def parse(): Unit = {
      if (_parsedOptions == null && _passThroughArgs == null) {
        val argMap: mutable.Map[CommandLineOption, mutable.ArrayBuffer[String]] = mutable.Map.empty
        val argIterator = args.iterator
        var terminate = false
        var i = 0
        var currOpt: CommandLineOption = null
        var currArgs: mutable.ArrayBuffer[String] = null
        while (!terminate && argIterator.hasNext) {
          val token = argIterator.next()
          if (token == Terminator) {
            terminate = true
          } else {
            if (token.startsWith(CommandLineOption.LongOptionPrefix)) {
              if (currOpt != null) {
                argMap += currOpt -> currArgs
              }
              val longOptName = token.substring(CommandLineOption.LongOptionPrefix.length)
              currOpt = options(longOptName)
              currArgs =
                if (currOpt.hasArgs) new mutable.ArrayBuffer(currOpt.argRange.max min (args.size - i - 1))
                else null
            } else if (token.startsWith(CommandLineOption.ShortOptionPrefix)) {
              if (currOpt != null) {
                argMap += currOpt -> currArgs
              }
              // TODO: handle single-letter options
              val shortOptName = token.substring(CommandLineOption.ShortOptionPrefix.length)
              currOpt = options(shortOptName)
              currArgs =
                if (currOpt.hasArgs) new mutable.ArrayBuffer(currOpt.argRange.max min (args.size - i - 1))
                else null
            } else {
              // assume arguments
              if (currOpt == null) {
                throw new ArgumentParsingException(args, s"Encountered an unexpected token ($token)")
              }
              if (currArgs == null && currOpt.hasArgs) {
                throw new ArgumentParsingException(
                  args,
                  s"Expected argument(s) for $currOpt, but no argument container has not been initialized!")
              }
              currArgs += token
            }
          }
          i += 1
        }

        if (currOpt != null && !argMap.contains(currOpt)) {
          argMap += currOpt -> currArgs
        }

        _parsedOptions = argMap
          .map(t => (t._1, mapToParsedArgument(args, t._1, t._2)))
          .toMap

        _passThroughArgs = if (terminate) {
          if (i < args.size) args.slice(i, args.size)
          else Seq.empty
        } else {
          Seq.empty
        }
      }
      assert(_parsedOptions != null && _passThroughArgs != null, s"Failed to parse ${args.mkString(" ")}")
    }

    override def productElement(n: Int): Any =
      n match {
        case 0 => args
        case 1 => options
        case i => throw new IndexOutOfBoundsException(s"There is no element at $i")
      }

    override val productArity: Int = 2

    override def canEqual(that: Any): Boolean = that.isInstanceOf[CommandLine]

    override def equals(that: Any): Boolean = ProductUtils.productEquals(this, that)

    override def hashCode(): Int = ProductUtils.productHashCode(this)

    override def toString: String = ProductUtils.productToString(this)
  }

  private object CommandLine {

    def apply(args: Seq[String], options: CommandLineOptions): CommandLine = new CommandLine(args, options)
  }

  private case object EnvVar extends Source {

    override def exists(opt: CommandLineOption): Boolean = opt.envVarKey.exists(_.exists())

    override def stringArgOfOrNone(opt: CommandLineOption): Option[String] = opt.envVarKey.flatMap(_.rawValueOrNone())

    override def stringArgsOfOrNone(opt: CommandLineOption): Option[Seq[String]] =
      opt.envVarKey
        .flatMap({
          case envVar: DelimitedEnvironmentVariableKey => Some(envVar.rawValues())
          case other                                   => other.rawValueOrNone().map(Seq(_))
        })
        .filter(validateArgs(opt, _))
  }

  private case object SysProp extends Source {

    override def exists(opt: CommandLineOption): Boolean = opt.sysPropKey.exists(_.exists())

    override def stringArgOfOrNone(opt: CommandLineOption): Option[String] = opt.sysPropKey.flatMap(_.rawValueOrNone())

    override def stringArgsOfOrNone(opt: CommandLineOption): Option[Seq[String]] =
      opt.sysPropKey
        .flatMap({
          case sysProp: DelimitedSystemPropertyKey => Some(sysProp.rawValues())
          case other                               => other.rawValueOrNone().map(Seq(_))
        })
        .filter(validateArgs(opt, _))
  }

  private[this] final case class SourcedParsedArgument(source: Source, arg: ParsedArgument)

}
