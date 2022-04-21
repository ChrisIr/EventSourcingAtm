package com.eventsourcing.notifier

import com.eventsourcing.common.BaseApp
import com.eventsourcing.notifier.configuration.NotifierConfiguration
import com.eventsourcing.notifier.topology.TopologyBuilder
import org.apache.kafka.streams.KafkaStreams
import pureconfig.ConfigSource
import pureconfig.generic.auto._

object NotifierApp extends BaseApp[NotifierConfiguration] {
  override def loadConfiguration = ConfigSource.default
    .at("notifier-configuration")
    .load[NotifierConfiguration].getOrElse(null)

  override def buildStreams: KafkaStreams = {
    val topology = TopologyBuilder.buildTopology(configuration)
    new KafkaStreams(topology, props)
  }
}
