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

import com.chrism.commons.util.{ProductUtils, WithResource}
import com.chrism.log.LoggingLike
import com.chrism.net.HostPort
import com.chrism.util.{ClassUtils, ImmutableProperties, PropertiesBuilder}
import org.apache.commons.lang3.StringUtils
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.{Serializer, StringSerializer}

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import scala.util.matching.Regex

sealed abstract class Producer[K, V] protected (
  final val producerProps: ImmutableProperties,
  conf: ProducerConfiguration)
    extends Product
    with Serializable
    with LoggingLike {

  protected final def sendRecord(record: ProducerRecord[K, V]): Boolean = sendRecords(Seq(record)).head

  protected final def sendRecords(records: Seq[ProducerRecord[K, V]]): Seq[Boolean] =
    WithResource(new KafkaProducer[K, V](producerProps.toJProperties))(kProducer =>
      try {
        logger.info(s"Attempting to send ${records.size} records...")
        records
          .map(record =>
            try {
              val f = kProducer.send(record)
              conf.retrievalTimeout.map(to => f.get(to._1, to._2)).getOrElse(f.get()) != null
            } catch {
              case t: Throwable =>
                logger.error(t)(s"Might have failed to send the record $record to the topic '${record.topic()}'")
                false
            })
      } finally {
        kProducer.flush()
      })

  // TODO: Implement async sending
}

final class KeyValueProducer[K, V] private (producerProps: ImmutableProperties, conf: ProducerConfiguration)
    extends Producer[K, V](producerProps, conf) {

  def send(topic: String, key: K, value: V): Boolean = sendRecord(new ProducerRecord[K, V](topic, key, value))

  def sendAll(topic: String, records: Map[K, Seq[V]]): Boolean =
    sendRecords(records.toSeq.flatMap(kv => kv._2.map(new ProducerRecord[K, V](topic, kv._1, _))))
      .forall(identity)

  def sendAll(recordsPerTopic: Map[String, Map[K, Seq[V]]]): Boolean =
    sendRecords(
      recordsPerTopic.toSeq
        .flatMap(tr =>
          tr._2.toSeq
            .flatMap(kv => kv._2.map(new ProducerRecord[K, V](tr._1, kv._1, _)))))
      .forall(identity)

  override def productElement(n: Int): Any =
    n match {
      case 0 => producerProps
      case 1 => conf
      case i => throw new IndexOutOfBoundsException(s"There is no element at $i")
    }

  override val productArity: Int = 2

  override def canEqual(that: Any): Boolean = that.isInstanceOf[KeyValueProducer[K, V]]

  override def equals(that: Any): Boolean = ProductUtils.productEquals(this, that)

  override def hashCode(): Int = ProductUtils.productHashCode(this)

  override def toString: String = ProductUtils.productToString(this)
}

object KeyValueProducer {

  private def apply[K, V](producerProps: ImmutableProperties, conf: ProducerConfiguration): KeyValueProducer[K, V] =
    new KeyValueProducer(producerProps, conf)

  def builder[K, V, SerK <: Serializer[K], SerV <: Serializer[V]](
  )(
    implicit
    cTagK: ClassTag[SerK],
    cTagV: ClassTag[SerV]
  ): Builder[K, V, SerK, SerV] =
    Builder()

  final class Builder[K, V, SerK <: Serializer[K], SerV <: Serializer[V]] private (
    implicit
    cTagK: ClassTag[SerK],
    cTagV: ClassTag[SerV])
      extends ProducerBuilder[K, V, SerK, SerV, KeyValueProducer[K, V]] {

    override protected def newProducer(
      producerProps: ImmutableProperties,
      conf: ProducerConfiguration
    ): KeyValueProducer[K, V] =
      KeyValueProducer(producerProps, conf)
  }

  private[this] object Builder {

    def apply[K, V, SerK <: Serializer[K], SerV <: Serializer[V]](
    )(
      implicit
      cTagK: ClassTag[SerK],
      cTagV: ClassTag[SerV]
    ): Builder[K, V, SerK, SerV] =
      new Builder()
  }
}

