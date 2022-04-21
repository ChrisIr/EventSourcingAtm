package com.eventsourcing.atm.dto.operation

sealed trait BalanceOperationType

case object Withdrawal extends BalanceOperationType

case object Debit extends BalanceOperationType

