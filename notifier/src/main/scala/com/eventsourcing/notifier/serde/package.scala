package com.eventsourcing.notifier

import com.eventsourcing.common.serde.foundation.GenericJsonSerde
import com.eventsourcing.notifier.dto.{BalanceNotification, NotificationType}
import julienrf.json.derived
import julienrf.json.derived.NameAdapter
import play.api.libs.json.{Json, OFormat}

package object serde {
  implicit val NotificationTypeFormat: OFormat[NotificationType] = derived.oformat[NotificationType](NameAdapter.identity)
  implicit val BalanceNotificationFormat: OFormat[BalanceNotification] = Json.format[BalanceNotification]

  implicit val BalanceNotificationSerde: GenericJsonSerde[BalanceNotification] = GenericJsonSerde[BalanceNotification]()
}
