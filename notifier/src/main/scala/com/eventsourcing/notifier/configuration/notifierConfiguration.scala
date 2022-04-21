package com.eventsourcing.notifier.configuration

import com.eventsourcing.common.config.BaseConfiguration
import pureconfig._
import pureconfig.generic.semiauto._

sealed trait ConfigNotificationType

case object BelowEqualThreshold extends ConfigNotificationType

case object AboveThreshold extends ConfigNotificationType

object ConfigNotificationType {
  implicit val notificationTypeConvert: ConfigReader[ConfigNotificationType] = deriveEnumerationReader[ConfigNotificationType]
}

case class AmountNotification(notificationType: ConfigNotificationType, applicableFrom: Double)

case class NotifierConfiguration(
                                  applicationName: String,
                                  balanceEventsTopic: String,
                                  bootstrapServer: String,
                                  notificationsTopic: String,
                                  notificationRules: List[AmountNotification]
                                ) extends BaseConfiguration
