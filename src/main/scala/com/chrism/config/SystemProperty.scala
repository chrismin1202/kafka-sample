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
package com.chrism.config

import com.chrism.shell.SystemPropertyKey

import scala.util.matching.Regex

final case class SystemProperty(name: String, value: String) extends Ordered[SystemProperty] {

  import SystemProperty.{DefaultValueDelimiter, Delimiter, Order}

  @transient
  lazy val key: SystemPropertyKey = SystemPropertyKey(name)

  def values(delimiter: String = DefaultValueDelimiter): Seq[String] = value.split(delimiter)

  /** Formats this instance as Java system property by prepending "-D" and
    * delimiting name and value with [[SystemProperty.Delimiter]].
    *
    * @return the formatted Java system property
    */
  def formatted: String = s"-D$name$Delimiter$value"

  override def compare(that: SystemProperty): Int = Order.compare(this, that)

  override def toString: String = formatted
}

object SystemProperty {

  /** The default delimiter for the value when the value is delimited. */
  val DefaultValueDelimiter: String = " "
  private val Delimiter: Char = '='
  private val AlternateDelimiter: Char = ':'

  private val Order: Ordering[SystemProperty] = Ordering.by((p: SystemProperty) => (p.name, p.value))

  private[this] val FullNameIdx: Int = 3
  private[this] val NameIdx: Int = 4
  private[this] val NameGroupIndices: Seq[Int] = Seq(FullNameIdx, NameIdx)

  private[this] val SystemPropRegex: Regex =
    s"^?[\\p{Space}]?((-D([a-zA-Z0-9_.]+))|([a-zA-Z0-9_.]+))[\\p{Space}]?[$Delimiter$AlternateDelimiter][\\p{Space}]?".r

  def apply[V](name: String, value: V)(stringify: V => String = (v: V) => v.toString): SystemProperty =
    SystemProperty(name, stringify(value))

  def ofBoolean(name: String, value: Boolean): SystemProperty = SystemProperty(name, value)()

  def ofInt(name: String, value: Int): SystemProperty = SystemProperty(name, value)()

  def ofLong(name: String, value: Long): SystemProperty = SystemProperty(name, value)()

  def ofFloat(name: String, value: Float): SystemProperty = SystemProperty(name, value)()

  def ofDouble(name: String, value: Double): SystemProperty = SystemProperty(name, value)()

  /** Parses one Java system property.
    *
    * @param rawProperty the raw Java system property to parse
    * @return the parsed [[SystemProperty]] or [[None]]
    */
  def parse(rawProperty: String): Option[SystemProperty] =
    SystemPropRegex
      .findFirstMatchIn(rawProperty)
      .map(ParsedName(_))
      .map(pn => toSystemProperty(rawProperty, pn.name, pn.end, rawProperty.length))

  /** Parses all Java system properties that are delimited by whitespaces.
    *
    * @param rawProperties the raw Java system properties to parse
    * @return the parsed sequence of [[SystemProperty]] in the order they appear
    */
  def parseAll(rawProperties: String): Seq[SystemProperty] =
    toSystemProperties(rawProperties, SystemPropRegex.findAllMatchIn(rawProperties).map(ParsedName(_)).toSeq)

  private[this] def toSystemProperties(rawProperties: String, parsedNames: Seq[ParsedName]): Seq[SystemProperty] =
    parsedNames.size match {
      case 0 => throw new IllegalArgumentException(s"Un-parsable: $rawProperties")
      case 1 =>
        val head = parsedNames.head
        Seq(toSystemProperty(rawProperties, head.name, head.end, rawProperties.length))
      case 2 =>
        val head = parsedNames.head
        val last = parsedNames.last
        Seq(
          SystemProperty(head.name, parseValue(rawProperties, head.end, last.start)),
          SystemProperty(last.name, parseValue(rawProperties, last.end, rawProperties.length)),
        )
      case _ =>
        val properties = parsedNames
          .sliding(2)
          .map { t =>
            val head = t.head
            val last = t.last
            SystemProperty(head.name, parseValue(rawProperties, head.end, last.start))
          }
          .toVector // use Vector for appending
        val last = parsedNames.last
        val property = SystemProperty(last.name, parseValue(rawProperties, last.end, rawProperties.length))
        properties :+ property
    }

  private[this] def toSystemProperty(rawProperties: String, name: String, start: Int, end: Int): SystemProperty =
    SystemProperty(name, parseValue(rawProperties, start, end))

  private[this] def parseValue(rawProperties: String, start: Int, end: Int): String =
    rawProperties.substring(start, end).trim

  private[this] final case class ParsedName(name: String, start: Int, end: Int)

  private[this] object ParsedName {

    def apply(regexMatch: Regex.Match): ParsedName =
      NameGroupIndices
        .map(regexMatch.group)
        .find(_ != null)
        .map(ParsedName(_, regexMatch.start, regexMatch.end))
        .getOrElse(throw new IllegalArgumentException(
          s"The input (${regexMatch.source}) does not seem to contain property name!"))
  }
}
