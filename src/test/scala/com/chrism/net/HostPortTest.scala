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

import com.chrism.commons.FunTestSuite

final class HostPortTest extends FunTestSuite {

  test("formatting host:port") {
    val hp = HostPort("host.name", 666)
    assert(hp.formatted === "host.name:666")
    assert(hp.toString === "host.name:666")
  }

  test("formatting localhost:port") {
    val hp = LocalHostPort(666)
    assert(hp.formatted === "localhost:666")
    assert(hp.toString === "localhost:666")
  }

  test("converting host:port to java.net.InetSocketAddress") {
    val hp = HostPort("host.name", 666)
    assert(hp.toInetSocketAddress === new jn.InetSocketAddress("host.name", 666))
  }

  test("converting localhost:port to java.net.InetSocketAddress") {
    val hp = LocalHostPort(666)
    assert(hp.toInetSocketAddress === new jn.InetSocketAddress("localhost", 666))
  }

  test("parsing host:port") {
    val expected = HostPort("https://host.name", 8080)
    assert(HostPort.parse("https://host.name:8080") === expected)
  }

  test("parsing malformed host:port") {
    assert(HostPort.parseOrNone("https://host.name8080").isEmpty)
    intercept[IllegalArgumentException] {
      HostPort.parse("https://host.name8080")
    }
  }

  test("parsing multiple host:ports") {
    val servers = (1 to 10).map(i => s"https://host$i.name:8088").mkString(",")
    HostPort.parseAll(servers) should contain theSameElementsInOrderAs (1 to 10)
      .map(i => HostPort(s"https://host$i.name", 8088))
  }

  test("ordering multiple instances") {
    val hostPorts = Seq(
      LocalHostPort(1095),
      LocalHostPort(8080),
      HostPort("server1", 80),
      HostPort("server2", 8090),
      HostPort("a2", 80),
      HostPort("a1", 9040),
    )
    hostPorts.sorted should contain theSameElementsInOrderAs Seq(
      HostPort("a1", 9040),
      HostPort("a2", 80),
      LocalHostPort(1095),
      LocalHostPort(8080),
      HostPort("server1", 80),
      HostPort("server2", 8090),
    )
  }
}
