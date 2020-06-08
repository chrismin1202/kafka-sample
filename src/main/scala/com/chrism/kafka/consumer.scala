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

import java.{util => ju}

import com.chrism.commons.util.{ProductUtils, WithResource}
import com.chrism.log.LoggingLike
import com.chrism.net.HostPort
import com.chrism.util.{ClassUtils, ImmutableProperties, PropertiesBuilder}
import org.apache.kafka.clients.consumer.{
  ConsumerConfig,
  ConsumerRecord,
  KafkaConsumer,
  OffsetAndMetadata,
  OffsetResetStrategy
}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{Deserializer, StringDeserializer}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

sealed abstract class Consumer[K, V, PV] protected (consumerProps: ImmutableProperties, conf: ConsumerConfiguration)
    extends Product
    with Serializable
    with LoggingLike {

  import scala.compat.java8.DurationConverters._

  protected def convertRecord(record: ConsumerRecord[K, V]): PV

  final def consumeMessageFromTopic(topic: String): Option[PV] =
    consumeMessagesFromTopic(topic, 1).flatMap(_.headOption)

  final def consumeMessagesFromTopic(topic: String, numMessages: Int): Option[Seq[PV]] =
    consumeMessagesFromTopics(Set(topic), numMessages).headOption.map(_._2)

  final def consumeMessageFromTopics(topics: Set[String]): Map[String, Option[PV]] =
    consumeMessagesFromTopics(topics, 1).mapValues(_.headOption)

  final def consumeMessagesFromTopics(topics: Set[String], numMessages: Int): Map[String, Seq[PV]] =
    consumeMessages(topics, Some(numMessages))

  final def consumeAnyMessagesFromTopic(topic: String): Option[Seq[PV]] =
    consumeAnyMessagesFromTopics(Set(topic)).headOption.map(_._2)

  final def consumeAnyMessagesFromTopics(topics: Set[String]): Map[String, Seq[PV]] = consumeMessages(topics, None)

  private def consumeMessages(topics: Set[String], numMessages: Option[Int]): Map[String, Seq[PV]] = {
    require(numMessages.isEmpty || numMessages.exists(_ > 0), "numMessages must be positive!")

    WithResource(new KafkaConsumer[K, V](consumerProps.toJProperties)) { kConsumer =>
      kConsumer.subscribe(topics.asJava)
      topics.foreach(t => kConsumer.partitionsFor(t))

      val buffer = topics.map(_ -> mutable.ListBuffer.empty[PV]).toMap
      val pollingTimeout = conf.pollingDuration.toJava
      var numConsumed = 0
      var polled = 0

      logger.info(s"Consumption timeout: ${conf.consumptionTimeout}")
      val timeout = System.nanoTime() + conf.consumptionTimeout.toNanos
      while (((numMessages.isEmpty && polled == 0) || numMessages.exists(numConsumed < _)) &&
             System.nanoTime() < timeout) {
        if (numMessages.isEmpty && polled == 0) {
          logger.info(s"Attempting to poll from the topics: ${topics.mkString(", ")}")
        }
        val iterator = kConsumer.poll(pollingTimeout).iterator()
        polled += 1
        while (iterator.hasNext && (numMessages.isEmpty || numMessages.exists(numConsumed < _))) {
          val record = iterator.next()
          buffer(record.topic()) += convertRecord(record)
          val partition = new TopicPartition(record.topic(), record.partition())
          val offset = new OffsetAndMetadata(record.offset() + 1)
          kConsumer.commitSync(ju.Collections.singletonMap(partition, offset))
          numConsumed += 1
        }
        logger.info(s"Consumed messages: $numConsumed")
      }
      buffer
    }
  }
}

final class DefaultConsumer[K, V] private (consumerProps: ImmutableProperties, conf: ConsumerConfiguration)
    extends Consumer[K, V, ConsumerRecord[K, V]](consumerProps, conf) {

  override protected def convertRecord(record: ConsumerRecord[K, V]): ConsumerRecord[K, V] = record

  override def productElement(n: Int): Any =
    n match {
      case 0 => consumerProps
      case 1 => conf
      case i => throw new IndexOutOfBoundsException(s"There is no element at $i")
    }

  override val productArity: Int = 2

  override def canEqual(that: Any): Boolean = that.isInstanceOf[DefaultConsumer[K, V]]

  override def equals(that: Any): Boolean = ProductUtils.productEquals(this, that)

  override def hashCode(): Int = ProductUtils.productHashCode(this)

  override def toString: String = ProductUtils.productToString(this)
}

object DefaultConsumer {

  private def apply[K, V](consumerProps: ImmutableProperties, conf: ConsumerConfiguration): DefaultConsumer[K, V] =
    new DefaultConsumer[K, V](consumerProps, conf)

  def builder[K, V, DeserK <: Deserializer[K], DeserV <: Deserializer[V]](
  )(
    implicit
    cTagK: ClassTag[DeserK],
    cTagV: ClassTag[DeserV]
  ): Builder[K, V, DeserK, DeserV] =
    Builder()