final class KeylessProducer[V] private (producerProps: ImmutableProperties, conf: ProducerConfiguration)
    extends Producer[String, V](producerProps, conf) {

  def send(topic: String, value: V): Boolean = sendRecord(new ProducerRecord[String, V](topic, value))

  def sendAll(topic: String, values: Seq[V]): Boolean =
    sendRecords(values.map(new ProducerRecord[String, V](topic, _)))
      .forall(identity)

  def sendAll(valuesPerTopic: Map[String, Seq[V]]): Boolean =
    sendRecords(valuesPerTopic.toSeq.flatMap(kv => kv._2.map(new ProducerRecord[String, V](kv._1, _))))
      .forall(identity)

  override def productElement(n: Int): Any =
    n match {
      case 0 => producerProps
      case 1 => conf
      case i => throw new IndexOutOfBoundsException(s"There is no element at $i")
    }

  override val productArity: Int = 2

  override def canEqual(that: Any): Boolean = that.isInstanceOf[KeylessProducer[V]]

  override def equals(that: Any): Boolean = ProductUtils.productEquals(this, that)

  override def hashCode(): Int = ProductUtils.productHashCode(this)

  override def toString: String = ProductUtils.productToString(this)
}

object KeylessProducer {

  private[this] implicit val cTagK: ClassTag[StringSerializer] = ClassTag(classOf[StringSerializer])

  private def apply[V](producerProps: ImmutableProperties, conf: ProducerConfiguration): KeylessProducer[V] =
    new KeylessProducer(producerProps, conf)

  def builder[V, SerV <: Serializer[V]]()(implicit cTagV: ClassTag[SerV]): Builder[V, SerV] =
    Builder()

  final class Builder[V, SerV <: Serializer[V]] private (implicit cTagV: ClassTag[SerV])
      extends ProducerBuilder[String, V, StringSerializer, SerV, KeylessProducer[V]] {

    override protected def newProducer(
      producerProps: ImmutableProperties,
      conf: ProducerConfiguration
    ): KeylessProducer[V] =
      KeylessProducer(producerProps, conf)
  }

  private[this] object Builder {

    def apply[V, SerV <: Serializer[V]]()(implicit cTagV: ClassTag[SerV]): Builder[V, SerV] = new Builder()
  }
}

final class ProducerConfiguration private (val retrievalTimeout: Option[Duration]) extends Product with Serializable {

  override def productElement(n: Int): Any =
    n match {
      case 0 => retrievalTimeout
      case i => throw new IndexOutOfBoundsException(s"There is no element at $i")
    }

  override val productArity: Int = 1

  override def canEqual(that: Any): Boolean = that.isInstanceOf[ProducerConfiguration]

  override def equals(that: Any): Boolean = ProductUtils.productEquals(this, that)

  override def hashCode(): Int = ProductUtils.productHashCode(this)

  override def toString: String = ProductUtils.productToString(this)
}

object ProducerConfiguration {

  private def apply(retrievalDuration: Option[Duration]): ProducerConfiguration = {
    require(
      retrievalDuration.isEmpty || retrievalDuration.exists(_ > Duration.Zero),
      "The retrieval duration cannot be null!")

    new ProducerConfiguration(retrievalDuration)
  }

  def builder: Builder = Builder()

  final class Builder private () {

    private[this] var _retrievalTimeout: Option[Duration] = None

    def build(): ProducerConfiguration = ProducerConfiguration(_retrievalTimeout)

    def retrievalTimeout(to: Duration): this.type = {
      _retrievalTimeout = Option(to)
      this
    }
  }

  private[this] object Builder {

    def apply(): Builder = new Builder()
  }
}

