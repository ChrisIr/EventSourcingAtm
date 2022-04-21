package com.eventsourcing.common

import com.eventsourcing.common.config.BaseConfiguration
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}

import java.util.Properties

trait BaseApp[T <: BaseConfiguration] extends App with LazyLogging {

  val configuration = loadConfiguration
  val props: Properties = {
    val properties = new Properties()
    properties.put(StreamsConfig.APPLICATION_ID_CONFIG, configuration.applicationName)
    properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, configuration.bootstrapServer)
    properties
  }
  val streams = buildStreams

  def loadConfiguration: T

  def buildStreams: KafkaStreams

  logger.info(s"Starting app: ${configuration.applicationName}")
  streams.start()

  Runtime.getRuntime.addShutdownHook(new Thread(s"${configuration.applicationName}-shutdown-hook") {
    override def run(): Unit = {
      logger.info(s"Shutting down app: ${configuration.applicationName}")
      streams.close()
    }
  })

}
