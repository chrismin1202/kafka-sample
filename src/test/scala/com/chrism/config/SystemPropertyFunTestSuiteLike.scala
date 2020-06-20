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

import com.chrism.commons.FunTestSuite
import com.chrism.log.LoggingLike

trait SystemPropertyFunTestSuiteLike extends LoggingLike {
  this: FunTestSuite =>

  protected final def withSysProps(prop: SystemProperty, moreProps: SystemProperty*)(testFun: => Any): Unit =
    withSysProps(prop +: moreProps)(testFun)

  protected final def withSysProps(props: Iterable[SystemProperty])(testFun: => Any): Unit = {
    props.foreach(SysProps.addOrOverwrite)
    withSysPropsRemoved(props)(testFun)
  }

  /** As opposed to [[withSysProps()]], this method assumes that the system properties have been already set prior to
    * invoking this method.
    *
    * @param props   the [[SystemProperty]] instances that have already been set
    * @param testFun the test logic
    */
  protected final def withSysPropsRemoved(props: Iterable[SystemProperty])(testFun: => Any): Unit =
    try {
      testFun
    } finally {
      val names = props.map(_.name)
      logger.info(s"Removing the following system properties: ${names.mkString("[", ", ", "]")}")
      names.foreach(SysProps.remove)

      val failedToRemove = names.filter(SysProps.exists)
      assert(
        failedToRemove.isEmpty,
        s"Failed to remove the following system properties: ${failedToRemove.mkString(", ")}")
    }
}
