package com.eventsourcing.common

import com.eventsourcing.common.model.BalanceChangeEvent
import com.eventsourcing.common.serde.foundation.GenericJsonSerde
import play.api.libs.json.{Json, OFormat}

package object serde {
  implicit val BalanceChangeEventFormat: OFormat[BalanceChangeEvent] = Json.format[BalanceChangeEvent]

  implicit val BalanceChangeEventSerde: GenericJsonSerde[BalanceChangeEvent] = GenericJsonSerde[BalanceChangeEvent]()
}
