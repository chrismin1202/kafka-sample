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

import com.chrism.log.LoggingLike

object PortDetector extends LoggingLike {

  /** Finds a (local) port that is open.
    *
    * @return a port that is open or [[None]]
    * @throws jn.BindException thrown if no free port is found
    */
  def findFreePort(): Int = {
    logger.info("Detecting a free port...")
    val ss = new jn.ServerSocket(0)
    ss.setReuseAddress(true)
    val port = ss.getLocalPort
    ss.close()
    if (port >= 0) {
      logger.info(s"Found a free port: $port")
      port
    } else {
      logger.warn("Free port not found!")
      throw new jn.BindException("No port can be bound!")
    }
  }

  /** Checks whether the given port is free.
    *
    * @param port the port to check
    * @return {{{ true }}} if the port is free else {{{ false }}}
    */
  def isFree(port: Int): Boolean = {
    var ss: jn.ServerSocket = null
    try {
      logger.info(s"Checking if the port $port is open...")
      ss = new jn.ServerSocket(port)
      ss.setReuseAddress(true)
      true
    } catch {
      case _: Throwable =>
        logger.info(s"The port $port is bound!")
        false
    } finally {
      if (ss != null) {
        ss.close()
      }
    }
  }
}
