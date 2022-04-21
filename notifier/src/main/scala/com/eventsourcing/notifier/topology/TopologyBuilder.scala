package com.eventsourcing.notifier.topology

import com.eventsourcing.common.model.BalanceChangeEvent
import com.eventsourcing.common.serde._
import com.eventsourcing.notifier.configuration.NotifierConfiguration
import com.eventsourcing.notifier.serde._
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.serialization.Serdes.stringSerde
import org.apache.kafka.streams.state.Stores

object TopologyBuilder extends LazyLogging {
  val BalanceChangeStore = "FinalBalanceEvents"

  def buildTopology(configuration: NotifierConfiguration): Topology = {
    val balanceStateStoreBuilder = Stores.keyValueStoreBuilder(
      Stores.persistentKeyValueStore(BalanceChangeStore),
      Serdes.String(),
      Serdes.Double()
    )
    val builder = new StreamsBuilder(new StreamsBuilder().addStateStore(balanceStateStoreBuilder))

    builder
      .stream[String, BalanceChangeEvent](configuration.balanceEventsTopic)
      .flatTransformValues(BalanceNotificationTransformerSupplier(configuration), BalanceChangeStore)
      .to(configuration.notificationsTopic)

    val topology = builder.build()
    logger.info(s"Topology: ${topology.describe().toString}")

    topology
  }
}
