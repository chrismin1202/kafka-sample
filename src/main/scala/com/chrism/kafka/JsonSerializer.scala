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

import io.circe.{Encoder, Printer}
import org.apache.kafka.common.serialization.Serializer

abstract class JsonSerializer[A: Encoder](printer: Printer = JsonSerializer.DefaultPrinter) extends Serializer[A] {

  override /* overridable */ def configure(configs: ju.Map[String, _], isKey: Boolean): Unit = {
    // stubbed
  }

  override def serialize(topic: String, data: A): Array[Byte] = {
    import io.circe.syntax._

    printer.print(data.asJson).getBytes("UTF-8")
  }

  override /* overridable */ def close(): Unit = {
    // stubbed
  }
}

object JsonSerializer {

  /** The default io.circe.Printer with null values dropped and no indentation */
  val DefaultPrinter: Printer = Printer(true, "")
}
