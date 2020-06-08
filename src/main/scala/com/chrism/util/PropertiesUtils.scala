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

object PropertiesUtils {

  object implicits {

    implicit final class PropertiesOps(props: ju.Properties) {

      def +=(kv: (Any, Any)): ju.Properties = {
        props.put(kv._1.asInstanceOf[AnyRef], kv._2.asInstanceOf[AnyRef])
        props
      }

      def addOrOverwrite(k: Any, v: Any): ju.Properties = this += (k, v)

      /** Finds and returns the value of type [[V]] if found or null.
        *
        * @param k the key
        * @tparam V the type of the value
        * @return the value associated with the key or null if the value is not found
        */
      def getRefOrNull[V <: AnyRef](k: Any): V = {
        val v = getRawOrNull(k)
        if (v == null) null.asInstanceOf[V] else v.asInstanceOf[V]
      }

      /** Finds and returns the value of type [[V]] if found or null.
        *
        * @param k the key
        * @tparam V the type of the value
        * @return the value associated with the key or null if the value is not found
        * @throws NoSuchElementException thrown when the value associated with the given key is not found
        */
      def getRef[V <: AnyRef](k: Any): V = getRaw(k).asInstanceOf[V]

      /** As opposed to [[getRefOrNull()]], this method returns [[None]]
        * if the value associated with the key is not found.
        *
        * @param k the key
        * @tparam V the type of the value
        * @return the value associated with the key or [[None]] if the value is not found
        */
      def getRefOrNone[V <: AnyRef](k: Any): Option[V] = Option(getRefOrNull(k))

      def getString(k: Any): String = getRef[String](k)

      def getStringOrNone(k: Any): Option[String] = Option(getString(k))

      /** As opposed to [[getRefOrNull()]], this method throws [[NoSuchElementException]]
        * as [[AnyVal]] types are not nullable.
        *
        * @param k the key
        * @tparam V the type of the value (a subclass of [[AnyVal]])
        * @return the value associated with the key or null if the value is not found
        * @throws NoSuchElementException thrown when the value associated with the given key is not found
        */
      def getVal[V <: AnyVal](k: Any): V = getRaw(k).asInstanceOf[V]

      /** As opposed to [[getVal()]], this method returns [[None]] if the value associated with the key is not found.
        *
        * @param k the key
        * @tparam V the type of the value (a subclass of [[AnyVal]])
        * @return the value associated with the key or null if the value is not found
        */
      def getValOrNone[V <: AnyVal](k: Any): Option[V] = Option(getRawOrNull(k)).map(_.asInstanceOf[V])

      def getByte(k: Any): Byte = getVal[Byte](k)

      def getShort(k: Any): Short = getVal[Short](k)

      def getInt(k: Any): Int = getVal[Int](k)

      def getLong(k: Any): Long = getVal[Long](k)

      def getFloat(k: Any): Float = getVal[Float](k)

      def getDouble(k: Any): Double = getVal[Double](k)

      def getBoolean(k: Any): Boolean = getVal[Boolean](k)

      def getByteOrNone(k: Any): Option[Byte] = getValOrNone(k)

      def getShortOrNone(k: Any): Option[Short] = getValOrNone(k)

      def getIntOrNone(k: Any): Option[Int] = getValOrNone(k)

      def getLongOrNone(k: Any): Option[Long] = getValOrNone(k)

      def getFloatOrNone(k: Any): Option[Float] = getValOrNone(k)

      def getDoubleOrNone(k: Any): Option[Double] = getValOrNone(k)

      def getBooleanOrNone(k: Any): Option[Boolean] = getValOrNone(k)

      private def getRawOrNull(k: Any): AnyRef = props.get(k.asInstanceOf[AnyRef])

      private def getRaw(k: Any): AnyRef = {
        val ref = getRawOrNull(k)
        if (ref == null) {
          throw new NoSuchElementException(s"There is no value associated with the key $k")
        }
        ref
      }
    }
  }
}
