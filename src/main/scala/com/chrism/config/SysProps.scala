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

import com.chrism.log.LoggingLike

import scala.util.matching.Regex

/** A wrapper for [[sys.props]], which in turn wraps [[java.util.Properties]] */
object SysProps extends LoggingLike {

  private val DelimiterChar: Char = 29
  val DefaultDelimiter: String = DelimiterChar.toString
  private val DelimiterRegex: Regex = new Regex(DefaultDelimiter)

  /** Adds the given name-value pair as a system property of this JVM instance if the value associated with the given
    * name does not exist. If the value does exist, it is overwritten.
    *
    * @param name  the system property name
    * @param value the system property value
    */
  def addOrOverwrite(name: String, value: String): Unit = {
    sys.props += name -> value
    // Do not log value as it can contain sensitive information
    logger.info(s"Added system property $name")
  }

  /** Concatenates the given values with the given delimiter
    * and adds it as a system property of this JVM instance if the value associated with the given name does not exist.
    * If the value does exist, it is overwritten.
    *
    * @param name      the system property name
    * @param values    the system property value
    * @param delimiter the delimiter for the values (default: [[DefaultDelimiter]])
    */
  def addDelimitedOrOverwrite(name: String, values: Seq[String], delimiter: String = DefaultDelimiter): Unit =
    addOrOverwrite(name, values.mkString(delimiter))

  /** Converts the given values to string,
    * concatenates them with the given delimiter,
    * and adds it as a system property of this JVM instance if the value associated with the given name does not exist.
    * If the value does exist, it is overwritten.
    *
    * @param name      the system property name
    * @param values    the system property value
    * @param delimiter the delimiter for the values (default: [[DefaultDelimiter]])
    * @param convert   the conversion function that converts the raw value to the expected return type
    *                  (default: {{ (a: A) => a.toString }})
    * @tparam A the type to convert from
    */
  def convertThenAddDelimitedOrOverwrite[A](
    name: String,
    values: Seq[A],
    delimiter: String = DefaultDelimiter
  )(
    convert: A => String = (a: A) => a.toString
  ): Unit =
    addDelimitedOrOverwrite(name, values.map(convert), delimiter = delimiter)

  /** Adds the given name-value pair as a system property of this JVM instance if the value associated with the given
    * name does not exist yet.
    *
    * @param name  the system property name
    * @param value the system property value
    */
  def addIfNotExists(name: String, value: String): Unit =
    if (!sys.props.exists(_._1 == name)) addOrOverwrite(name, value)
    else logger.info(s"The system property with name $name already exists")

  /** Concatenates the given values with the given delimiter and
    * adds it as a system property of this JVM instance if the value associated with the given name does not exist.
    *
    * @param name      the system property name
    * @param values    the system property value
    * @param delimiter the delimiter for the values (default: [[DefaultDelimiter]])
    */
  def addDelimitedIfNotExists(name: String, values: Seq[String], delimiter: String = DefaultDelimiter): Unit =
    addIfNotExists(name, values.mkString(delimiter))

  /** Converts the given values to string,
    * concatenates them with the given delimiter,
    * and adds it as a system property of this JVM instance if the value associated with the given name does not exist.
    *
    * @param name      the system property name
    * @param values    the system property value
    * @param delimiter the delimiter for the values (default: [[DefaultDelimiter]])
    * @param convert   the conversion function that converts the raw value to the expected return type
    *                  (default: {{ (a: A) => a.toString }})
    * @tparam A the type to convert from
    */
  def convertThenAddDelimitedIfNotExists[A](
    name: String,
    values: Seq[A],
    delimiter: String = DefaultDelimiter
  )(
    convert: A => String = (a: A) => a.toString
  ): Unit =
    addDelimitedIfNotExists(name, values.map(convert), delimiter = delimiter)

  /** Adds the given name-value pair as a system property of this JVM instance if the value associated with the given
    * name does not exist. If the value does exist, it is overwritten.
    *
    * @param prop the system property
    */
  def addOrOverwrite(prop: SystemProperty): Unit = addOrOverwrite(prop.name, prop.value)

  /** Adds the given name-value pair as a system property of this JVM instance if the value associated with the given
    * name does not exist yet.
    *
    * @param prop the system property
    */
  def addIfNotExists(prop: SystemProperty): Unit = addIfNotExists(prop.name, prop.value)

  /** Removes the property with the given name.
    *
    * @param name the system property name
    */
  def remove(name: String): Unit = sys.props -= name

  /** Returns the system property value with the given name.
    * If the value is not found, [[None]] is returned.
    *
    * @param name the system property name
    * @return the system property value associated with the given name or [[None]] if not found
    */
  def getOrNone(name: String): Option[String] = sys.props.get(name)

  /** Returns the system property values with the given name.
    * If the values are not found or empty, [[None]] is returned.
    *
    * @param name      the system property name
    * @param delimiter the delimiter for the values (default: [[DefaultDelimiter]])
    * @return the system property values associated with the given name or [[None]] if not found
    */
  def getSeqOrNone(name: String, delimiter: String = DefaultDelimiter): Option[Seq[String]] =
    getOrNone(name).map(v => splitValue(v, delimiter)).filter(_.nonEmpty)

