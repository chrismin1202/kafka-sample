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
package com.chrism.shell

import java.util.NoSuchElementException

import com.chrism.config.{SysProps, SystemProperty}
import org.apache.commons.lang3.StringUtils

import scala.util.Try

sealed trait EnvironmentKey extends Product with Serializable {

  require(names.nonEmpty, "There must be at least 1 name!")

  protected def names: Seq[String]

  def exists(/* IO */ ): Boolean = rawValueOrNone().isDefined

  def rawValueOrNone(/* IO */ ): Option[String]

  final def rawValue(): String = rawValueOrNone().getOrElse(throw new NoSuchKeyException(names.head, names.tail: _*))

  final def typedValue[T](convert: String => T): T =
    rawValueOrNone()
      .map(convert)
      .getOrElse(throw new NoSuchKeyException(names.head, names.tail: _*))

  final def typedValueOrNone[T](convert: String => T): Option[T] = rawValueOrNone().map(convert)

  final def optionalTypedValueOrNone[T](convert: String => Option[T]): Option[T] = rawValueOrNone().flatMap(convert)

  final def booleanValue(convert: String => Boolean = _.toBoolean): Boolean =
    rawValueOrNone()
      .map(convert)
      .getOrElse(throw new NoSuchKeyException(names.head, names.tail: _*))

  final def booleanValueOrNone(convert: String => Boolean = _.toBoolean): Option[Boolean] =
    rawValueOrNone().map(convert)

  final def optionalBooleanValueOrNone(
    convert: String => Option[Boolean] = EnvironmentKey.tryConverting(_, _.toBoolean)
  ): Option[Boolean] =
    rawValueOrNone().flatMap(convert)

  final def intValue(convert: String => Int = _.toInt): Int =
    rawValueOrNone()
      .map(convert)
      .getOrElse(throw new NoSuchKeyException(names.head, names.tail: _*))

  final def intValueOrNone(convert: String => Int = _.toInt): Option[Int] = rawValueOrNone().map(convert)

  final def optionalIntValueOrNone(
    convert: String => Option[Int] = EnvironmentKey.tryConverting(_, _.toInt)
  ): Option[Int] =
    rawValueOrNone().flatMap(convert)

  final def longValue(convert: String => Long = _.toLong): Long =
    rawValueOrNone()
      .map(convert)
      .getOrElse(throw new NoSuchKeyException(names.head, names.tail: _*))

  final def longValueOrNone(convert: String => Long = _.toLong): Option[Long] = rawValueOrNone().map(convert)

  final def optionalLongValueOrNone(
    convert: String => Option[Long] = EnvironmentKey.tryConverting(_, _.toLong)
  ): Option[Long] =
    rawValueOrNone().flatMap(convert)

  final def floatValue(convert: String => Float = _.toFloat): Float =
    rawValueOrNone()
      .map(convert)
      .getOrElse(throw new NoSuchKeyException(names.head, names.tail: _*))

  final def floatValueOrNone(convert: String => Float = _.toFloat): Option[Float] = rawValueOrNone().map(convert)

  final def optionalFloatValueOrNone(
    convert: String => Option[Float] = EnvironmentKey.tryConverting(_, _.toFloat)
  ): Option[Float] =
    rawValueOrNone().flatMap(convert)

  final def doubleValue(convert: String => Double = _.toDouble): Double =
    rawValueOrNone()
      .map(convert)
      .getOrElse(throw new NoSuchKeyException(names.head, names.tail: _*))

  final def doubleValueOrNone(convert: String => Double = _.toDouble): Option[Double] = rawValueOrNone().map(convert)

  final def optionalDoubleValueOrNone(
    convert: String => Option[Double] = EnvironmentKey.tryConverting(_, _.toDouble)
  ): Option[Double] =
    rawValueOrNone().flatMap(convert)
}

private[this] object EnvironmentKey {

  private def tryConverting[T](s: String, convert: String => T): Option[T] =
    Try(convert(s)).map(Some(_)).getOrElse(None)
}

sealed abstract class SingleEnvironmentKey(name: String) extends EnvironmentKey {

