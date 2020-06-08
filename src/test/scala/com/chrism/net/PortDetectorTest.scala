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
import com.chrism.commons.util.WithResource

final class PortDetectorTest extends FunTestSuite {

  test("detecting a free port") {
    val port = PortDetector.findFreePort()
    assert(PortDetector.isFree(port))
  }

  test("isFree should return false if the port is already bound") {
    WithResource(new jn.ServerSocket(0)) { ss =>
      ss.setReuseAddress(true)
      val port = ss.getLocalPort
      assert(PortDetector.isFree(port) === false)
    }
  }
}
