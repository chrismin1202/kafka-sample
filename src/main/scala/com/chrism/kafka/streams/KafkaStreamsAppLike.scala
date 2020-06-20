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
package com.chrism.kafka.streams

import java.{util => ju}

import com.chrism.commons.util.WithResource
import com.chrism.kafka.{KafkaInputTopic, KafkaOutputTopic}
import com.chrism.log.LoggingLike
import com.chrism.net.{HostPort, LocalHostPort, PortDetector}
import com.chrism.util.{ImmutableProperties, PropertiesBuilder}
import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig, CreateTopicsResult, NewTopic}
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import org.apache.kafka.streams.Topology

import scala.collection.JavaConverters._

// TODO: Create a driver

/** A base trait for Kafka streaming application.
  *
  * Limitation:
  *   - All input topics must have the same key and value types
  *   - All output topics must have the same key and value types
  *
  * @tparam KIn the type of the input topic key
  * @tparam VIn the type of the input topic value
  * @tparam KOut the type of the output topic key
  * @tparam VOut the type of the output topic value
  */
trait KafkaStreamsAppLike[KIn, VIn, KOut, VOut] extends LoggingLike {

  /** Specifies the bootstrap servers.
    *
    * If empty, localhost is assumed and the app will be bound to a random available port.
    *
    * @return a set of bootstrap servers
    */
  def bootstrapServers: Set[HostPort]

  def inputKeySerializer: Serializer[KIn]

  def inputValueSerializer: Serializer[VIn]

  def outputKeyDeserializer: Deserializer[KOut]

  def outputValueDeserializer: Deserializer[VOut]

  def inputTopics: Set[KafkaInputTopic]

  def outputTopics: Set[KafkaOutputTopic[KOut, VOut]]

  def topologyConfig: ImmutableProperties

  /** Builds the [[Topology]] for the streaming application.
    *
    * @return the [[Topology]] to run
    */
  def buildTopology(): Topology

  @transient
  final lazy val inputTopicNames: Set[String] = inputTopics.map(_.topic)

  @transient
  final lazy val outputTopicNames: Set[String] = outputTopics.map(_.topic)

  @transient
  private[this] lazy val newTopics: ju.Collection[NewTopic] =
    (inputTopics.map(_.newTopic) ++ outputTopics.map(_.newTopic)).asJavaCollection

  @transient
  protected final lazy val formattedBootstrapServers: String =
    if (bootstrapServers.isEmpty) {
      val port = PortDetector.findFreePort()
      logger.info(s"No bootstrap server specified. Binding to localhost:$port")
      LocalHostPort(port).formatted
    } else bootstrapServers.toSeq.sorted.map(_.formatted).mkString(",")

  /** Override if additional configurations need to be added.
    * The default configuration is [[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG]], which is set to [[bootstrapServers]].
    *
    * It is recommended to override as follows:
    * {{{
    *   override def adminClientConfig: ImmutableProperties =
    *     PropertiesBuilder(super.adminClientConfig)
    *       // add configurations here
    *       .buildAsImmutableProperties
    * }}}
    * If cake pattern is employed, the method should not be overridden as {{{ final }}}.
    *
    * @return the [[AdminClient]] configurations
    */
  def adminClientConfig: ImmutableProperties =
    PropertiesBuilder()
      .addOrOverwrite(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, formattedBootstrapServers)
      .buildAsImmutableProperties

  /** Creates topics.
    *
    * Override if one needs to customize the way the topics are created.
    * Or if topics need to be created some other way, override and stub out the method.
    *
    * @return the [[CreateTopicsResult]] returned by [[AdminClient]]
    */
  def createTopics(): CreateTopicsResult = WithResource(createAdminClient())(_.createTopics(newTopics))

  protected final def createAdminClient(): AdminClient = AdminClient.create(adminClientConfig.toJProperties)
}
