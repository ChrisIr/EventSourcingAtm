package com.eventsourcing.notifier.topology

import com.eventsourcing.common.model.BalanceChangeEvent
import com.eventsourcing.notifier.configuration.{AboveThreshold, BelowEqualThreshold, NotifierConfiguration}
import com.eventsourcing.notifier.dto.{BalanceAboveThreshold, BalanceBelowEqualThreshold, BalanceNotification, NotificationType}
import com.eventsourcing.notifier.topology.BalanceNotificationTransformer.{BalanceNotificationRule, buildRules}
import com.eventsourcing.notifier.topology.TopologyBuilder.BalanceChangeStore
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.streams.kstream.{ValueTransformer, ValueTransformerSupplier}
import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.kafka.streams.state.KeyValueStore

case class BalanceNotificationTransformer(configuration: NotifierConfiguration) extends ValueTransformer[BalanceChangeEvent, Iterable[BalanceNotification]] with LazyLogging {

  private var balanceStore: KeyValueStore[String, Double] = _
  private var balanceNotificationsRules: Seq[BalanceNotificationRule] = _

  override def init(context: ProcessorContext): Unit = {
    balanceStore = context.getStateStore(BalanceChangeStore)
    balanceNotificationsRules = buildRules(configuration)
  }

  override def transform(changeEvent: BalanceChangeEvent): Iterable[BalanceNotification] = {

    val currentAmount: Double = Option(balanceStore.get(changeEvent.accountId)).getOrElse(0)
    val updatedAmount: Double = currentAmount + changeEvent.changeAmount

    balanceStore.put(changeEvent.accountId, updatedAmount)

    balanceNotificationsRules
      .filter(_.isApplicable(updatedAmount))
      .groupBy(_.notificationType)
      .map { case (notificationType: NotificationType, rules: Seq[BalanceNotificationRule]) =>
        notificationType match {
          case BalanceBelowEqualThreshold => rules.minBy(_.thresholdValue)
          case BalanceAboveThreshold => rules.maxBy(_.thresholdValue)
        }
      }
      .map(rule => BalanceNotification(changeEvent.accountId, updatedAmount, rule.notificationType, rule.thresholdValue))
  }

  override def close(): Unit = ()
}

object BalanceNotificationTransformer {
  def buildRules(configuration: NotifierConfiguration): Seq[BalanceNotificationRule] = {
    configuration.notificationRules.map { rule =>
      new BalanceNotificationRule {
        override val thresholdValue: Double = rule.applicableFrom
        override val notificationType: NotificationType = {
          rule.notificationType match {
            case BelowEqualThreshold => BalanceBelowEqualThreshold
            case AboveThreshold => BalanceAboveThreshold
          }
        }
      }
    }
  }

  trait BalanceNotificationRule {
    val thresholdValue: Double

    def notificationType: NotificationType

    def isApplicable(amount: Double): Boolean = {
      notificationType match {
        case BalanceBelowEqualThreshold => amount <= thresholdValue
        case BalanceAboveThreshold => amount > thresholdValue
      }
    }
  }
}

case class BalanceNotificationTransformerSupplier(configuration: NotifierConfiguration) extends ValueTransformerSupplier[BalanceChangeEvent, Iterable[BalanceNotification]] {
  override def get(): ValueTransformer[BalanceChangeEvent, Iterable[BalanceNotification]] = BalanceNotificationTransformer(configuration)
}

