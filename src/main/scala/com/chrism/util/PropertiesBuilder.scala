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

import scala.collection.mutable

final class PropertiesBuilder private (props: mutable.AnyRefMap[AnyRef, AnyRef]) {

  /** Creates and returns a new instance of [[ImmutableProperties]] with all properties that have been accumulated.
    *
    * @return a new instance of [[ImmutableProperties]] with all properties accumulated so far
    */
  def buildAsImmutableProperties: ImmutableProperties = ImmutableProperties(props.toMap)

  /** Creates and returns a new instance of [[ju.Properties]] with all properties that have been accumulated.
    * Note that it returns a new instance because [[ju.Properties]] is mutable.
    * If you are planning on having [[ju.Properties]] as a member of a class, especially case class,
    * consider using [[ImmutableProperties]] by calling [[buildAsImmutableProperties]] instead.
    *
    * @return a new instance of [[ju.Properties]] with all properties accumulated so far
    */
  def buildAsJProperties: ju.Properties = {
    val jProps = new ju.Properties()
    props.foreach(p => jProps.put(p._1, p._2))
    jProps
  }

  def +=(kv: (Any, Any)): this.type = {
    props += (kv._1.asInstanceOf[AnyRef], kv._2.asInstanceOf[AnyRef])
    this
  }

  def ++=(morePros: scala.collection.Map[_, _]): this.type = {
    morePros.foreach(this += _)
    this
  }

  def addOrOverwrite(k: Any, v: Any): this.type = this += (k, v)
}

object PropertiesBuilder {

  def apply(): PropertiesBuilder = new PropertiesBuilder(mutable.AnyRefMap[AnyRef, AnyRef]())

  /** Note that the values from the given instance of [[ju.Properties]] are thin-copied,
    * i.e., the reference to the values are copied, but the key-value pairs themselves are not; therefore,
    * if key and/or value is/are mutable, the mutation will be reflected.
    *
    * @param props an instance of [[ju.Properties]]
    * @return a new builder with all values copied
    */
  def apply(props: ju.Properties): PropertiesBuilder = {
    val map = mutable.AnyRefMap[AnyRef, AnyRef]()
    val iterator = props.entrySet().iterator()
    while (iterator.hasNext) {
      val e = iterator.next()
      map += e.getKey -> e.getValue
    }
    new PropertiesBuilder(map)
  }

  def apply(props: ImmutableProperties): PropertiesBuilder = {
    val map = mutable.AnyRefMap[AnyRef, AnyRef]()
    props.properties.foreach(p => map += (p._1.asInstanceOf[AnyRef] -> p._2.asInstanceOf[AnyRef]))
    new PropertiesBuilder(map)
  }
}
