package sample.model

import play.api.libs.json.Json

object PaymentLifecycle {
    type IdempotentIdentifier = String

    sealed trait Correlated {
      def id: IdempotentIdentifier
    }

    sealed trait Command extends Correlated {
      def amount: BigDecimal
    }

    case class Authorize  (id: IdempotentIdentifier, amount: BigDecimal) extends Command
    case class Settle     (id: IdempotentIdentifier, amount: BigDecimal) extends Command
    case class Refund     (id: IdempotentIdentifier, amount: BigDecimal) extends Command
    case class Chargeback (id: IdempotentIdentifier, amount: BigDecimal) extends Command

    sealed trait Balance extends Correlated {
      def amount: BigDecimal
    }

    case class Authorization (id: IdempotentIdentifier, amount: BigDecimal) extends Balance
    case class Settlement    (id: IdempotentIdentifier, amount: BigDecimal) extends Balance
    case class Refunded      (id: IdempotentIdentifier, amount: BigDecimal) extends Balance
    case class Chargedback   (id: IdempotentIdentifier, amount: BigDecimal) extends Balance


    sealed trait Event extends Correlated
    sealed trait FailedEvent[T <: Command] extends Event
    sealed trait SuccessfulEvent[T <: Balance] extends Event {
        def balance: T
    }
    sealed trait RaceConditionEvent[T <: Correlated] extends Event

    case class State(history: List[Event])

    sealed trait Requested[T <: Command]  extends Event           { def command: T ; val id = command.id }
    sealed trait Successful[T <: Balance] extends SuccessfulEvent[T] { def balance: T ; val id = balance.id }
    sealed trait Failed[T <: Command]     extends FailedEvent[T]     { def message: String}
    sealed trait Timedout[T <: Command]   extends FailedEvent[T]

    case class AuthorizationRequested (command: Authorize) extends Requested[Authorize]
    case class AuthorizationSuccessful(balance: Authorization) extends Successful[Authorization]
    case class AuthorizationFailed    (id: IdempotentIdentifier, message: String) extends Failed[Authorize]
    case class AuthorizationTimedout  (id: IdempotentIdentifier, message: String) extends Timedout[Authorize]

    case class SettlementRequested (command: Settle) extends Requested[Settle]
    case class SettlementSuccessful(balance: Settlement) extends Successful[Settlement]
    case class SettlementFailed    (id: IdempotentIdentifier, message: String) extends Failed[Settle]
    case class SettlementTimeout   (id: IdempotentIdentifier, message: String) extends Timedout[Settle]

    case class RefundRequested (command: Refund) extends Requested[Refund]
    case class RefundSuccessful(balance: Refunded) extends Successful[Refunded]
    case class RefundFailed    (id: IdempotentIdentifier, message: String) extends Failed[Refund]
    case class RefundTimeout   (id: IdempotentIdentifier, message: String) extends Timedout[Refund]

    case class ChargebackRequested (command: Chargeback) extends Requested[Chargeback]
    case class ChargebackSuccessful(balance: Chargedback) extends Successful[Chargedback]
    case class ChargebackFailed    (id: IdempotentIdentifier, message: String) extends Failed[Chargeback]
    case class ChargebackTimeout   (id: IdempotentIdentifier, message: String) extends Timedout[Chargeback]

    implicit val authorizeReads = Json.reads[Authorize]
    implicit val settleReads = Json.reads[Settle]
    implicit val refundReads = Json.reads[Refund]
    implicit val chargebackReads = Json.reads[Chargeback]
    implicit val commandReads =  Json.reads[Command]


}