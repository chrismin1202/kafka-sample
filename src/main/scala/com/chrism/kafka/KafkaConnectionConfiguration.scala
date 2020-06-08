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
package com.chrism.kafka

import com.chrism.commons.util.ProductUtils
import com.chrism.net.HostPort

import scala.collection.mutable

final class KafkaConnectionConfiguration private (val bootstrapServers: Set[HostPort])
    extends Product
    with Serializable {

  override def productElement(n: Int): Any =
    n match {
      case 0 => bootstrapServers
      case i => throw new IndexOutOfBoundsException(s"There is no element at $i")
    }

  override val productArity: Int = 1

  override def canEqual(that: Any): Boolean = that.isInstanceOf[KafkaConnectionConfiguration]

  override def equals(that: Any): Boolean = ProductUtils.productEquals(this, that)

  override def hashCode(): Int = ProductUtils.productHashCode(this)

  override def toString: String = ProductUtils.productToString(this)
}

object KafkaConnectionConfiguration {

  def builder: Builder = Builder()

  final class Builder private () {

    private[this] val _bootstrapServers: mutable.Set[HostPort] = mutable.Set.empty

    def build(): KafkaConnectionConfiguration = {
      require(_bootstrapServers.nonEmpty, "At least 1 bootstrap server must be specified!")

      new KafkaConnectionConfiguration(_bootstrapServers.toSet)
    }

    def addBootstrapServer(server: HostPort): this.type = {
      _bootstrapServers += server
      this
    }

    def addBootstrapServers(servers: Iterable[HostPort]): this.type = {
      _bootstrapServers ++= servers
      this
    }

    def bootstrapServers(servers: Iterable[HostPort]): this.type = {
      _bootstrapServers.clear()
      addBootstrapServers(servers)
    }
  }

  private[this] object Builder {

    def apply(): Builder = new Builder()
  }
}
