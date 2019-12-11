package sample.model

object PaymentLifecycle {
    type IdempotentIdentifier = String

    trait Correlated { def id: IdempotentIdentifier }

    sealed trait Command extends Correlated

    sealed trait SetBalance extends Command { def amount: BigDecimal }
    case class Authorize  (id: IdempotentIdentifier, amount: BigDecimal) extends SetBalance
    case class Settle     (id: IdempotentIdentifier, amount: BigDecimal) extends SetBalance
    case class Refund     (id: IdempotentIdentifier, amount: BigDecimal) extends SetBalance
    case class Chargeback (id: IdempotentIdentifier, amount: BigDecimal) extends SetBalance

    sealed trait GetBalance extends Command
    case class GetAuthorizationBalance(id: IdempotentIdentifier) extends GetBalance
    case class GetSettledBalance      (id: IdempotentIdentifier) extends GetBalance
    case class GetRefundedBalance     (id: IdempotentIdentifier) extends GetBalance
    case class GetChargebackBalance   (id: IdempotentIdentifier) extends GetBalance

    sealed trait Balance extends Correlated { def amount: BigDecimal }

    case class Authorization (id: IdempotentIdentifier, amount: BigDecimal) extends Balance
    case class Settlement    (id: IdempotentIdentifier, amount: BigDecimal) extends Balance
    case class Refunded      (id: IdempotentIdentifier, amount: BigDecimal) extends Balance
    case class Chargedback   (id: IdempotentIdentifier, amount: BigDecimal) extends Balance


    sealed trait Event extends Correlated

    sealed trait RaceConditionEvent[T <: Correlated] extends Event

    case class State(history: List[Event])

    case class Requested(command: SetBalance) extends Event { val id = command.id }
    case class Successful(balance: Balance)   extends Event { val id = balance.id }

    sealed trait Failed                   extends Event { def message: String}
    sealed trait Timedout                 extends Failed

    case class AuthorizationFailed    (id: IdempotentIdentifier, message: String) extends Failed
    case class AuthorizationTimedout  (id: IdempotentIdentifier, message: String) extends Timedout

    case class SettlementFailed    (id: IdempotentIdentifier, message: String) extends Failed
    case class SettlementTimeout   (id: IdempotentIdentifier, message: String) extends Timedout

    case class RefundFailed    (id: IdempotentIdentifier, message: String) extends Failed
    case class RefundTimeout   (id: IdempotentIdentifier, message: String) extends Timedout


    case class ChargebackFailed    (id: IdempotentIdentifier, message: String) extends Failed
    case class ChargebackTimeout   (id: IdempotentIdentifier, message: String) extends Timedout

}