  require(StringUtils.isNotBlank(name), "The key cannot be null or blank!")

  override protected final lazy val names: Seq[String] = Seq(name)
}

sealed trait Delimited {
  this: SingleEnvironmentKey =>

  def delimiter: String

  final def rawValues(): Seq[String] = rawValueOrNone().map(_.split(delimiter).toSeq).getOrElse(Seq.empty)

  final def typedValues[T](convert: String => T): Seq[T] = rawValues().map(convert)

  final def optionalTypedValues[T](convert: String => Option[T]): Seq[T] = rawValues().flatMap(convert(_))
}

sealed abstract class EnvironmentVariableKey(val name: String) extends SingleEnvironmentKey(name) {

  override final def rawValueOrNone(): Option[String] = sys.env.get(name)

  override final def toString: String = name
}

object EnvironmentVariableKey {

  def apply(name: String): EnvironmentVariableKey = Impl(name)

  private[this] final case class Impl(override val name: String) extends EnvironmentVariableKey(name)
}

sealed abstract class SystemPropertyKey(val name: String) extends SingleEnvironmentKey(name) {

  override final def rawValueOrNone(): Option[String] = SysProps.getOrNone(name)

  override final def toString: String = s"-D$name"
}

object SystemPropertyKey {

  def apply(name: String): SystemPropertyKey = Impl(name)

  private[this] final case class Impl(override val name: String) extends SystemPropertyKey(name)
}

final case class DelimitedEnvironmentVariableKey(
  override val name: String,
  delimiter: String = DelimitedEnvironmentVariableKey.DefaultValueDelimiter)
    extends EnvironmentVariableKey(name)
    with Delimited

object DelimitedEnvironmentVariableKey {

  val DefaultValueDelimiter: String = " "
}

final case class DelimitedSystemPropertyKey(
  override val name: String,
  delimiter: String = SystemProperty.DefaultValueDelimiter)
    extends SystemPropertyKey(name)
    with Delimited

/** By default, [[EnvironmentVariableKey]] takes precedence over [[SystemPropertyKey]], that is,
  * when both keys are specified, the value associated with [[EnvironmentVariableKey]] is used.
  *
  * @param envVar     the [[EnvironmentVariableKey]]
  * @param sysProp    the [[SystemPropertyKey]]
  * @param precedence the [[CombinedEnvironmentKeyPrecedence]] (default: [[EnvVarFirst]])
  */
final case class CombinedEnvironmentKey(
  envVar: EnvironmentVariableKey,
  sysProp: SystemPropertyKey,
  precedence: CombinedEnvironmentKeyPrecedence = EnvVarFirst)
    extends EnvironmentKey {

  override protected lazy val names: Seq[String] = Seq(envVar.name, sysProp.name)

  override def rawValueOrNone(): Option[String] =
    precedence match {
      case EnvVarFirst  => envVar.rawValueOrNone().orElse(sysProp.rawValueOrNone())
      case SysPropFirst => sysProp.rawValueOrNone().orElse(envVar.rawValueOrNone())
      case other        => throw new UnsupportedOperationException(s"$other is not a supported precedence type!")
    }

  def rawValuesOrNone(): Option[Seq[String]] =
    precedence match {
      case EnvVarFirst  => rawValuesOrNoneFromEnvVar().orElse(rawValuesOrNoneFromSysProp())
      case SysPropFirst => rawValuesOrNoneFromSysProp().orElse(rawValuesOrNoneFromEnvVar())
      case other        => throw new UnsupportedOperationException(s"$other is not a supported precedence type!")
    }

  def rawValues(): Seq[String] =
    precedence match {
      case EnvVarFirst =>
        val values = rawValuesFromEnvVar()
        if (values.isEmpty) rawValuesFromSysProp() else values
      case SysPropFirst =>
        val values = rawValuesFromSysProp()
        if (values.isEmpty) rawValuesFromEnvVar() else values
      case other => throw new UnsupportedOperationException(s"$other is not a supported precedence type!")
    }

  def typedValues[T](convert: String => T): Seq[T] = rawValues().map(convert)

  def optionalTypedValues[T](convert: String => Option[T]): Seq[T] = rawValues().flatMap(convert(_))

  private[this] def rawValuesOrNoneFromEnvVar(): Option[Seq[String]] =
    envVar match {
      case delimited: DelimitedEnvironmentVariableKey =>
        val values = delimited.rawValues()
        if (values.isEmpty) None else Some(values)
      case notDelimited => Some(Seq(notDelimited.rawValue()))
    }

  private[this] def rawValuesOrNoneFromSysProp(): Option[Seq[String]] =
    sysProp match {
      case delimited: DelimitedSystemPropertyKey =>
        val values = delimited.rawValues()
        if (values.isEmpty) None else Some(values)
      case notDelimited => Some(Seq(notDelimited.rawValue()))
    }

  private[this] def rawValuesFromEnvVar(): Seq[String] =
    envVar match {
      case delimited: DelimitedEnvironmentVariableKey => delimited.rawValues()
      case notDelimited                               => Seq(notDelimited.rawValue())
    }

  private[this] def rawValuesFromSysProp(): Seq[String] =
    sysProp match {
      case delimited: DelimitedSystemPropertyKey => delimited.rawValues()
      case notDelimited                          => Seq(notDelimited.rawValue())
    }
}