private[kafka] sealed abstract class ProducerBuilder[
  K,
  V,
  SerK <: Serializer[K],
  SerV <: Serializer[V],
  P <: Producer[K, V]
] protected (
  implicit
  cTagK: ClassTag[SerK],
  cTagV: ClassTag[SerV]) {

  import ProducerBuilder.commaSplit

  private[this] var _bootstrapServers: Vector[String] = Vector.empty
  private[this] var _builder: PropertiesBuilder = PropertiesBuilder()
  private[this] var _confBuilder: ProducerConfiguration.Builder = ProducerConfiguration.builder

  protected def newProducer(producerProps: ImmutableProperties, conf: ProducerConfiguration): P

  final def build(): P = {
    val servers = _bootstrapServers.distinct.sorted
    require(servers.nonEmpty, "At least 1 server name must be specified!")

    _builder
      .addOrOverwrite(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers.mkString(","))
      .addOrOverwrite(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ClassUtils.classNameOf(cTagK.runtimeClass))
      .addOrOverwrite(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ClassUtils.classNameOf(cTagV.runtimeClass))

    newProducer(_builder.buildAsImmutableProperties, _confBuilder.build())
  }

  final def addConnectionConfiguration(conf: KafkaConnectionConfiguration): this.type =
    addBootstrapServers(conf.bootstrapServers)

  final def connectionConfiguration(conf: KafkaConnectionConfiguration): this.type =
    bootstrapServers(conf.bootstrapServers)

  final def addBootstrapServer(host: String, port: Int): this.type = addBootstrapServer(HostPort(host, port))

  final def addBootstrapServer(server: HostPort): this.type =
    rawBootstrapServers(_bootstrapServers :+ server.formatted)

  final def addBootstrapServers(servers: Iterable[HostPort]): this.type =
    rawBootstrapServers(_bootstrapServers ++ servers.map(_.formatted))

  def bootstrapServers(rawServers: Iterable[HostPort]): this.type =
    rawBootstrapServers(rawServers.map(_.formatted).toVector)

  /** (Re)sets [[_bootstrapServers]] with the given servers
    *
    * @param rawServers the bootstrap servers in host:port format
    */
  private[this] def rawBootstrapServers(rawServers: Iterable[String]): this.type = {
    _bootstrapServers = rawServers.toVector
    this
  }

  final def clientId(id: String): this.type = addStringProperty(ProducerConfig.CLIENT_ID_CONFIG, id)

  final def requestTimeoutMs(to: Duration): this.type =
    addDurationAsIntProperty(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, to)

  final def maxBlockMs(d: Duration): this.type = addDurationAsIntProperty(ProducerConfig.MAX_BLOCK_MS_CONFIG, d)

  final def retryBackoffMs(d: Duration): this.type =
    addDurationAsIntProperty(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, d)

  /** Adds all [[ProducerConfig]] from the given instance of [[ImmutableProperties]].
    * Note that if there exists a property with the same name in this instance, it will get overwritten.
    *
    * @param props an instance of [[ImmutableProperties]] to add
    */
  final def addProducerProperties(props: ImmutableProperties): this.type = {
    val filteredProps =
      props.stringValueOfOrNone(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG).map(commaSplit) match {
        case Some(servers) =>
          println(s"servers: ${servers.mkString(" ")}")
          rawBootstrapServers(servers)
          props.filterKeys(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG != _)
        case _ => props.properties
      }
    _builder ++= filteredProps
    this
  }

  final def addStringProperty(key: String, value: String): this.type = {
    require(StringUtils.isNotBlank(value), s"The specified value for the key $key is blank!")

    _builder += (key, value)
    this
  }

  final def addStringPropertyIfNotBlank(key: String, value: String): this.type = {
    if (StringUtils.isNotBlank(value)) {
      _builder += (key, value)
    }
    this
  }

  final def addDurationAsIntProperty(key: String, value: Duration): this.type =
    addIntProperty(key, value.toMillis.toInt)

  final def addIntProperty(key: String, value: Int): this.type = {
    _builder += (key, value)
    this
  }

  final def addConf(f: ProducerConfiguration.Builder => ProducerConfiguration.Builder): this.type = {
    _confBuilder = f(_confBuilder)
    this
  }
}

private[this] object ProducerBuilder {

  private[this] val CommaSplitRegex: Regex = ",".r

  private def commaSplit(s: String): Seq[String] = CommaSplitRegex.split(s)
}
