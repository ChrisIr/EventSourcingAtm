package com.eventsourcing.notifier

import com.eventsourcing.common.model.BalanceChangeEvent
import com.eventsourcing.common.serde._
import com.eventsourcing.notifier.configuration.{AboveThreshold, AmountNotification, BelowEqualThreshold, NotifierConfiguration}
import com.eventsourcing.notifier.dto.{BalanceAboveThreshold, BalanceBelowEqualThreshold, BalanceNotification}
import com.eventsourcing.notifier.serde._
import com.eventsourcing.notifier.topology.TopologyBuilder
import org.apache.kafka.common.serialization.Serdes

import scala.util.Try
import org.apache.kafka.streams._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util.Properties

class TopologyBuilderSpec extends AnyWordSpec with Matchers {

  val configuration: NotifierConfiguration = NotifierConfiguration(
    applicationName = "",
    balanceEventsTopic = "balance-events",
    bootstrapServer = "",
    notificationsTopic = "notifications",
    notificationRules = List(
      AmountNotification(notificationType = BelowEqualThreshold, applicableFrom = 50),
      AmountNotification(notificationType = BelowEqualThreshold, applicableFrom = 100),
      AmountNotification(notificationType = AboveThreshold, applicableFrom = 150),
      AmountNotification(notificationType = AboveThreshold, applicableFrom = 200)
    )
  )
  val topology: Topology = TopologyBuilder.buildTopology(configuration)
  val properties: Properties = {
    val properties = new Properties()
    properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "NotifierApp_Topology_Test")
    properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "test_servers:33")

    properties
  }

  "Topology" should {
    "not send notification as amount is above lower and below higher threshold" in {
      val testDriver = new TopologyTestDriver(topology, properties)

      val balanceChanges: TestInputTopic[String, BalanceChangeEvent] = testDriver.createInputTopic[String, BalanceChangeEvent](configuration.balanceEventsTopic, Serdes.String.serializer(), BalanceChangeEventSerde.serializer())
      val notifications: TestOutputTopic[String, BalanceNotification] = testDriver.createOutputTopic[String, BalanceNotification](configuration.notificationsTopic, Serdes.String.deserializer(), BalanceNotificationSerde.deserializer())

      balanceChanges.pipeInput(
        "1234",
        BalanceChangeEvent(accountId = "1234", changeAmount = 120),
      )

      val notification = Try(notifications.readRecord())
      notification.isFailure shouldBe true

      testDriver.close()
    }

    "send notification as amount is below threshold" in {
      val testDriver = new TopologyTestDriver(topology, properties)

      val balanceChanges: TestInputTopic[String, BalanceChangeEvent] = testDriver.createInputTopic[String, BalanceChangeEvent](configuration.balanceEventsTopic, Serdes.String.serializer(), BalanceChangeEventSerde.serializer())
      val notifications: TestOutputTopic[String, BalanceNotification] = testDriver.createOutputTopic[String, BalanceNotification](configuration.notificationsTopic, Serdes.String.deserializer(), BalanceNotificationSerde.deserializer())

      balanceChanges.pipeInput(
        "1234",
        BalanceChangeEvent(accountId = "1234", changeAmount = 20),
      )

      val notification = notifications.readRecord()
      notification.value shouldBe BalanceNotification(accountId = "1234", currentValue = 20, notificationType = BalanceBelowEqualThreshold, balanceThreshold = 50)

      testDriver.close()
    }

    "send notification as amount is above threshold" in {
      val testDriver = new TopologyTestDriver(topology, properties)

      val balanceChanges: TestInputTopic[String, BalanceChangeEvent] = testDriver.createInputTopic[String, BalanceChangeEvent](configuration.balanceEventsTopic, Serdes.String.serializer(), BalanceChangeEventSerde.serializer())
      val notifications: TestOutputTopic[String, BalanceNotification] = testDriver.createOutputTopic[String, BalanceNotification](configuration.notificationsTopic, Serdes.String.deserializer(), BalanceNotificationSerde.deserializer())

      balanceChanges.pipeInput(
        "1234",
        BalanceChangeEvent(accountId = "1234", changeAmount = 220),
      )

      val notification = notifications.readRecord()
      notification.value shouldBe BalanceNotification(accountId = "1234", currentValue = 220, notificationType = BalanceAboveThreshold, balanceThreshold = 200)

      testDriver.close()
    }

    "send notification as amount is between two thresholds" in {
      val testDriver = new TopologyTestDriver(topology, properties)

      val balanceChanges: TestInputTopic[String, BalanceChangeEvent] = testDriver.createInputTopic[String, BalanceChangeEvent](configuration.balanceEventsTopic, Serdes.String.serializer(), BalanceChangeEventSerde.serializer())
      val notifications: TestOutputTopic[String, BalanceNotification] = testDriver.createOutputTopic[String, BalanceNotification](configuration.notificationsTopic, Serdes.String.deserializer(), BalanceNotificationSerde.deserializer())

      balanceChanges.pipeInput(
        "1234",
        BalanceChangeEvent(accountId = "1234", changeAmount = 170),
      )

      val notification = notifications.readRecord()
      notification.value shouldBe BalanceNotification(accountId = "1234", currentValue = 170, notificationType = BalanceAboveThreshold, balanceThreshold = 150)

      testDriver.close()
    }
  }
}
