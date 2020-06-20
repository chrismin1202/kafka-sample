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

import com.chrism.commons.FunTestSuite
import com.chrism.commons.util.WithResource
import com.chrism.log.LoggingLike
import com.chrism.net.{HostPort, LocalHostPort, PortDetector}
import com.chrism.util.{ImmutableProperties, PropertiesBuilder, RandomUtils}
import kafka.server.{KafkaConfig, KafkaServer}
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.zookeeper.server.{ServerCnxnFactory, ZooKeeperServer}

import scala.reflect.io.Directory

trait KafkaTestSuiteLike {
  this: FunTestSuite =>

  import KafkaTestSuiteLike._

  protected def brokerId(): Int = RandomUtils.randomPositiveInt(upperBound = 999)

  protected final def withRunningKafka[A](
    kafkaPort: Int = PortDetector.findFreePort(),
    zooKeeperPort: Int = PortDetector.findFreePort(),
    customKafkaProperties: Map[String, String] = Map.empty
  )(
    f: KafkaConnectionConfiguration => A
  ): A = {
    require(
      kafkaPort != zooKeeperPort,
      s"The Kafka port ($kafkaPort) must be different from the ZooKeeper port ($zooKeeperPort)!")

    withRunningZooKeeper(zooKeeperPort) { zk =>
      assert(
        zk.hostPort.port === zooKeeperPort,
        s"The ZooKeeper ports do not match: $zooKeeperPort (specified) vs. ${zk.hostPort.port} (actual)")

      val kafkaListener = LocalHostPort(kafkaPort)
      val zkConnect = LocalHostPort(zooKeeperPort)
      val kafkaLogDir = createKafkaLogDir()
      val props = KafkaTestSuiteLike.buildKafkaProperties(
        kafkaListener,
        zkConnect,
        brokerId(),
        kafkaLogDir,
        customProperties = customKafkaProperties)

      WithResource(RunningKafka(props, kafkaLogDir)) { kafka =>
        kafka.start()
        f(KafkaConnectionConfiguration.builder.addBootstrapServer(kafkaListener).build())
      }
    }
  }

  private[this] def withRunningZooKeeper[A](port: Int)(f: RunningZooKeeper => A): A =
    WithResource(newRunningZooKeeper(port)) { zk =>
      zk.start()
      f(zk)
    }
}

private[this] object KafkaTestSuiteLike {

  import scala.concurrent.duration._

  private[this] val DefaultZkConnectionTimeout: Int = 10.seconds.toMillis.toInt
  private[this] val DefaultAutoCreateTopics: Boolean = true
  private[this] val DefaultOffsetsTopicReplicationFactor: Short = 1
  private[this] val DefaultTransactionsTopicReplicationFactor: Short = 1
  private[this] val DefaultLogCleanerDedupeBufferSize: Int = 1048577

  private[this] val KafkaPrefix: String = "zookeeper-"
  private[this] val ZooKeeperPrefix: String = "zookeeper-"
  private[this] val LogDirPrefix: String = "log-"
  private[this] val SnapDirPrefix: String = "snap-"

  private def buildKafkaProperties(
    kafkaListener: HostPort,
    zkConnect: HostPort,
    brokerId: Int,
    kafkaLogDir: Directory,
    customProperties: Map[String, String] = Map.empty
  ): ImmutableProperties = {
    if (customProperties.nonEmpty) {
      val names = KafkaConfig.configNames().toSet
      val customPropKeys = customProperties.keySet
      require(
        customPropKeys.subsetOf(KafkaConfig.configNames().toSet),
        "One or more of the specified names [" + customPropKeys.mkString(", ") +
          "] is/are not one of the KafkaConfig names in [" + names.mkString(", ") + "]"
      )
    }

    val listener = s"${SecurityProtocol.PLAINTEXT}://$kafkaListener"

    val builder = PropertiesBuilder() +=
      (KafkaConfig.ZkConnectProp -> zkConnect.formatted) +=
      (KafkaConfig.ZkConnectionTimeoutMsProp -> DefaultZkConnectionTimeout) +=
      (KafkaConfig.BrokerIdProp -> brokerId) +=
      (KafkaConfig.ListenersProp -> listener) +=
      (KafkaConfig.AdvertisedListenersProp -> listener) +=
      (KafkaConfig.AutoCreateTopicsEnableProp -> DefaultAutoCreateTopics) +=
      (KafkaConfig.LogDirProp -> kafkaLogDir.toAbsolute.path) +=
      (KafkaConfig.LogFlushIntervalMessagesProp -> 1) +=
      (KafkaConfig.OffsetsTopicReplicationFactorProp -> DefaultOffsetsTopicReplicationFactor) +=
      (KafkaConfig.OffsetsTopicPartitionsProp -> 1) +=
      (KafkaConfig.TransactionsTopicReplicationFactorProp -> DefaultTransactionsTopicReplicationFactor) +=
      (KafkaConfig.TransactionsTopicMinISRProp -> 1) +=
      (KafkaConfig.LogCleanerDedupeBufferSizeProp -> DefaultLogCleanerDedupeBufferSize) ++=
      customProperties

    builder.buildAsImmutableProperties
  }

  private def newRunningZooKeeper(port: Int): RunningZooKeeper =
    RunningZooKeeper(LocalHostPort(port), createLogDir(ZooKeeperPrefix), createZooKeeperSnapDir())

  private def createKafkaLogDir(): Directory = createLogDir(KafkaPrefix)

  private[this] def createLogDir(prefix: String): Directory = makeTempDir(suffixName(prefix + LogDirPrefix))

  private[this] def createZooKeeperSnapDir(): Directory = makeTempDir(suffixName(SnapDirPrefix))

  private[this] def makeTempDir(name: String): Directory = Directory.makeTemp(prefix = name)

  private[this] def suffixName(name: String): String = name + RandomUtils.randomPositiveLong()
}

