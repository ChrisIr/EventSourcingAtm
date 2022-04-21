package com.eventsourcing.atm.dto.result

case class OperationResult(operationId: String, status: OperationResultType)
