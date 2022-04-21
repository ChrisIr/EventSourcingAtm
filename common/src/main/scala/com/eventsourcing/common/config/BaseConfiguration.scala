package com.eventsourcing.common.config

trait BaseConfiguration {
  def applicationName: String

  def balanceEventsTopic: String

  def bootstrapServer: String
}
