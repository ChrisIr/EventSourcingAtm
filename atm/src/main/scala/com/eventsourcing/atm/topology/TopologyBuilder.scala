package com.eventsourcing.atm.topology

import com.eventsourcing.atm.configuration.AtmConfiguration
import com.eventsourcing.atm.dto.operation.{AccountOperation, BalanceOperation}
import com.eventsourcing.atm.serde._
import com.eventsourcing.atm.topology.AccountProcessor.accountVerifier
import com.eventsourcing.atm.topology.BalanceProcessor.balanceVerifier
import com.eventsourcing.common.serde._
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.state.Stores

object TopologyBuilder extends LazyLogging {

  val AccountOperationsSourceNode = "AccountOperationsSource"
  val BalanceOperationsSourceNode = "BalanceOperationsSource"

  val AccountVerificationProcessorNode = "AccountVerificationProcessor"
  val BalanceVerificationProcessorNode = "BalanceVerificationProcessor"
  val AccountProcessorNode = "AccountProcessor"
  val BalanceProcessorNode = "BalanceProcessor"
  val ResultProcessorNode = "ResultProcessor"

  val BalanceStore = "BalanceStore"

  val BalanceChangeEventSinkNode = "BalanceChangeEventSink"
  val OperationsResultSinkNode = "OperationsResultSink"

  def buildTopology(configuration: AtmConfiguration): Topology = {
    val builder = new Topology()
    val balanceStateStoreBuilder = Stores.keyValueStoreBuilder(
      Stores.inMemoryKeyValueStore(BalanceStore),
      Serdes.String(),
      Serdes.Double()
    )

    val topology = builder
      // Account operations
      .addSource(AccountOperationsSourceNode, Serdes.String.deserializer(), AccountOperationSerde.deserializer(), configuration.operations.accountOperationsTopic)
      .addProcessor(AccountVerificationProcessorNode, VerificationProcessorSupplier[AccountOperation](accountVerifier), AccountOperationsSourceNode)
      .addProcessor(AccountProcessorNode, AccountProcessorSupplier(), AccountVerificationProcessorNode)
      // Balance operations
      .addSource(BalanceOperationsSourceNode, Serdes.String.deserializer(), BalanceOperationSerde.deserializer(), configuration.operations.balanceOperationsTopic)
      .addProcessor(BalanceVerificationProcessorNode, VerificationProcessorSupplier[BalanceOperation](balanceVerifier), BalanceOperationsSourceNode)
      .addProcessor(BalanceProcessorNode, BalanceProcessorSupplier(), BalanceVerificationProcessorNode)
      // Results
      .addProcessor(ResultProcessorNode, ResultProcessorSupplier(), AccountVerificationProcessorNode, BalanceVerificationProcessorNode)
      // State stores
      .addStateStore(balanceStateStoreBuilder, AccountVerificationProcessorNode, AccountProcessorNode, BalanceVerificationProcessorNode, BalanceProcessorNode)
      // Sinks
      .addSink(BalanceChangeEventSinkNode, configuration.balanceEventsTopic, Serdes.String.serializer(), BalanceChangeEventSerde.serializer(), AccountProcessorNode, BalanceProcessorNode)
      .addSink(OperationsResultSinkNode, configuration.operations.operationsResultTopic, Serdes.String.serializer(), OperationResultSerde.serializer(), ResultProcessorNode)

    logger.info(s"Topology: ${builder.describe().toString}")

    topology
  }
}
