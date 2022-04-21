package com.eventsourcing.atm.dto.operation

case class AccountOperation(accountId: String, operationId: String, operationAmount: Double) extends BaseOperation
