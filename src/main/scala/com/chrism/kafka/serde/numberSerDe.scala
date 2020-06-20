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
package com.chrism.kafka.serde

import java.{lang => jl}

import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import org.apache.kafka.common.{serialization => kser}

sealed abstract class NumberSerializer[SNum <: AnyVal, JNum <: jl.Number](
  protected val underlying: kser.Serializer[JNum])
    extends Serializer[SNum] {

  override final def serialize(topic: String, data: SNum): Array[Byte] =
    underlying.serialize(topic, data.asInstanceOf[JNum])
}

sealed abstract class NumberDeserializer[SNum <: AnyVal, JNum <: jl.Number](
  protected val underlying: kser.Deserializer[JNum])
    extends Deserializer[SNum] {

  override final def deserialize(topic: String, data: Array[Byte]): SNum =
    underlying.deserialize(topic, data).asInstanceOf[SNum]
}

final class ShortSerializer private () extends NumberSerializer[Short, jl.Short](new kser.ShortSerializer())

object ShortSerializer {

  def apply(): ShortSerializer = new ShortSerializer()
}

final class ShortDeserializer private () extends NumberDeserializer[Short, jl.Short](new kser.ShortDeserializer())

object ShortDeserializer {

  def apply(): ShortDeserializer = new ShortDeserializer()
}

final class IntSerializer private () extends NumberSerializer[Int, jl.Integer](new kser.IntegerSerializer())

object IntSerializer {

  def apply(): IntSerializer = new IntSerializer()
}

final class IntDeserializer private () extends NumberDeserializer[Int, jl.Integer](new kser.IntegerDeserializer())

object IntDeserializer {

  def apply(): IntDeserializer = new IntDeserializer()
}

final class LongSerializer private () extends NumberSerializer[Long, jl.Long](new kser.LongSerializer())

object LongSerializer {

  def apply(): LongSerializer = new LongSerializer()
}

final class LongDeserializer private () extends NumberDeserializer[Long, jl.Long](new kser.LongDeserializer())

object LongDeserializer {

  def apply(): LongDeserializer = new LongDeserializer()
}

final class FloatSerializer private () extends NumberSerializer[Float, jl.Float](new kser.FloatSerializer())

object FloatSerializer {

  def apply(): FloatSerializer = new FloatSerializer()
}

final class FloatDeserializer private () extends NumberDeserializer[Float, jl.Float](new kser.FloatDeserializer())

object FloatDeserializer {

  def apply(): FloatDeserializer = new FloatDeserializer()
}

final class DoubleSerializer private () extends NumberSerializer[Double, jl.Double](new kser.DoubleSerializer())

object DoubleSerializer {

  def apply(): DoubleSerializer = new DoubleSerializer()
}

final class DoubleDeserializer private () extends NumberDeserializer[Double, jl.Double](new kser.DoubleDeserializer())

object DoubleDeserializer {

  def apply(): DoubleDeserializer = new DoubleDeserializer()
}
