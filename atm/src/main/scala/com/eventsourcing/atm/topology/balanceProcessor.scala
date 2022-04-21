package com.eventsourcing.atm.topology

import com.eventsourcing.atm.dto.operation.{BalanceOperation, VerifiedOperation}
import com.eventsourcing.atm.topology.TopologyBuilder._
import com.eventsourcing.atm.topology.VerificationProcessor.{OperationVerifier, VerificationFunc}
import com.eventsourcing.common.model.BalanceChangeEvent
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.streams.processor.api.{Processor, ProcessorContext, ProcessorSupplier, Record}
import org.apache.kafka.streams.state.KeyValueStore

import java.time.Instant

case class BalanceProcessor() extends Processor[String, VerifiedOperation[BalanceOperation], String, BalanceChangeEvent] with LazyLogging {

  private var context: ProcessorContext[String, BalanceChangeEvent] = _
  private var accountStore: KeyValueStore[String, Double] = _

  override def init(context: ProcessorContext[String, BalanceChangeEvent]): Unit = {
    this.context = context
    accountStore = this.context.getStateStore(BalanceStore)
  }

  override def process(record: Record[String, VerifiedOperation[BalanceOperation]]): Unit = {
    if (!record.value().isCorrect) {
      logger.error(s"${record.value().operation.operationDescription} should not reach this processor. Only correct operations can be processed!")
      return
    }

    val operation = record.value().operation

    val currentAmount = accountStore.get(record.key())
    val updatedAmount = operation.applyOperationAmount(currentAmount)
    accountStore.put(record.key(), updatedAmount)

    val balanceChangeEvent = BalanceChangeEvent(
      accountId = operation.accountId,
      changeAmount = operation.operationAmountWithSign()
    )

    context.forward(new Record(record.key(), balanceChangeEvent, Instant.now().toEpochMilli))
  }
}

object BalanceProcessor {
  val balanceVerifier: OperationVerifier[BalanceOperation] = new OperationVerifier[BalanceOperation] {
    override val additionalVerifications: Seq[VerificationFunc[BalanceOperation]] = Seq(
      accountExists,
      operationAmountIsPositive,
      operationIsAcceptable
    )
  }

  private def accountExists(operation: BalanceOperation, store: KeyValueStore[String, Double]): Boolean = Option(store.get(operation.accountId)).isDefined

  private def operationIsAcceptable(operation: BalanceOperation, store: KeyValueStore[String, Double]): Boolean = operation.applyOperationAmount(store.get(operation.accountId)) >= 0
}

case class BalanceProcessorSupplier() extends ProcessorSupplier[String, VerifiedOperation[BalanceOperation], String, BalanceChangeEvent] {
  override def get(): Processor[String, VerifiedOperation[BalanceOperation], String, BalanceChangeEvent] = BalanceProcessor()
}

