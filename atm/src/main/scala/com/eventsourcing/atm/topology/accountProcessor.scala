package com.eventsourcing.atm.topology

import com.eventsourcing.atm.dto.operation.{AccountOperation, VerifiedOperation}
import com.eventsourcing.atm.topology.TopologyBuilder.BalanceStore
import com.eventsourcing.atm.topology.VerificationProcessor.{OperationVerifier, VerificationFunc}
import com.eventsourcing.common.model.BalanceChangeEvent
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.streams.processor.api.{Processor, ProcessorContext, ProcessorSupplier, Record}
import org.apache.kafka.streams.state.KeyValueStore

import java.time.Instant

case class AccountProcessor() extends Processor[String, VerifiedOperation[AccountOperation], String, BalanceChangeEvent] with LazyLogging {

  private var context: ProcessorContext[String, BalanceChangeEvent] = _
  private var accountStore: KeyValueStore[String, Double] = _

  override def init(context: ProcessorContext[String, BalanceChangeEvent]): Unit = {
    this.context = context
    accountStore = this.context.getStateStore(BalanceStore)
  }

  override def process(record: Record[String, VerifiedOperation[AccountOperation]]): Unit = {
    if (!record.value().isCorrect) {
      logger.error(s"${record.value().operation.operationDescription} should not reach this processor. Only correct operations can be processed!")
      return
    }

    val operation = record.value().operation
    accountStore.put(record.key(), operation.operationAmount)

    val balanceChangeEvent =
      BalanceChangeEvent(
        accountId = operation.accountId,
        changeAmount = operation.operationAmount
      )

    context.forward(new Record(record.key(), balanceChangeEvent, Instant.now().toEpochMilli))
  }

}

object AccountProcessor {
  val accountVerifier: OperationVerifier[AccountOperation] = new OperationVerifier[AccountOperation] {
    override val additionalVerifications: Seq[VerificationFunc[AccountOperation]] = Seq(
      accountDoesNotExists,
      operationAmountIsPositive
    )
  }

  private def accountDoesNotExists(operation: AccountOperation, store: KeyValueStore[String, Double]): Boolean = Option(store.get(operation.accountId)).isEmpty
}

case class AccountProcessorSupplier() extends ProcessorSupplier[String, VerifiedOperation[AccountOperation], String, BalanceChangeEvent] {
  override def get(): Processor[String, VerifiedOperation[AccountOperation], String, BalanceChangeEvent] = AccountProcessor()
}

