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

import com.chrism.kafka.serde.{JsonDeserializer, JsonSerializer}
import io.circe.generic.extras.{semiauto, Configuration}
import io.circe.{Decoder, Encoder}

private[kafka] final case class DummyKafkaRequest(
  stringParam: String,
  nullableStringParam: Option[String],
  intParam: Int,
  nullableIntParam: Option[Int],
  booleanParam: Boolean,
  nullableBooleanParam: Option[Boolean])

private[kafka] object DummyKafkaRequest {

  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults
  implicit val encoder: Encoder[DummyKafkaRequest] = semiauto.deriveConfiguredEncoder
  implicit val decoder: Decoder[DummyKafkaRequest] = semiauto.deriveConfiguredDecoder
}

private[kafka] object DummyKafkaRequestSerializer extends JsonSerializer[DummyKafkaRequest]

private[kafka] object DummyKafkaRequestDeserializer extends JsonDeserializer[DummyKafkaRequest]
