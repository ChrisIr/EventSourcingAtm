package com.eventsourcing.atm.configuration

import com.eventsourcing.common.config.BaseConfiguration

case class Operations(accountOperationsTopic: String, balanceOperationsTopic: String, operationsResultTopic: String)

case class AtmConfiguration(applicationName: String, balanceEventsTopic: String, bootstrapServer: String, operations: Operations) extends BaseConfiguration
