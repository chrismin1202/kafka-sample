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
package com.chrism.kafka.streams.examples

import java.{util => ju}

import com.chrism.commons.util.{StringUtils, WithResource}
import com.chrism.kafka.serde.LongDeserializer
import com.chrism.kafka.streams.KafkaStreamsAppLike
import com.chrism.kafka.{CleanupPolicy, KafkaInputTopic, KafkaOutputTopic}
import com.chrism.log.LoggingLike
import com.chrism.net.HostPort
import com.chrism.util.{ImmutableProperties, PropertiesBuilder}
import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig, CreateTopicsResult}
import org.apache.kafka.common.serialization.{Deserializer, Serializer, StringDeserializer, StringSerializer}
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.{StreamsConfig, Topology}

import scala.util.matching.Regex

object WordCount {

  val DefaultApplicationId: String = "word-count-scala-example"

  val InputTopic: KafkaInputTopic =
    KafkaInputTopic("word-count-input", numPartitions = Some(1), replicationFactor = Some(1))

  val OutputTopic: KafkaOutputTopic[String, Long] = KafkaOutputTopic[String, Long](
    "word-count-output",
    numPartitions = Some(1),
    replicationFactor = Some(1),
    cleanupPolicy = Some(CleanupPolicy.Compact))

  private val WhitespaceRegex: Regex = "\\W+".r

  private[streams] def tokenize(text: String): Seq[String] =
    if (StringUtils.isBlank(text)) Seq.empty
    else WhitespaceRegex.split(text).map(_.toLowerCase)
}

final case class WordCount(appId: String = WordCount.DefaultApplicationId, bootstrapServers: Set[HostPort] = Set.empty)
    extends KafkaStreamsAppLike[String, String, String, Long]
    with LoggingLike {

  import WordCount.{tokenize, InputTopic, OutputTopic}
  import org.apache.kafka.streams.scala.ImplicitConversions._
  import org.apache.kafka.streams.scala.Serdes._

  @transient
  override lazy val inputKeySerializer: Serializer[String] = new StringSerializer()

  @transient
  override lazy val inputValueSerializer: Serializer[String] = new StringSerializer()

  @transient
  override lazy val outputKeyDeserializer: Deserializer[String] = new StringDeserializer()

  @transient
  override lazy val outputValueDeserializer: Deserializer[Long] = LongDeserializer()

  override lazy val topologyConfig: ImmutableProperties = PropertiesBuilder()
    .addOrOverwrite(StreamsConfig.APPLICATION_ID_CONFIG, appId)
    .addOrOverwrite(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, formattedBootstrapServers)
    .buildAsImmutableProperties

  override lazy val inputTopics: Set[KafkaInputTopic] = Set(InputTopic)

  override lazy val outputTopics: Set[KafkaOutputTopic[String, Long]] = Set(OutputTopic)

  override def createTopics(): CreateTopicsResult = {
    val config = PropertiesBuilder()
      .addOrOverwrite(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, formattedBootstrapServers)
      .buildAsJProperties
    WithResource(AdminClient.create(config))(
      _.createTopics(ju.Arrays.asList(InputTopic.newTopic, OutputTopic.newTopic)))
  }

  override def adminClientConfig: ImmutableProperties =
    PropertiesBuilder(super.adminClientConfig).buildAsImmutableProperties

  override def buildTopology(): Topology = {
    val builder = new StreamsBuilder()
    val stream = builder
      .stream[String, String](inputTopicNames)
      .flatMapValues(tokenize _)
      .groupBy((_, word) => word)
      .count()
      .toStream
    outputTopics.foreach(_.materializeTo(stream))
    builder.build()
  }
}
