package com.eventsourcing.notifier.dto

case class BalanceNotification(accountId: String, currentValue: Double, notificationType: NotificationType, balanceThreshold: Double)
