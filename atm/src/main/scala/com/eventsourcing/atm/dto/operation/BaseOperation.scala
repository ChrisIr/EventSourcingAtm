package com.eventsourcing.atm.dto.operation

trait BaseOperation {
  def operationId: String

  def accountId: String

  def operationAmount: Double

  def operationDescription: String = s"Operation with requestId: $operationId, accountId: $accountId and operation amount: $operationAmount"
}
