package payments.model

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
  case class Refund        (id: IdempotentIdentifier, amount: BigDecimal) extends Balance
  case class Chargeback    (id: IdempotentIdentifier, amount: BigDecimal) extends Balance


  sealed trait Event extends Correlated
  sealed trait FailedEvent extends Event
  sealed trait SuccessfulEvent extends Event
  sealed trait RaceConditionEvent extends Event

  case class State(history:List[Event])


  case class AuthorizationRequested (id: IdempotentIdentifier, amount: BigDecimal) extends Event
  case class AuthorizationSuccessful(id: IdempotentIdentifier, authorization: Authorization) extends SuccessfulEvent
  case class AuthorizationFailed    (id: IdempotentIdentifier, message: String) extends FailedEvent
  case class AuthorizationTimeout   (id: IdempotentIdentifier, message: String) extends FailedEvent

  case class SettlementRequested (id: IdempotentIdentifier, amount: BigDecimal) extends Event
  case class SettlementSuccessful(id: IdempotentIdentifier, settlement: Settlement)  extends SuccessfulEvent
  case class SettlementFailed    (id: IdempotentIdentifier, message: String) extends FailedEvent
  case class SettlementTimeout   (id: IdempotentIdentifier, message: String) extends FailedEvent

  case class RefundRequested (id: IdempotentIdentifier, amount: BigDecimal) extends Event
  case class RefundSuccessful(id: IdempotentIdentifier, refund: Refund)  extends SuccessfulEvent
  case class RefundFailed    (id: IdempotentIdentifier, message: String) extends FailedEvent
  case class RefundTimeout   (id: IdempotentIdentifier, message: String) extends FailedEvent

  case class ChargebackRequested (id: IdempotentIdentifier, amount: BigDecimal) extends Event
  case class ChargebackSuccessful(id: IdempotentIdentifier, chargeback: Chargeback)  extends SuccessfulEvent
  case class ChargebackFailed    (id: IdempotentIdentifier, message: String) extends FailedEvent
  case class ChargebackTimeout   (id: IdempotentIdentifier, message: String) extends FailedEvent
