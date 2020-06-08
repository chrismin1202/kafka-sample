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
object Dependencies {

  import sbt._

  private val ZooKeeperGroupId: String = "org.apache.zookeeper"
  private val KafkaGroupId: String = "org.apache.kafka"
  private val JacksonGroupId: String = "com.fasterxml.jackson.core"
  private val Slf4jGroupId: String = "org.slf4j"

  private val ZooKeeperArtifactId: String = "zookeeper"
  private val JacksonDatabindArtifactId: String = "jackson-databind"
  private val Slf4jApiArtifactId: String = "slf4j-api"

  private val CirceVersion = "0.13.0"
  private val ZooKeeperVersion: String = "3.5.7"
  private val KafkaVersion: String = "2.5.0"

  private val ScalacheckVersion: String = "1.14.0"
  private val ScalatestVersion: String = "3.0.8"
  private val Specs2CoreVersion: String = "4.7.0"

  val Commons4s: ModuleID = "com.chrism" %% "commons4s" % "0.0.8"
  val Java8Compat: ModuleID = "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.1"
  val LogbackClassic: ModuleID = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val Log4s: ModuleID = "org.log4s" %% "log4s" % "1.8.2"

  val CirceCore: ModuleID = "io.circe" %% "circe-core" % CirceVersion
  val CirceGeneric: ModuleID = "io.circe" %% "circe-generic" % CirceVersion
  val CirceParser: ModuleID = "io.circe" %% "circe-parser" % CirceVersion
  val CirceGenericExtras: ModuleID = "io.circe" %% "circe-generic-extras" % CirceVersion

  val ZooKeeper: ModuleID = (ZooKeeperGroupId % ZooKeeperArtifactId % ZooKeeperVersion)
    .exclude("log4j", "log4j")
    .exclude(Slf4jGroupId, Slf4jApiArtifactId)
    .exclude(Slf4jGroupId, "slf4j-log4j12")

  val KafkaClients: ModuleID = (KafkaGroupId % "kafka-clients" % KafkaVersion)
    .exclude(JacksonGroupId, JacksonDatabindArtifactId)
    .exclude(Slf4jGroupId, Slf4jApiArtifactId)

  val Kafka: ModuleID = (KafkaGroupId %% "kafka" % KafkaVersion)
    .exclude(ZooKeeperGroupId, ZooKeeperArtifactId)
    .exclude(JacksonGroupId, JacksonDatabindArtifactId)
    .exclude(Slf4jGroupId, Slf4jApiArtifactId)
    .exclude("com.typesafe.scala-logging", "scala-logging")

  val Scalacheck: ModuleID = "org.scalacheck" %% "scalacheck" % ScalacheckVersion
  val Scalatest: ModuleID = "org.scalatest" %% "scalatest" % ScalatestVersion
  val Specs2Core: ModuleID = "org.specs2" %% "specs2-core" % Specs2CoreVersion
}
