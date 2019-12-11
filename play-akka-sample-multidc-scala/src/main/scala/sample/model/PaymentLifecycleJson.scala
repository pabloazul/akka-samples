package sample.model

import play.api.libs.json.Json
import sample.model.PaymentLifecycle.{Authorize, Chargeback, Command, Refund, Settle}

object PaymentLifecycleJson {
  implicit val authorizeReads = Json.reads[Authorize]
  implicit val settleReads = Json.reads[Settle]
  implicit val refundReads = Json.reads[Refund]
  implicit val chargebackReads = Json.reads[Chargeback]
  implicit val commandReads =  Json.reads[Command]
}