object CombinedEnvironmentKey {

  def of(
    envVarName: String,
    sysPropName: String,
    precedence: CombinedEnvironmentKeyPrecedence = EnvVarFirst
  ): CombinedEnvironmentKey =
    CombinedEnvironmentKey(EnvironmentVariableKey(envVarName), SystemPropertyKey(sysPropName), precedence = precedence)
}

sealed trait CombinedEnvironmentKeyPrecedence extends Product with Serializable

case object EnvVarFirst extends CombinedEnvironmentKeyPrecedence

case object SysPropFirst extends CombinedEnvironmentKeyPrecedence

trait EnvironmentKeyed[V] {

  protected def convert(raw: String): V

  protected def stringify(v: V): String

  final def fromEnv(key: EnvironmentKey): V = key.typedValue(convert)

  final def fromEnvOrNone(key: EnvironmentKey): Option[V] = key.typedValueOrNone(convert)
}

trait EnvironmentVariableKeyed[V] extends EnvironmentKeyed[V] {

  def defaultEnvVarName: String

  final lazy val defaultEnvVarKey: EnvironmentVariableKey = EnvironmentVariableKey(defaultEnvVarName)

  final def fromEnvByVarName(envVarName: String = defaultEnvVarName): V =
    fromEnv(EnvironmentVariableKey(envVarName))

  final def fromEnvByVarNameOrNone(envVarName: String = defaultEnvVarName): Option[V] =
    fromEnvOrNone(EnvironmentVariableKey(envVarName))
}

trait SystemPropertyKeyed[V] extends EnvironmentKeyed[V] {

  def defaultSysPropName: String

  final lazy val defaultSysPropKey: SystemPropertyKey = SystemPropertyKey(defaultSysPropName)

  final def fromEnvByPropName(propName: String = defaultSysPropName): V =
    fromEnv(SystemPropertyKey(propName))

  final def fromEnvByPropNameOrNone(propName: String = defaultSysPropName): Option[V] =
    fromEnvOrNone(SystemPropertyKey(propName))

  final def toSystemProperty(v: V, propName: String = defaultSysPropName): SystemProperty =
    SystemProperty(propName, v)(stringify = stringify)

  final def toSystemProperty(k: SystemPropertyKey, v: V): SystemProperty =
    SystemProperty(k.name, v)(stringify = stringify)
}

trait CombinedEnvironmentKeyed[V] extends EnvironmentVariableKeyed[V] with SystemPropertyKeyed[V] {

  def precedence: CombinedEnvironmentKeyPrecedence = EnvVarFirst

  final lazy val defaultCombinedKey: CombinedEnvironmentKey =
    CombinedEnvironmentKey.of(defaultEnvVarName, defaultSysPropName, precedence = precedence)
}

final class NoSuchKeyException private[shell] (name: String, alternateNames: String*)
    extends NoSuchElementException(s"There is no key by the name ${(name +: alternateNames).mkString("|")}!")
