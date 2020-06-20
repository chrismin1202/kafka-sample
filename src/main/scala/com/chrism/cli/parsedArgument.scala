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

sealed abstract class ParsedArgument(private[cli] final val rawArgs: Seq[String]) extends Product with Serializable {

  @transient
  lazy val numArgs: Int = rawArgs.size
}

case object Argumentless extends ParsedArgument(Seq.empty)

final case class SingleArgument(arg: String) extends ParsedArgument(Seq(arg))

final case class MultiArgument(args: Seq[String]) extends ParsedArgument(args) {

  def isEmpty: Boolean = args.isEmpty

  def nonEmpty: Boolean = args.nonEmpty
}

object MultiArgument {

  def apply(arg: String, moreArgs: String*): MultiArgument = MultiArgument(arg +: moreArgs)
}
