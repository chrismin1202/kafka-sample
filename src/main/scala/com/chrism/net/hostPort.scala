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
package com.chrism.net

import java.{net => jn}

import org.apache.commons.lang3.StringUtils

import scala.util.matching.Regex

final case class HostPort(host: String, port: Int) extends Ordered[HostPort] {

  require(StringUtils.isNotBlank(host), "The host name must not be blank!")
  require(port >= 0, "The port number cannot be negative!")
  require(port <= HostPort.MaxPortNumber, s"The port number cannot exceed ${HostPort.MaxPortNumber}")

  @transient
  lazy val formatted: String = s"$host:$port"

  def toInetSocketAddress: jn.InetSocketAddress = new jn.InetSocketAddress(host, port)

  override def compare(that: HostPort): Int = HostPort.Order.compare(this, that)

  override def toString: String = formatted
}

object HostPort {

  val MaxPortNumber: Int = 65535
  private[this] val HostPortRegex: Regex = "([a-zA-Z0-9-._~:/?#\\[\\]@!$&'()*+;]+):([0-9]{1,5})(,|$)".r

  private val Order: Ordering[HostPort] = Ordering.by((hp: HostPort) => (hp.host, hp.port))

  def parse(hostPort: String): HostPort =
    parseOrNone(hostPort)
      .getOrElse(throw new IllegalArgumentException(s"$hostPort is not parsable!"))

  def parseOrNone(hostPort: String): Option[HostPort] = HostPortRegex.findFirstMatchIn(hostPort).map(format)

  def parseAll(hostPorts: String): Seq[HostPort] = HostPortRegex.findAllMatchIn(hostPorts).map(format).toSeq

  private[this] def format(m: Regex.Match): HostPort = HostPort(m.group(1), m.group(2).toInt)
}

object LocalhostPort {

  def apply(port: Int): HostPort = HostPort("localhost", port)
}
