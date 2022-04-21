package com.eventsourcing.common.serde.foundation

import org.apache.kafka.common.serialization.Deserializer
import play.api.libs.json.{Json, Reads}

case class GenericJsonDeserializer[T >: Null]()(implicit fjs: Reads[T]) extends Deserializer[T] {
  override def deserialize(topic: String, data: Array[Byte]): T = {
    val g = Option(data)
      .map(new String(_))
      .map(Json.parse(_).as[T])
      .orNull
    g
  }
}