  final class Builder[K, V, DeserK <: Deserializer[K], DeserV <: Deserializer[V]] private (
    implicit
    cTagK: ClassTag[DeserK],
    cTagV: ClassTag[DeserV])
      extends ConsumerBuilder[K, V, ConsumerRecord[K, V], DeserK, DeserV, DefaultConsumer[K, V]] {

    override protected def newConsumer(
      consumerProps: ImmutableProperties,
      conf: ConsumerConfiguration
    ): DefaultConsumer[K, V] =
      new DefaultConsumer[K, V](consumerProps, conf)
  }

  private[this] object Builder {

    def apply[K, V, DeserK <: Deserializer[K], DeserV <: Deserializer[V]](
    )(
      implicit
      cTagK: ClassTag[DeserK],
      cTagV: ClassTag[DeserV]
    ): Builder[K, V, DeserK, DeserV] =
      new Builder()
  }
}

final class KeylessConsumer[V] private (consumerProps: ImmutableProperties, conf: ConsumerConfiguration)
    extends Consumer[String, V, V](consumerProps, conf) {

  override protected def convertRecord(record: ConsumerRecord[String, V]): V = record.value()

  override def productElement(n: Int): Any =
    n match {
      case 0 => consumerProps
      case 1 => conf
      case i => throw new IndexOutOfBoundsException(s"There is no element at $i")
    }

  override val productArity: Int = 2

  override def canEqual(that: Any): Boolean = that.isInstanceOf[KeylessConsumer[V]]

  override def equals(that: Any): Boolean = ProductUtils.productEquals(this, that)

  override def hashCode(): Int = ProductUtils.productHashCode(this)

  override def toString: String = ProductUtils.productToString(this)
}

object KeylessConsumer {

  private[this] implicit val cTagK: ClassTag[StringDeserializer] = ClassTag(classOf[StringDeserializer])

  private def apply[V](consumerProps: ImmutableProperties, conf: ConsumerConfiguration): KeylessConsumer[V] =
    new KeylessConsumer[V](consumerProps, conf)

  def builder[V, DeserV <: Deserializer[V]]()(implicit cTagV: ClassTag[DeserV]): Builder[V, DeserV] = Builder()

  final class Builder[V, DeserV <: Deserializer[V]] private (implicit cTagV: ClassTag[DeserV])
      extends ConsumerBuilder[String, V, V, StringDeserializer, DeserV, KeylessConsumer[V]] {

    override protected def newConsumer(
      consumerProps: ImmutableProperties,
      conf: ConsumerConfiguration
    ): KeylessConsumer[V] =
      new KeylessConsumer[V](consumerProps, conf)
  }

  private[this] object Builder {

    def apply[V, DeserV <: Deserializer[V]]()(implicit cTagV: ClassTag[DeserV]): Builder[V, DeserV] = new Builder()
  }
}

final class ConsumerConfiguration private (val pollingDuration: FiniteDuration, val consumptionTimeout: FiniteDuration)
    extends Product
    with Serializable {

  override def productElement(n: Int): Any =
    n match {
      case 0 => consumptionTimeout
      case 1 => pollingDuration
      case i => throw new IndexOutOfBoundsException(s"There is no element at $i")
    }

  override val productArity: Int = 2

  override def canEqual(that: Any): Boolean = that.isInstanceOf[ConsumerConfiguration]

  override def equals(that: Any): Boolean = ProductUtils.productEquals(this, that)

  override def hashCode(): Int = ProductUtils.productHashCode(this)

  override def toString: String = ProductUtils.productToString(this)
}

object ConsumerConfiguration {

  import scala.concurrent.duration._

  val DefaultPollingDuration: FiniteDuration = 5.seconds
  val DefaultConsumingTimeout: FiniteDuration = 7.seconds

  private def apply(pollingDuration: FiniteDuration, consumptionTimeout: FiniteDuration): ConsumerConfiguration = {
    require(pollingDuration != null, "The polling duration cannot be null!")
    require(pollingDuration > Duration.Zero, "The polling duration must be greater than 0!")
    require(consumptionTimeout != null, "The consumption timeout cannot be null!")
    require(consumptionTimeout > Duration.Zero, "The consumption timeout must be greater than 0!")
    require(
      pollingDuration <= consumptionTimeout,
      s"The polling duration ($pollingDuration) cannot be greater than the consumption timeout ($consumptionTimeout)!")

    new ConsumerConfiguration(pollingDuration, consumptionTimeout)
  }

  def builder: Builder = Builder()

  final class Builder private () {

    private[this] var _pollingDuration: Option[FiniteDuration] = None
    private[this] var _consumptionTimeout: Option[FiniteDuration] = None

    def build(): ConsumerConfiguration =
      ConsumerConfiguration(
        _pollingDuration.getOrElse(DefaultPollingDuration),
        _consumptionTimeout.getOrElse(DefaultConsumingTimeout))

