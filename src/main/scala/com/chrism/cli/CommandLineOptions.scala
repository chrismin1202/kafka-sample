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

import com.chrism.commons.util.{ProductUtils, StringUtils}

import scala.collection.mutable

final class CommandLineOptions private (
  val opts: Set[CommandLineOption],
  private[cli] val appName: Option[String],
  private[cli] val usage: Option[String])
    extends Product
    with Serializable {

  def apply(shortOrLongName: String): CommandLineOption =
    get(shortOrLongName)
      .getOrElse(throw new NoSuchElementException(s"There is no option with the name $shortOrLongName"))

  def get(shortOrLongName: String): Option[CommandLineOption] =
    opts.find(o => o.shortOpt.contains(shortOrLongName) || o.longOpt.contains(shortOrLongName))

  def contains(opt: CommandLineOption): Boolean = opts.contains(opt)

  def requiredOptions: Set[CommandLineOption] = opts.filter(_.required)

  override def productElement(n: Int): Any =
    n match {
      case 0 => opts
      case 1 => appName
      case 2 => usage
      case i => throw new IndexOutOfBoundsException(s"There is no element at $i!")
    }

  override val productArity: Int = 3

  override def canEqual(that: Any): Boolean = that.isInstanceOf[CommandLineOptions]

  override def equals(that: Any): Boolean = ProductUtils.productEquals(this, that)

  override def hashCode(): Int = ProductUtils.productHashCode(this)

  override def toString: String = ProductUtils.productToString(this)
}

object CommandLineOptions {

  def builder: Builder = Builder()

  private def apply(
    opts: Iterable[CommandLineOption],
    appName: Option[String],
    usage: Option[String]
  ): CommandLineOptions =
    new CommandLineOptions(opts.toSet, appName, usage)

  final class Builder private () {

    private[this] var _appName: String = _
    private[this] var _usage: String = _
    private[this] val shortOpts: mutable.Set[String] = mutable.Set.empty
    private[this] val longOpts: mutable.Set[String] = mutable.Set.empty
    private[this] var opts: mutable.ListBuffer[CommandLineOption] = mutable.ListBuffer.empty

    def build(): CommandLineOptions = {
      if (!opts.contains(HelpOption)) {
        opts += HelpOption
      }
      CommandLineOptions(opts, StringUtils.someTrimmedIfNotBlank(_appName), StringUtils.someTrimmedIfNotBlank(_usage))
    }

    def appName(name: String): this.type = {
      _appName = name
      this
    }

    def usage(usage: String): this.type = {
      _usage = usage
      this
    }

    def add(opt: CommandLineOption): this.type = {
      require(
        !opt.shortOpt.exists(shortOpts.contains),
        s"There already is a short option with the name ${opt.shortOpt.get}")
      require(
        !opt.longOpt.exists(longOpts.contains),
        s"There already is a long option with the name ${opt.longOpt.get}")
      opt.shortOpt.foreach(shortOpts += _)
      opt.longOpt.foreach(longOpts += _)
      opts += opt
      this
    }

    def addAll(options: Iterable[CommandLineOption]): this.type = {
      options.foreach(add)
      this
    }

    def +=(opt: CommandLineOption): this.type = add(opt)

    def ++=(options: Iterable[CommandLineOption]): this.type = addAll(options)
  }

  private[this] object Builder {

    def apply(): Builder = new Builder()
  }
}
