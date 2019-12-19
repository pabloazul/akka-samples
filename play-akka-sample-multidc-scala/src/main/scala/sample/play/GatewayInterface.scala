package sample.play

import play.api.libs.json.Json
import sample.model.PaymentLifecycle.IdempotentIdentifier

object GatewayInterface {
  implicit val authorizeRequestReads = Json.reads[AuthorizeRequest]
  case class AuthorizeRequest(id: IdempotentIdentifier, amount: BigDecimal)
}
