package com.eventsourcing.atm.topology

import com.eventsourcing.atm.dto.operation.{BaseOperation, VerifiedOperation}
import com.eventsourcing.atm.topology.TopologyBuilder._
import com.eventsourcing.atm.topology.VerificationProcessor.OperationVerifier
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.streams.processor.api.{Processor, ProcessorContext, ProcessorSupplier, Record}
import org.apache.kafka.streams.state.KeyValueStore

import java.time.Instant

case class VerificationProcessor[T <: BaseOperation](verifier: OperationVerifier[T]) extends Processor[String, T, String, VerifiedOperation[T]] with LazyLogging {

  private var context: ProcessorContext[String, VerifiedOperation[T]] = _
  private var accountStore: KeyValueStore[String, Double] = _

  override def init(context: ProcessorContext[String, VerifiedOperation[T]]): Unit = {
    this.context = context
    accountStore = this.context.getStateStore(BalanceStore)
  }

  override def process(record: Record[String, T]): Unit = {
    logger.debug(s"Verification of operation: ${record.value().operationDescription} started.")

    val verifiedOperation = verifier.verifyOperation(record.key(), record.value(), accountStore)
    val forwardRecord = new Record(record.key(), verifiedOperation, Instant.now().toEpochMilli)
    verifiedOperation match {
      case VerifiedOperation(isCorrect, _) if isCorrect =>
        logger.debug("Verification succeeded.")
        context.forward(forwardRecord)
      case VerifiedOperation(isCorrect, _) if !isCorrect =>
        logger.debug("Verification failed.")
        context.forward(forwardRecord, ResultProcessorNode)
      case _ => logger.error(s"Received operation: ${record.value().operationDescription} cannot be validated!")
    }
  }
}

object VerificationProcessor {
  type VerificationFunc[T <: BaseOperation] = (T, KeyValueStore[String, Double]) => Boolean

  trait OperationVerifier[T <: BaseOperation] {
    protected val additionalVerifications: Seq[VerificationFunc[T]]

    def verifyOperation(key: String, operation: T, store: KeyValueStore[String, Double]): VerifiedOperation[T] = {
      val baseVerification = key == operation.accountId
      val additionalVerificationsReduced = additionalVerifications.map(func => func(operation, store)).reduce(_ && _)
      VerifiedOperation(isCorrect = baseVerification && additionalVerificationsReduced, operation)
    }

    protected def operationAmountIsPositive(operation: T, store: KeyValueStore[String, Double]): Boolean = operation.operationAmount >= 0
  }
}

case class VerificationProcessorSupplier[T <: BaseOperation](verifier: OperationVerifier[T]) extends ProcessorSupplier[String, T, String, VerifiedOperation[T]] {
  override def get(): Processor[String, T, String, VerifiedOperation[T]] = VerificationProcessor(verifier)
}

