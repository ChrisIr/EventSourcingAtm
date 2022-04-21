package com.eventsourcing.atm

import com.eventsourcing.atm.configuration.AtmConfiguration
import com.eventsourcing.atm.topology.TopologyBuilder
import com.eventsourcing.common.BaseApp
import org.apache.kafka.streams.KafkaStreams
import pureconfig.ConfigSource
import pureconfig.generic.auto._

object AtmApp extends BaseApp[AtmConfiguration] {
  override def loadConfiguration = ConfigSource.default
    .at("atm-configuration")
    .load[AtmConfiguration].getOrElse(null)

  override def buildStreams = {
    val topology = TopologyBuilder.buildTopology(configuration)
    new KafkaStreams(topology, props)
  }
}
