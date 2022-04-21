package com.eventsourcing.common.serde.foundation

import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}
import play.api.libs.json.OFormat

case class GenericJsonSerde[T >: Null]()(implicit format: OFormat[T]) extends Serde[T] {
  override def serializer(): Serializer[T] = GenericJsonSerializer[T]()

  override def deserializer(): Deserializer[T] = GenericJsonDeserializer[T]()
}
