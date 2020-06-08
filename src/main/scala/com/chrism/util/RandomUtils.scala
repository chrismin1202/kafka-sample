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

import java.util.{concurrent => juc}

object RandomUtils {

  def randomPositiveInt(
    lowerBound: Int = 1,
    upperBound: Int = Int.MaxValue,
    upperBoundInclusive: Boolean = true
  ): Int = {
    require(lowerBound >= 1, "The lowerBound must be greater than or equal to 1!")
    require(upperBound <= Int.MaxValue, "The upperBound can be at most Int.MaxValue!")
    require(lowerBound < upperBound, "The lowerBound must be less than the upperBound!")

    val upper = if (upperBoundInclusive && upperBound < Int.MaxValue) upperBound + 1 else upperBound
    localRandom().nextInt(lowerBound, upper)
  }

  def randomPositiveLong(
    lowerBound: Long = 1L,
    upperBound: Long = Long.MaxValue,
    upperBoundInclusive: Boolean = true
  ): Long = {
    require(lowerBound >= 1L, "The lowerBound must be greater than or equal to 1!")
    require(upperBound <= Long.MaxValue, "The upperBound can be at most Long.MaxValue!")
    require(lowerBound < upperBound, "The lowerBound must be less than the upperBound!")

    val upper = if (upperBoundInclusive && upperBound < Long.MaxValue) upperBound + 1L else upperBound
    localRandom().nextLong(lowerBound, upper)
  }

  private def localRandom(/* IO */ ): juc.ThreadLocalRandom = juc.ThreadLocalRandom.current()
}
