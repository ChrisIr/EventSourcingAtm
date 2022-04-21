package com.eventsourcing.common.model

case class BalanceChangeEvent(accountId: String, changeAmount: Double)
