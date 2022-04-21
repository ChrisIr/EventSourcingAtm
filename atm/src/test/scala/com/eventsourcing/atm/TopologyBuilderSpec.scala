package com.eventsourcing.atm

import com.eventsourcing.atm.configuration.{AtmConfiguration, Operations}
import com.eventsourcing.atm.dto.operation.{AccountOperation, BalanceOperation, Debit, Withdrawal}
import com.eventsourcing.atm.dto.result.{FailedOperation, OperationResult, SuccessfulOperation}
import com.eventsourcing.atm.serde._
import com.eventsourcing.atm.topology.TopologyBuilder
import com.eventsourcing.common.model.BalanceChangeEvent
import com.eventsourcing.common.serde._
import org.apache.kafka.common.serialization.Serdes

import scala.jdk.CollectionConverters._
import org.apache.kafka.streams._
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.test.TestRecord
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.Properties

class TopologyBuilderSpec extends AnyWordSpec with Matchers {

  val configuration: AtmConfiguration = AtmConfiguration(
    applicationName = "",
    balanceEventsTopic = "balance-events",
    bootstrapServer = "",
    operations = Operations(
      accountOperationsTopic = "account-operations",
      balanceOperationsTopic = "balance-operations",
      operationsResultTopic = "operations-result"
    )
  )
  val topology: Topology = TopologyBuilder.buildTopology(configuration)
  val properties: Properties = {
    val properties = new Properties()
    properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "AtmApp_Topology_Test")
    properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "test_servers:33")

    properties
  }

  "Topology" should {
    "reject account operation as amount is below zero" in {
      val testDriver = new TopologyTestDriver(topology, properties)

      val accountsOps: TestInputTopic[String, AccountOperation] = testDriver.createInputTopic[String, AccountOperation](configuration.operations.accountOperationsTopic, Serdes.String.serializer(), AccountOperationSerde.serializer())
      val results: TestOutputTopic[String, OperationResult] = testDriver.createOutputTopic[String, OperationResult](configuration.operations.operationsResultTopic, Serdes.String.deserializer(), OperationResultSerde.deserializer())

      accountsOps.pipeInput(
        "1234",
        AccountOperation(accountId = "1234", operationId = "0", operationAmount = -10),
      )

      val operationResult = results.readRecord()
      operationResult.value() shouldBe OperationResult(operationId = "0", status = FailedOperation)

      testDriver.close()
    }


    "reject balance operation as account does not exists" in {
      val testDriver = new TopologyTestDriver(topology, properties)

      val balancesOps: TestInputTopic[String, BalanceOperation] = testDriver.createInputTopic[String, BalanceOperation](configuration.operations.balanceOperationsTopic, Serdes.String.serializer(), BalanceOperationSerde.serializer())
      val results: TestOutputTopic[String, OperationResult] = testDriver.createOutputTopic[String, OperationResult](configuration.operations.operationsResultTopic, Serdes.String.deserializer(), OperationResultSerde.deserializer())

      balancesOps.pipeInput("1234", BalanceOperation(accountId = "1234", operationId = "0", operationType = Debit, operationAmount = 1.23))

      val operationResult = results.readRecord()
      operationResult.value() shouldBe OperationResult(operationId = "0", status = FailedOperation)

      testDriver.close()
    }

    "accept account and Debit balance operations where account operation is first and creates an account" in {
      val testDriver = new TopologyTestDriver(topology, properties)

      val accountsOps: TestInputTopic[String, AccountOperation] = testDriver.createInputTopic[String, AccountOperation](configuration.operations.accountOperationsTopic, Serdes.String.serializer(), AccountOperationSerde.serializer())
      val balancesOps: TestInputTopic[String, BalanceOperation] = testDriver.createInputTopic[String, BalanceOperation](configuration.operations.balanceOperationsTopic, Serdes.String.serializer(), BalanceOperationSerde.serializer())
      val balanceChanges: TestOutputTopic[String, BalanceChangeEvent] = testDriver.createOutputTopic[String, BalanceChangeEvent](configuration.balanceEventsTopic, Serdes.String.deserializer(), BalanceChangeEventSerde.deserializer())
      val results: TestOutputTopic[String, OperationResult] = testDriver.createOutputTopic[String, OperationResult](configuration.operations.operationsResultTopic, Serdes.String.deserializer(), OperationResultSerde.deserializer())

      accountsOps.pipeInput(
        "1234",
        AccountOperation(accountId = "1234", operationId = "0", operationAmount = 100)
      )
      balancesOps.pipeInput(
        "1234",
        BalanceOperation(accountId = "1234", operationId = "1", operationType = Debit, operationAmount = 1.23)
      )
      val operationsResult = results.readValuesToList()
      operationsResult.size() shouldBe 2
      operationsResult.get(0) shouldBe OperationResult(operationId = "0", status = SuccessfulOperation)
      operationsResult.get(1) shouldBe OperationResult(operationId = "1", status = SuccessfulOperation)

      val changes = balanceChanges.readValuesToList()
      changes.size() shouldBe 2
      changes.get(0) shouldBe BalanceChangeEvent(accountId = "1234", changeAmount = 100)
      changes.get(1) shouldBe BalanceChangeEvent(accountId = "1234", changeAmount = 1.23)

      testDriver.close()
    }

    "reject Withdrawal balance operation where balance would be below zero" in {
      val testDriver = new TopologyTestDriver(topology, properties)

      val accountsOps: TestInputTopic[String, AccountOperation] = testDriver.createInputTopic[String, AccountOperation](configuration.operations.accountOperationsTopic, Serdes.String.serializer(), AccountOperationSerde.serializer())
      val balancesOps: TestInputTopic[String, BalanceOperation] = testDriver.createInputTopic[String, BalanceOperation](configuration.operations.balanceOperationsTopic, Serdes.String.serializer(), BalanceOperationSerde.serializer())
      val balanceChanges: TestOutputTopic[String, BalanceChangeEvent] = testDriver.createOutputTopic[String, BalanceChangeEvent](configuration.balanceEventsTopic, Serdes.String.deserializer(), BalanceChangeEventSerde.deserializer())
      val results: TestOutputTopic[String, OperationResult] = testDriver.createOutputTopic[String, OperationResult](configuration.operations.operationsResultTopic, Serdes.String.deserializer(), OperationResultSerde.deserializer())

      accountsOps.pipeInput(
        "1234",
        AccountOperation(accountId = "1234", operationId = "0", operationAmount = 100)
      )
      balancesOps.pipeInput(
        "1234",
        BalanceOperation(accountId = "1234", operationId = "1", operationType = Withdrawal, operationAmount = 120)
      )
      val operationsResult = results.readValuesToList()
      operationsResult.size() shouldBe 2
      operationsResult.get(0) shouldBe OperationResult(operationId = "0", status = SuccessfulOperation)
      operationsResult.get(1) shouldBe OperationResult(operationId = "1", status = FailedOperation)

      val changes = balanceChanges.readValuesToList()
      changes.size() shouldBe 1
      changes.get(0) shouldBe BalanceChangeEvent(accountId = "1234", changeAmount = 100)

      testDriver.close()
    }

    "accept Withdrawal balance operation where balance is above zero" in {
      val testDriver = new TopologyTestDriver(topology, properties)

      val accountsOps: TestInputTopic[String, AccountOperation] = testDriver.createInputTopic[String, AccountOperation](configuration.operations.accountOperationsTopic, Serdes.String.serializer(), AccountOperationSerde.serializer())
      val balancesOps: TestInputTopic[String, BalanceOperation] = testDriver.createInputTopic[String, BalanceOperation](configuration.operations.balanceOperationsTopic, Serdes.String.serializer(), BalanceOperationSerde.serializer())
      val balanceChanges: TestOutputTopic[String, BalanceChangeEvent] = testDriver.createOutputTopic[String, BalanceChangeEvent](configuration.balanceEventsTopic, Serdes.String.deserializer(), BalanceChangeEventSerde.deserializer())
      val results: TestOutputTopic[String, OperationResult] = testDriver.createOutputTopic[String, OperationResult](configuration.operations.operationsResultTopic, Serdes.String.deserializer(), OperationResultSerde.deserializer())

      accountsOps.pipeInput(
        "1234",
        AccountOperation(accountId = "1234", operationId = "0", operationAmount = 100)
      )
      balancesOps.pipeInput(
        "1234",
        BalanceOperation(accountId = "1234", operationId = "1", operationType = Withdrawal, operationAmount = 95)
      )
      val operationsResult = results.readValuesToList()
      operationsResult.size() shouldBe 2
      operationsResult.get(0) shouldBe OperationResult(operationId = "0", status = SuccessfulOperation)
      operationsResult.get(1) shouldBe OperationResult(operationId = "1", status = SuccessfulOperation)

      val changes = balanceChanges.readValuesToList()
      changes.size() shouldBe 2
      changes.get(0) shouldBe BalanceChangeEvent(accountId = "1234", changeAmount = 100)
      changes.get(1) shouldBe BalanceChangeEvent(accountId = "1234", changeAmount = -95)

      testDriver.close()
    }

    "maintain order of operations" in {
      val testDriver = new TopologyTestDriver(topology, properties)

      val accountsOps: TestInputTopic[String, AccountOperation] = testDriver.createInputTopic[String, AccountOperation](configuration.operations.accountOperationsTopic, Serdes.String.serializer(), AccountOperationSerde.serializer())
      val balancesOps: TestInputTopic[String, BalanceOperation] = testDriver.createInputTopic[String, BalanceOperation](configuration.operations.balanceOperationsTopic, Serdes.String.serializer(), BalanceOperationSerde.serializer())
      val balanceChanges: TestOutputTopic[String, BalanceChangeEvent] = testDriver.createOutputTopic[String, BalanceChangeEvent](configuration.balanceEventsTopic, Serdes.String.deserializer(), BalanceChangeEventSerde.deserializer())

      accountsOps.pipeInput(
        "1234",
        AccountOperation(accountId = "1234", operationId = "0", operationAmount = 100)
      )

      balancesOps.pipeRecordList(Seq(
        new TestRecord("1234", BalanceOperation(accountId = "1234", operationId = "1", operationType = Debit, operationAmount = 95)),
        new TestRecord("1234", BalanceOperation(accountId = "1234", operationId = "2", operationType = Withdrawal, operationAmount = 20)),
        new TestRecord("1234", BalanceOperation(accountId = "1234", operationId = "3", operationType = Withdrawal, operationAmount = 100)),
        new TestRecord("1234", BalanceOperation(accountId = "1234", operationId = "3", operationType = Withdrawal, operationAmount = 17)),
      ).asJava)

      val changes = balanceChanges.readValuesToList()
      changes.size() shouldBe 5
      changes.get(0) shouldBe BalanceChangeEvent(accountId = "1234", changeAmount = 100)
      changes.get(1) shouldBe BalanceChangeEvent(accountId = "1234", changeAmount = 95)
      changes.get(2) shouldBe BalanceChangeEvent(accountId = "1234", changeAmount = -20)
      changes.get(3) shouldBe BalanceChangeEvent(accountId = "1234", changeAmount = -100)
      changes.get(4) shouldBe BalanceChangeEvent(accountId = "1234", changeAmount = -17)

      val balanceStateStore: KeyValueStore[String, Double] = testDriver.getKeyValueStore(TopologyBuilder.BalanceStore)
      val accountBalance = balanceStateStore.get("1234")
      accountBalance shouldBe 58

      testDriver.close()
    }
  }
}
