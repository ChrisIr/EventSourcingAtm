package com.eventsourcing.atm.dto.operation

case class VerifiedOperation[T <: BaseOperation](isCorrect: Boolean, operation: T)
