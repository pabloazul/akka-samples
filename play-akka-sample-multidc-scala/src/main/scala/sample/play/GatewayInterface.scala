package sample.play

import play.api.libs.json.Json
import sample.model.PaymentLifecycle.IdempotentIdentifier

object GatewayInterface {

  case class AuthorizeRequest(id: IdempotentIdentifier, amount: BigDecimal)
  case class Settle     (id: IdempotentIdentifier, amount: BigDecimal)
  case class Chargeback (id: IdempotentIdentifier, amount: BigDecimal)
  case class Refund     (id: IdempotentIdentifier, amount: BigDecimal)

  implicit val settleReads = Json.reads[Settle]
  implicit val refundReads = Json.reads[Refund]
  implicit val chargebackReads = Json.reads[Chargeback]
  implicit val authorizeRequestReads = Json.reads[AuthorizeRequest]

}
