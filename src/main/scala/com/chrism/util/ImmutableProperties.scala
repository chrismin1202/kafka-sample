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
package com.chrism.util

import java.{util => ju}

final case class ImmutableProperties(properties: Map[Any, Any]) {

  def apply(key: Any): Any = valueOf(key)

  def valueOf(key: Any): Any = properties(key)

  def valueOfOrNone(key: Any): Option[Any] = properties.get(key)

  def stringValueOf(key: Any): String =
    valueOf(key) match {
      case s: String => s
      case other     => throw new ClassCastException(s"$other of type ${other.getClass} cannot be cast to String!")
    }

  def stringValueOfOrNone(key: Any): Option[String] =
    valueOfOrNone(key).collect { case s: String => s }

  def intValueOf(key: Any): Int =
    valueOf(key) match {
      case i: Int => i
      case other  => throw new ClassCastException(s"$other of type ${other.getClass} cannot be cast to Int!")
    }

  def intValueOfOrNone(key: Any): Option[Int] = valueOfOrNone(key).collect { case i: Int => i }

  def filterKeys(p: Any => Boolean): Map[Any, Any] = properties.filterKeys(p)

  /** Instantiates and returns a new instance of [[ju.Properties]].
    * Note that this method returns a new instance every time as [[ju.Properties]] is a mutable object.
    *
    * @return a new instance of [[ju.Properties]]
    */
  def toJProperties: ju.Properties = {
    val jProps = new ju.Properties()
    properties.foreach(p => jProps.put(p._1.asInstanceOf[AnyRef], p._2.asInstanceOf[AnyRef]))
    jProps
  }

  def size: Int = properties.size
}
