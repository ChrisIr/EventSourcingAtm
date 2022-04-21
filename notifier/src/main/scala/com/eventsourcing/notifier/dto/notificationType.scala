package com.eventsourcing.notifier.dto

sealed trait NotificationType

case object BalanceBelowEqualThreshold extends NotificationType

case object BalanceAboveThreshold extends NotificationType
