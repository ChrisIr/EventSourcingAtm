package com.eventsourcing.common.serde.foundation

import org.apache.kafka.common.serialization.Serializer
import play.api.libs.json.{Json, Writes}

case class GenericJsonSerializer[T >: Null]()(implicit fjs: Writes[T]) extends Serializer[T] {
  override def serialize(topic: String, data: T): Array[Byte] = {
    Option(data)
      .map(Json.toJson[T])
      .map(_.toString().getBytes)
      .orNull
  }
}
