package sample.model

import akka.actor.typed.ActorRef
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}

trait PaymentLifecycleSerializable

object PaymentLifecycle {
  type IdempotentIdentifier = String

  trait Correlated {
    def id: IdempotentIdentifier
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes(
    Array(
      new JsonSubTypes.Type(value = classOf[Authorize], name = "Authorize"),
      new JsonSubTypes.Type(value = classOf[Settle], name = "Settle"),
      new JsonSubTypes.Type(value = classOf[Refund], name = "Refund"),
      new JsonSubTypes.Type(value = classOf[Chargeback], name = "Chargeback")
    )
  )
  sealed trait Command extends Correlated with PaymentLifecycleSerializable

  sealed trait SetBalance extends Command {
    def amount: BigDecimal
  }

  case class Authorize(id: IdempotentIdentifier, amount: BigDecimal, replyTo: ActorRef[SetBalanceResponse]) extends SetBalance

  case class Settle(id: IdempotentIdentifier, amount: BigDecimal, replyTo: ActorRef[SetBalanceResponse]) extends SetBalance

  case class Refund(id: IdempotentIdentifier, amount: BigDecimal, replyTo: ActorRef[SetBalanceResponse]) extends SetBalance

  case class Chargeback(id: IdempotentIdentifier, amount: BigDecimal, replyTo: ActorRef[SetBalanceResponse]) extends SetBalance

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  case class SimulatedIntegrationResult(id: IdempotentIdentifier, effect: Event) extends Command

  //TODO Remove : Debug Only
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  case class TransactionStateReply(id: IdempotentIdentifier, state: String, events: List[Event])

  case class GetTransactionState(id: IdempotentIdentifier, replyTo: ActorRef[TransactionStateReply]) extends Command

  //TODO Remove : Debug Only

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes(
    Array(
      new JsonSubTypes.Type(value = classOf[ReceivedSuccessfully], name = "ReceivedSuccessfully"),
      new JsonSubTypes.Type(value = classOf[BalanceCommandError], name = "BalanceCommandError")
    )
  )
  sealed trait BalanceStatus extends PaymentLifecycleSerializable

  case class ReceivedSuccessfully(message: String) extends BalanceStatus

  case class BalanceCommandError(message: String) extends BalanceStatus

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  case class SetBalanceResponse(id: IdempotentIdentifier, amount: BigDecimal, status: BalanceStatus) extends PaymentLifecycleSerializable

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes(
    Array(
      new JsonSubTypes.Type(value = classOf[GetAuthorizationBalance], name = "GetAuthorizationBalance"),
      new JsonSubTypes.Type(value = classOf[GetSettledBalance], name = "GetSettledBalance"),
      new JsonSubTypes.Type(value = classOf[GetRefundedBalance], name = "GetRefundedBalance"),
      new JsonSubTypes.Type(value = classOf[GetChargebackBalance], name = "GetChargebackBalance"),
      new JsonSubTypes.Type(value = classOf[GetTransactionState], name = "GetTransactionState")
    )
  )
  sealed trait GetBalance extends Command

  case class GetAuthorizationBalance(id: IdempotentIdentifier, replyTo: ActorRef[BalanceResponse]) extends GetBalance

  case class GetSettledBalance(id: IdempotentIdentifier, replyTo: ActorRef[BalanceResponse]) extends GetBalance

  case class GetRefundedBalance(id: IdempotentIdentifier, replyTo: ActorRef[BalanceResponse]) extends GetBalance

  case class GetChargebackBalance(id: IdempotentIdentifier, replyTo: ActorRef[BalanceResponse]) extends GetBalance

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  case class BalanceResponse(balanceEvents: List[Event]) extends PaymentLifecycleSerializable

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes(
    Array(
      new JsonSubTypes.Type(value = classOf[Authorization], name = "Authorization"),
      new JsonSubTypes.Type(value = classOf[Settlement], name = "Settlement"),
      new JsonSubTypes.Type(value = classOf[Refunded], name = "Refunded"),
      new JsonSubTypes.Type(value = classOf[Chargedback], name = "Chargedback")
    )
  )
  sealed trait Balance extends PaymentLifecycleSerializable with Correlated {
    def amount: BigDecimal
  }

  case class Authorization(id: IdempotentIdentifier, amount: BigDecimal) extends Balance

  case class Settlement(id: IdempotentIdentifier, amount: BigDecimal) extends Balance

  case class Refunded(id: IdempotentIdentifier, amount: BigDecimal) extends Balance

  case class Chargedback(id: IdempotentIdentifier, amount: BigDecimal) extends Balance


  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes(
    Array(
      new JsonSubTypes.Type(value = classOf[Requested], name = "Requested"),
      new JsonSubTypes.Type(value = classOf[Successful], name = "Successful"),
      new JsonSubTypes.Type(value = classOf[AuthorizationFailed], name = "AuthorizationFailed"),
      new JsonSubTypes.Type(value = classOf[AuthorizationTimeout], name = "AuthorizationTimeout"),
      new JsonSubTypes.Type(value = classOf[SettlementFailed], name = "SettlementFailed"),
      new JsonSubTypes.Type(value = classOf[SettlementTimeout], name = "SettlementTimeout"),
      new JsonSubTypes.Type(value = classOf[RefundFailed], name = "RefundFailed"),
      new JsonSubTypes.Type(value = classOf[RefundTimeout], name = "RefundTimeout"),
      new JsonSubTypes.Type(value = classOf[ChargebackFailed], name = "ChargebackFailed"),
      new JsonSubTypes.Type(value = classOf[ChargebackTimeout], name = "ChargebackTimeout")
    )
  )
  sealed trait Event extends Correlated with PaymentLifecycleSerializable

  sealed trait RaceConditionEvent[T <: Correlated] extends Event

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  case class State(history: List[Event]) extends PaymentLifecycleSerializable

  case class Requested(command: SetBalance) extends Event {
    val id: IdempotentIdentifier = command.id
  }

  case class Successful(balance: Balance) extends Event {
    val id: IdempotentIdentifier = balance.id
  }

  sealed trait Failed extends Event {
    def message: String
  }

  sealed trait Timeout extends Failed

  case class AuthorizationFailed(id: IdempotentIdentifier, message: String) extends Failed

  case class AuthorizationTimeout(id: IdempotentIdentifier, message: String) extends Timeout

  case class SettlementFailed(id: IdempotentIdentifier, message: String) extends Failed

  case class SettlementTimeout(id: IdempotentIdentifier, message: String) extends Timeout

  case class RefundFailed(id: IdempotentIdentifier, message: String) extends Failed

  case class RefundTimeout(id: IdempotentIdentifier, message: String) extends Timeout

  case class ChargebackFailed(id: IdempotentIdentifier, message: String) extends Failed

  case class ChargebackTimeout(id: IdempotentIdentifier, message: String) extends Timeout

  case class Rejected(id: IdempotentIdentifier, message: String, last: Option[Event]) extends Failed

  sealed trait TransactionState

  case object Started extends TransactionState

  case class Pending(command: SetBalance) extends TransactionState

  case object Authorized extends TransactionState

  case object Declined extends TransactionState

  case class OperationFailed(failure: Failed) extends TransactionState

  case class OperationTimedOut(timeout: Timeout) extends TransactionState

  case object Expired extends TransactionState

  case object Refunded extends TransactionState

  case object SettlementCompleted extends TransactionState

  case object MerchantChargedBack extends TransactionState

  case object Complete extends TransactionState

}