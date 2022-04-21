package com.eventsourcing.atm.dto.operation

case class BalanceOperation(accountId: String, operationId: String, operationType: BalanceOperationType, operationAmount: Double) extends BaseOperation {
  def applyOperationAmount(currentValue: Double): Double = {
    onTypeResolution(
      onDebit = () => currentValue + operationAmount,
      onWithdrawal = () => currentValue - operationAmount
    )
  }

  def operationAmountWithSign(): Double = {
    onTypeResolution(
      onDebit = () => operationAmount,
      onWithdrawal = () => operationAmount * -1
    )
  }

  private def onTypeResolution[T](onDebit: () => T, onWithdrawal: () => T): T = {
    operationType match {
      case Debit => onDebit()
      case Withdrawal => onWithdrawal()
    }
  }
}
