package com.eventsourcing.atm.topology

import com.eventsourcing.atm.dto.operation.{BaseOperation, VerifiedOperation}
import com.eventsourcing.atm.dto.result.{FailedOperation, OperationResult, SuccessfulOperation}
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.streams.processor.api.{Processor, ProcessorContext, ProcessorSupplier, Record}

import java.time.Instant

case class ResultProcessor() extends Processor[String, VerifiedOperation[BaseOperation], String, OperationResult] with LazyLogging {

  private var context: ProcessorContext[String, OperationResult] = _

  override def init(context: ProcessorContext[String, OperationResult]): Unit = {
    this.context = context
  }

  override def process(record: Record[String, VerifiedOperation[BaseOperation]]): Unit = {
    val operationResult = {
      val verifiedOperation = record.value()
      OperationResult(
        operationId = verifiedOperation.operation.operationId,
        status = if (verifiedOperation.isCorrect) SuccessfulOperation else FailedOperation
      )
    }
    logger.debug(s"Sending result: ${operationResult.status} for operation: ${record.value().operation.operationDescription}")
    context.forward(new Record(record.key(), operationResult, Instant.now().toEpochMilli))
  }
}

case class ResultProcessorSupplier() extends ProcessorSupplier[String, VerifiedOperation[BaseOperation], String, OperationResult] {
  override def get(): Processor[String, VerifiedOperation[BaseOperation], String, OperationResult] = ResultProcessor()
}

