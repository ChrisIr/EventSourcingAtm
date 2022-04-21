package com.eventsourcing.atm.dto.result

sealed trait OperationResultType

case object SuccessfulOperation extends OperationResultType

case object FailedOperation extends OperationResultType