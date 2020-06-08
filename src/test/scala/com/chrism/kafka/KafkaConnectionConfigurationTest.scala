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

import com.chrism.commons.FunTestSuite
import com.chrism.net.HostPort

final class KafkaConnectionConfigurationTest extends FunTestSuite {

  test("resetting bootstrap servers") {
    val conf = KafkaConnectionConfiguration.builder
      .addBootstrapServer(HostPort("server1", 1000))
      .addBootstrapServer(HostPort("server2", 2000))
      .bootstrapServers(Set(HostPort("server3", 3000)))
      .build()
    conf.bootstrapServers should contain theSameElementsAs Seq(HostPort("server3", 3000))
  }

  test("equals/hashCode") {
    val conf1 = KafkaConnectionConfiguration.builder
      .addBootstrapServer(HostPort("server1", 1000))
      .addBootstrapServer(HostPort("server2", 2000))
      .addBootstrapServer(HostPort("server3", 3000))
      .build()
    val conf2 = KafkaConnectionConfiguration.builder
      .bootstrapServers(Set(HostPort("server3", 3000), HostPort("server2", 2000), HostPort("server1", 1000)))
      .build()
    assert(conf1 === conf2)
    assert(conf1.hashCode() === conf2.hashCode())
  }
}
