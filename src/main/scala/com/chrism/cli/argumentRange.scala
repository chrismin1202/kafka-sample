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
package com.chrism.cli

sealed abstract class ArgumentRange protected (val min: Int, val max: Int) extends Product with Serializable {

  final def hasArgs: Boolean = max > 0

  final def isWithinRange(numArgs: Int): Boolean = numArgs >= min && numArgs <= max

  override def toString: String = s"[$min,$max]"
}

object ArgumentRange {

  def apply(min: Int, max: Int): ArgumentRange = {
    require(min >= 0, "The min value must be non-negative!")
    require(max >= 0, "The max value must be non-negative!")
    require(max >= min, s"The max value cannot be less than the min value!")

    if (min == max) FixedArgumentRange(min)
    else Impl(min, max)
  }

  private[this] final case class Impl(override val min: Int, override val max: Int) extends ArgumentRange(min, max)

}

sealed abstract class FixedArgumentRange protected (numArgs: Int) extends ArgumentRange(numArgs, numArgs) {

  override def toString: String = numArgs.toString
}

object FixedArgumentRange {

  def apply(numArgs: Int): FixedArgumentRange =
    numArgs match {
      case 0          => ZeroArgument
      case 1          => OneArgument
      case n if n > 0 => Impl(n)
      case _          => throw new IllegalArgumentException("The number of arguments must be non-negative!")
    }

  private[this] final case class Impl(numArgs: Int) extends FixedArgumentRange(numArgs)

}

case object ZeroArgument extends FixedArgumentRange(0) {

  override def toString: String = "None"
}

case object OneArgument extends FixedArgumentRange(1)