private[this] final class RunningKafka private (props: ImmutableProperties, logDir: Directory) extends LoggingLike {

  private[this] var server: KafkaServer = _

  private[kafka] def start(): Unit =
    if (server == null) {
      logger.info("Starting Kafka...")
      server = new KafkaServer(KafkaConfig.fromProps(props.toJProperties))
      server.startup()
      logger.info("Successfully started Kafka...")
    } else {
      logger.info("Kafka has already been started!")
    }

  def close(): Unit =
    if (server != null) {
      logger.info("Shutting down Kafka...")
      server.shutdown()
      server.awaitShutdown()
      server = null
      logger.info("Successfully shut down Kafka.")

      logger.info(s"Deleting the Kafka log directory $logDir...")
      if (logDir.deleteRecursively()) {
        logger.info("Deleted the Kafka log directory successfully.")
      } else {
        logger.warn(s"Failed to delete the Kafka log directory $logDir!")
      }
    } else {
      logger.info("The Kafka connection has either not started yet or already been closed!")
    }
}

private[this] object RunningKafka {

  def apply(props: ImmutableProperties, logDir: Directory): RunningKafka = new RunningKafka(props, logDir)
}

private[this] final class RunningZooKeeper private (val hostPort: HostPort, logDir: Directory, snapDir: Directory)
    extends LoggingLike {

  import RunningZooKeeper._

  private[this] var factory: ServerCnxnFactory = _

  private[kafka] def start(): Unit =
    if (factory == null) {
      logger.info(s"Starting ZooKeeper (host:port=$hostPort, logDir=$logDir, snapDir=$snapDir)")
      val zk = new ZooKeeperServer(logDir.toFile.jfile, snapDir.toFile.jfile, DefaultTickTime)
      factory = ServerCnxnFactory.createFactory(hostPort.toInetSocketAddress, DefaultMaxClientCnxns)
      factory.startup(zk)
      logger.info("Successfully started ZooKeeper...")
    } else {
      logger.info("ZooKeeper has already been started!")
    }

  def close(): Unit =
    if (factory != null) {
      logger.info("Shutting down ZooKeeper...")
      factory.shutdown()
      factory = null
      logger.info("Successfully shut down ZooKeeper.")

      logger.info(s"Deleting the ZooKeeper log directory $logDir...")
      if (logDir.deleteRecursively()) {
        logger.info("Deleted the ZooKeeper log directory successfully.")
      } else {
        logger.warn(s"Failed to delete the ZooKeeper log directory $logDir!")
      }

      logger.info(s"Deleting the ZooKeeper snapshot directory $snapDir...")
      if (snapDir.deleteRecursively()) {
        logger.info("Deleted the ZooKeeper snapshot directory successfully.")
      } else {
        logger.warn(s"Failed to delete the ZooKeeper snapshot directory $snapDir!")
      }
    } else {
      logger.info("The ZooKeeper connection has either not started yet or already been closed!")
    }
}

private[this] object RunningZooKeeper {

  private val DefaultTickTime: Int = 2000
  private val DefaultMaxClientCnxns: Int = 1024

  private[kafka] def apply(hostPort: HostPort, logDir: Directory, snapDir: Directory): RunningZooKeeper =
    new RunningZooKeeper(hostPort, logDir, snapDir)
}
