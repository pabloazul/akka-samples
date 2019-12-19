package sample.model

import akka.actor.typed.ActorRef

object PaymentLifecycle {
    type IdempotentIdentifier = String

    trait Correlated { def id: IdempotentIdentifier }

    sealed trait Command extends Correlated

    sealed trait SetBalance extends Command { def amount: BigDecimal }
    case class Authorize  (id: IdempotentIdentifier, amount: BigDecimal, replyTo: ActorRef[SetBalanceResponse]) extends SetBalance
    case class Settle     (id: IdempotentIdentifier, amount: BigDecimal) extends SetBalance
    case class Refund     (id: IdempotentIdentifier, amount: BigDecimal) extends SetBalance
    case class Chargeback (id: IdempotentIdentifier, amount: BigDecimal) extends SetBalance

    sealed trait BalanceStatus
    case object ReceivedSuccessfully extends BalanceStatus
    case class BalanceCommandError(message: String) extends BalanceStatus

    case class SetBalanceResponse(id: IdempotentIdentifier, amount: BigDecimal, status: BalanceStatus)

    sealed trait GetBalance extends Command
    case class GetAuthorizationBalance(id: IdempotentIdentifier, replyTo: ActorRef[BalanceResponse]) extends GetBalance
    case class GetSettledBalance      (id: IdempotentIdentifier, replyTo: ActorRef[BalanceResponse]) extends GetBalance
    case class GetRefundedBalance     (id: IdempotentIdentifier, replyTo: ActorRef[BalanceResponse]) extends GetBalance
    case class GetChargebackBalance   (id: IdempotentIdentifier, replyTo: ActorRef[BalanceResponse]) extends GetBalance

    case class BalanceResponse(balanceEvents: List[Event])

    sealed trait Balance extends Correlated { def amount: BigDecimal }

    case class Authorization (id: IdempotentIdentifier, amount: BigDecimal) extends Balance
    case class Settlement    (id: IdempotentIdentifier, amount: BigDecimal) extends Balance
    case class Refunded      (id: IdempotentIdentifier, amount: BigDecimal) extends Balance
    case class Chargedback   (id: IdempotentIdentifier, amount: BigDecimal) extends Balance


    sealed trait Event extends Correlated

    sealed trait RaceConditionEvent[T <: Correlated] extends Event

    case class State(history: List[Event])

    case class Requested(command: SetBalance) extends Event { val id : IdempotentIdentifier = command.id }
    case class Successful(balance: Balance)   extends Event { val id : IdempotentIdentifier = balance.id }

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