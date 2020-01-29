package sample.play

import play.api.libs.json.Json
import sample.model.PaymentLifecycle.IdempotentIdentifier

object GatewayInterface {

  case class AuthorizeRequest(id: IdempotentIdentifier, amount: BigDecimal)
  case class SettleRequest(id: IdempotentIdentifier, amount: BigDecimal)
  case class RefundRequest(id: IdempotentIdentifier, amount: BigDecimal)
  case class ChargebackRequest(id: IdempotentIdentifier, amount: BigDecimal)

  case class Settle     (id: IdempotentIdentifier, amount: BigDecimal)
  case class Chargeback (id: IdempotentIdentifier, amount: BigDecimal)
  case class Refund     (id: IdempotentIdentifier, amount: BigDecimal)

  implicit val settleReads = Json.reads[Settle]
  implicit val refundReads = Json.reads[Refund]
  implicit val chargebackReads = Json.reads[Chargeback]
  implicit val authorizeRequestReads = Json.reads[AuthorizeRequest]
  implicit val settleRequestReads = Json.reads[SettleRequest]
  implicit val refundRequestReads = Json.reads[RefundRequest]
  implicit val chargebackRequestReads = Json.reads[ChargebackRequest]


  case class GetBalances(id: IdempotentIdentifier)
  implicit val getBalancesReads = Json.reads[GetBalances]

}