  /** Returns the system property value for the given key.
    * If the value is not found, [[MissingSystemPropertyException]] is thrown.
    *
    * @param name the system property name
    * @return the system property value associated with the given name
    * @throws MissingSystemPropertyException when the property value associated with the given name is not found
    */
  def get(name: String): String = getOrNone(name).getOrElse(throw new MissingSystemPropertyException(name))

  /** Returns the system property values for the given key.
    * If the values are not found, [[MissingSystemPropertyException]] is thrown.
    *
    * @param name      the system property name
    * @param delimiter the delimiter for the values (default: [[DefaultDelimiter]])
    * @return the system property values associated with the given name
    * @throws MissingSystemPropertyException when the property value associated with the given name is not found
    */
  def getSeq(name: String, delimiter: String = DefaultDelimiter): Seq[String] = {
    val values = splitValue(get(name), delimiter)
    if (values.isEmpty) {
      throw new MissingSystemPropertyException(name)
    }
    values
  }

  /** Returns the system property value for the given key after converting to [[A]] using the conversion function
    * being passed in if the value exists in the system properties or [[None]].
    *
    * @param name    the system property name
    * @param convert the conversion function that converts the raw value to the expected return type
    * @tparam A the type to convert
    * @return the value of type [[A]] converted from the raw system property or [[None]]
    */
  def getAsOrNone[A](name: String)(convert: String => A): Option[A] = getOrNone(name).map(convert)

  /** Returns the system property values for the given key after converting to [[A]] using the conversion function
    * being passed in if the values exist in the system properties or [[None]].
    *
    * @param name      the system property name
    * @param delimiter the delimiter for the values (default: [[DefaultDelimiter]])
    * @param convert   the conversion function that converts the raw value to the expected return type
    * @tparam A the type to convert
    * @return the values of type [[A]] converted from the raw system property or [[None]]
    */
  def getAsSeqOrNone[A](name: String, delimiter: String = DefaultDelimiter)(convert: String => A): Option[Seq[A]] =
    getOrNone(name).map(splitValue(_, delimiter)).map(_.map(convert)).filter(_.nonEmpty)

  /** Returns the system property value for the given key after converting to [[A]] using the conversion function
    * being passed in if the value exists in the system properties.
    * If the value is not found, [[MissingSystemPropertyException]] is thrown.
    *
    * @param name    the system property name
    * @param convert the conversion function that converts the raw value to the expected return type
    * @tparam A the type to convert
    * @return the value of type [[A]] converted from the raw system property or the default value
    * @throws MissingSystemPropertyException when the property value associated with the given name is not found
    */
  def getAs[A](name: String)(convert: String => A): A =
    getAsOrNone(name)(convert).getOrElse(throw new MissingSystemPropertyException(name))

  /** Returns the system property values for the given key after converting to [[A]] using the conversion function
    * being passed in if the values exist in the system properties.
    * If the values are not found, [[MissingSystemPropertyException]] is thrown.
    *
    * @param name      the system property name
    * @param delimiter the delimiter for the values (default: [[DefaultDelimiter]])
    * @param convert   the conversion function that converts the raw value to the expected return type
    * @tparam A the type to convert
    * @return the value of type [[A]] converted from the raw system property or the default value
    * @throws MissingSystemPropertyException when the property value associated with the given name is not found
    */
  def getAsSeq[A](name: String, delimiter: String = DefaultDelimiter)(convert: String => A): Seq[A] =
    getAsSeqOrNone(name, delimiter = delimiter)(convert).getOrElse(throw new MissingSystemPropertyException(name))

  /** Returns the system property value for the given key after converting to [[A]] using the conversion function
    * being passed in or the default value.
    *
    * @param name         the system property name
    * @param defaultValue the default value to return when not found
    * @param convert      the conversion function that converts the raw value to the expected return type
    * @tparam A the type to convert
    * @return the value of type [[A]] converted from the raw system property or the default value
    */
  def getAsOrDefault[A](name: String, defaultValue: => A)(convert: String => A): A =
    getAsOrNone(name)(convert).getOrElse(defaultValue)

  /** Returns the system properties that satisfy the given predicate.
    * Note that the first element in the input to the predicate is the key of the property,
    * whereas the second element is the (raw) property.
    *
    * @param p a predicate
    * @return all system properties that satisfy the given predicate
    */
  def findAll(p: ((String, String)) => Boolean): Seq[(String, String)] = sys.props.toSeq.filter(p)

  /** Returns true if the system property exists.
    *
    * @param name the system property name
    * @return true if exists else false
    */
  def exists(name: String): Boolean = sys.props.exists(_._1 == name)

  private def splitValue(value: String, delimiter: String): Seq[String] = {
    val splitRegex = if (delimiter == DefaultDelimiter) DelimiterRegex else delimiter.r
    splitRegex.split(value)
  }
}
