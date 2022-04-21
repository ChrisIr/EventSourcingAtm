package com.eventsourcing.atm

import com.eventsourcing.atm.dto.operation.{AccountOperation, BalanceOperation, BalanceOperationType}
import com.eventsourcing.atm.dto.result.{OperationResult, OperationResultType}
import com.eventsourcing.common.serde.foundation.GenericJsonSerde
import julienrf.json.derived
import julienrf.json.derived.NameAdapter
import play.api.libs.json.{Json, OFormat}

package object serde {
  implicit val BalanceOperationTypeFormat: OFormat[BalanceOperationType] = derived.oformat[BalanceOperationType](NameAdapter.identity)
  implicit val BalanceOperationFormat: OFormat[BalanceOperation] = Json.format[BalanceOperation]
  implicit val AccountOperationFormat: OFormat[AccountOperation] = Json.format[AccountOperation]

  implicit val OperationResultTypeFormat: OFormat[OperationResultType] = derived.oformat[OperationResultType](NameAdapter.identity)
  implicit val OperationResultFormat: OFormat[OperationResult] = Json.format[OperationResult]

  implicit val AccountOperationSerde: GenericJsonSerde[AccountOperation] = GenericJsonSerde[AccountOperation]()
  implicit val BalanceOperationSerde: GenericJsonSerde[BalanceOperation] = GenericJsonSerde[BalanceOperation]()
  implicit val OperationResultSerde: GenericJsonSerde[OperationResult] = GenericJsonSerde[OperationResult]()
}