    def pollingDuration(d: FiniteDuration): this.type = {
      _pollingDuration = Option(d)
      this
    }

    def consumptionTimeout(to: FiniteDuration): this.type = {
      _consumptionTimeout = Option(to)
      this
    }
  }

  private[this] object Builder {

    def apply(): Builder = new Builder()
  }
}

private[kafka] sealed abstract class ConsumerBuilder[
  K,
  V,
  PV,
  DeserK <: Deserializer[K],
  DeserV <: Deserializer[V],
  C <: Consumer[K, V, PV]
] protected (
  implicit
  cTagK: ClassTag[DeserK],
  cTagV: ClassTag[DeserV])
    extends LoggingLike {

  import ConsumerBuilder._

  private[this] var _bootstrapServers: Vector[HostPort] = Vector.empty
  private[this] var _groupId: String = DefaultGroupId
  private[this] var _clientId: Option[String] = None
  private[this] var _enableAutoCommit: Boolean = DefaultEnableAutoCommit
  private[this] var _autoCommitIntervalMs: Option[Int] = None
  private[this] var _sessionTimeoutMs: Int = DefaultSessionTimeoutMs
  private[this] var _confBuilder: ConsumerConfiguration.Builder = ConsumerConfiguration.builder

  protected def newConsumer(consumerProps: ImmutableProperties, conf: ConsumerConfiguration): C

  final def build(): C = {
    val servers = _bootstrapServers.distinct.map(_.formatted).sorted
    require(servers.nonEmpty, "At least 1 bootstrap server must be specified!")

    val builder = PropertiesBuilder()
      .addOrOverwrite(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers.mkString(","))
      .addOrOverwrite(ConsumerConfig.GROUP_ID_CONFIG, _groupId)
      .addOrOverwrite(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, _enableAutoCommit)
      .addOrOverwrite(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, _sessionTimeoutMs)
      .addOrOverwrite(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ClassUtils.classNameOf(cTagK.runtimeClass))
      .addOrOverwrite(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ClassUtils.classNameOf(cTagV.runtimeClass))
      .addOrOverwrite(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetResetStrategy.EARLIEST.toString.toLowerCase)

    _clientId
      .foreach { cid =>
        logger.info(s"Setting the ${ConsumerConfig.CLIENT_ID_CONFIG} to $cid...")
        builder.addOrOverwrite(ConsumerConfig.CLIENT_ID_CONFIG, cid)
      }

    if (_enableAutoCommit) {
      _autoCommitIntervalMs match {
        case Some(interval) =>
          logger.info(s"Enabling auto-commit with the interval ${interval}ms...")
          builder.addOrOverwrite(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, interval)
        case _ => logger.info("Enabling auto-commit with the default interval.")
      }
    } else {
      logger.info("Disabling auto-commit...")
      _autoCommitIntervalMs
        .foreach(i =>
          logger.info(
            "Auto-commit interval is specified without enabling auto-commit. The specified commit interval (" +
              i + "ms) will be ignored."))
    }

    newConsumer(builder.buildAsImmutableProperties, _confBuilder.build())
  }

  final def addConnectionConfiguration(conf: KafkaConnectionConfiguration): this.type =
    addBootstrapServers(conf.bootstrapServers)

  final def connectionConfiguration(conf: KafkaConnectionConfiguration): this.type =
    bootstrapServers(conf.bootstrapServers)

  final def addBootstrapServer(host: String, port: Int): this.type = addBootstrapServer(HostPort(host, port))

  final def addBootstrapServer(hp: HostPort): this.type = {
    _bootstrapServers = _bootstrapServers :+ hp
    this
  }

  final def addBootstrapServers(hps: Iterable[HostPort]): this.type = {
    _bootstrapServers = _bootstrapServers ++ hps
    this
  }

  final def bootstrapServers(hps: Iterable[HostPort]): this.type = {
    _bootstrapServers = hps.toVector
    this
  }

  final def groupId(id: String): this.type = {
    _groupId = id
    this
  }

  final def clientId(id: String): this.type = {
    _clientId = Option(id)
    this
  }

  final def enableAutoCommit(enable: Boolean): this.type = {
    _enableAutoCommit = enable
    this
  }

  final def autoCommitIntervalMs(interval: Int): this.type = {
    _autoCommitIntervalMs = Some(interval)
    this
  }

  final def sessionTimeoutMs(timeout: Int): this.type = {
    _sessionTimeoutMs = timeout
    this
  }

  final def addConf(f: ConsumerConfiguration.Builder => ConsumerConfiguration.Builder): this.type = {
    _confBuilder = f(_confBuilder)
    this
  }
}

private[this] object ConsumerBuilder {

  private val DefaultGroupId: String = "CHRISM"
  private val DefaultEnableAutoCommit: Boolean = true
  private val DefaultSessionTimeoutMs: Int = 30000
}
