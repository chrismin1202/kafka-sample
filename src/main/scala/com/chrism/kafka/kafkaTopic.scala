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

import java.{lang => jl, time => jt, util => ju}

import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.streams.scala.kstream.{KStream, Produced}

import scala.compat.java8.DurationConverters._
import scala.compat.java8.OptionConverters._
import scala.concurrent.duration.FiniteDuration

sealed trait KafkaTopicLike {

  def topic: String

  def numPartitions: Option[Int]

  def replicationFactor: Option[Short]

  def cleanupPolicy: Option[CleanupPolicy]

  @transient
  private[this] lazy val jNumPartitions: ju.Optional[jl.Integer] = numPartitions.map(_.asInstanceOf[jl.Integer]).asJava

  @transient
  private[this] lazy val jReplicationFactor: ju.Optional[jl.Short] = numPartitions.map(_.asInstanceOf[jl.Short]).asJava

  def newTopic: NewTopic = {
    val nTopic = new NewTopic(topic, jNumPartitions, jReplicationFactor)
    cleanupPolicy.map(_.name).foreach(ju.Collections.singletonMap("cleanup.policy", _))
    nTopic
  }
}

final case class KafkaInputTopic(
  topic: String,
  numPartitions: Option[Int] = None,
  replicationFactor: Option[Short] = None,
  cleanupPolicy: Option[CleanupPolicy] = None,
  startTimestamp: Option[jt.Instant] = None,
  autoAdvance: Option[FiniteDuration] = None)
    extends KafkaTopicLike {

  @transient
  lazy val jAutoAdvance: Option[jt.Duration] = autoAdvance.map(_.toJava)
}

final case class KafkaOutputTopic[K, V](
  topic: String,
  numPartitions: Option[Int] = None,
  replicationFactor: Option[Short] = None,
  cleanupPolicy: Option[CleanupPolicy] = None)
    extends KafkaTopicLike {

  def materializeTo(stream: KStream[K, V])(implicit produced: Produced[K, V]): Unit = stream.to(topic)
}
