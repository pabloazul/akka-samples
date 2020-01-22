package sample.persistence.multidc

import akka.actor.ActorSystem
import akka.cluster.sharding.ShardRegion
import akka.persistence.multidc.{PersistenceMultiDcSettings, SpeculativeReplicatedEvent}
import akka.persistence.multidc.scaladsl.ReplicatedEntity
import sample.model.PaymentLifecycle.{Authorization, AuthorizationFailed, AuthorizationTimeout, ChargebackFailed, ChargebackTimeout, Chargedback, Command, Event, IdempotentIdentifier, RefundFailed, RefundTimeout, Refunded, Settlement, SettlementFailed, SettlementTimeout, SimulatedIntegrationResult, State, Successful}

import scala.reflect.ClassTag

object PaymentLifecycleTracking {

  val shardingName = "PaymentLifecycleTracking"
  val maxNumberOfShards = 11

  def shardId(entityId: String): String =
    math.abs(entityId.hashCode % maxNumberOfShards).toString

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: Command => (cmd.id, cmd)
    case evt: SpeculativeReplicatedEvent => (evt.entityId, evt)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: Command => shardId(cmd.id)
    case ShardRegion.StartEntity(entityId) => shardId(entityId)
  }

  def props(system: ActorSystem) = ReplicatedEntity.clusterShardingProps(
    entityTypeName = shardingName,
    entityFactory = () => new PaymentLifecycleTracking,
    settings = PersistenceMultiDcSettings(system))

}

object IntegrationSimulator {
  // Simulates communicating with a downstream system to process commands

  def authorizationChoices(id: IdempotentIdentifier, amount: BigDecimal): List[Event] =
    ((8, AuthorizationFailed(id, "Declined by issuer...")) ::
      (2, AuthorizationTimeout(id, "Communication error...")) ::
      (90, Successful(Authorization(id, amount))) :: Nil).flatMap { v => List.fill(v._1)(v._2) }

  def settleChoices(id: IdempotentIdentifier, amount: BigDecimal): List[Event] =
    ((8, SettlementFailed(id, "Unable to process settle...")) ::
      (2, SettlementTimeout(id, "Communication error...")) ::
      (90, Successful(Settlement(id, amount))) :: Nil).flatMap { v => List.fill(v._1)(v._2) }

  def chargebackChoices(id: IdempotentIdentifier, amount: BigDecimal): List[Event] =
    ((8, ChargebackFailed(id, "Unable to process chargeback...")) ::
      (2, ChargebackTimeout(id, "Communication error...")) ::
      (90, Successful(Chargedback(id, amount))) :: Nil).flatMap { v => List.fill(v._1)(v._2) }

  def refundChoices(id: IdempotentIdentifier, amount: BigDecimal): List[Event] =
    ((8, RefundFailed(id, "Unable to process refund...")) ::
      (2, RefundTimeout(id, "Communication error...")) ::
      (90, Successful(Refunded(id, amount))) :: Nil).flatMap { v => List.fill(v._1)(v._2) }

  def integrationRequest(id: IdempotentIdentifier, choices: List[Event]): Command =
    SimulatedIntegrationResult(id, scala.util.Random.shuffle(choices).head)

}

class PaymentLifecycleTracking
  extends ReplicatedEntity[Command, Event, State] {

  import sample.model.PaymentLifecycle._
  import scala.concurrent.duration._

  override def initialState: State = State(List.empty)

  def transactionState(events: List[Event]): TransactionState = {

    val defaultCases : PartialFunction[Event, Option[TransactionState]] = {
      case AuthorizationFailed(_, _) =>
        Some(Declined)
      case Requested(o) =>
        Some(Pending(o))
      case Successful(_: Authorization) =>
        Some(Authorized)
      case Successful(_: Settlement) =>
        Some(Authorized)
      case Successful(_: Chargedback) =>
        Some(MerchantChargedBack)
      case Successful(_: Refunded) =>
        Some(Refunded)
      case AuthorizationFailed(_, _) =>
        Some(Declined)
      case fail: Event with Failed =>
        Some(OperationFailed(fail))
      case timeout: Event with Timeout =>
        Some(OperationTimedOut(timeout))
    }

    if (events.isEmpty)
      Started
    else {
      events.foldRight(None: Option[TransactionState]) { (event, derivedState) =>
        if (derivedState.contains(Declined) || derivedState.contains(MerchantChargedBack)
          || derivedState.contains(Refunded)) derivedState // put a few terminal states in
        else {
          if (defaultCases isDefinedAt event)
            defaultCases(event)
          else {
            event match {
              case Rejected(_, _, last) =>
                last match {
                  case None => Some(Started)
                  case Some(lastEvent) if defaultCases isDefinedAt lastEvent => defaultCases(lastEvent)
                  case Some(lastEvent) =>
                    log.error(s"Unable to match starting event state: $lastEvent")
                    derivedState
                }
              case unknown =>
                log.warning(s"Unhandled event: $unknown")
                derivedState
            }
          }
        }
      }.get
    }
  }

  def validate(state: State, command: Command): Option[String] = {
    // minimal business logic example
    val derivedState = transactionState(state.history)

    log.info(s"checking command $command for derived state $derivedState")

    derivedState match {
      case Started =>
        command match {
          case _: Authorize => None
          case _ => Some("No operations possible until authorization is requested")
        }

      case Pending(_: Authorize) =>
        command match {
          case _: AuthorizationFailed => None
          case _: AuthorizationTimeout => None
          case _: Successful => None
          case _ => Some("Invalid operation for a pending authorization")
        }

      case Pending(_: Settle) =>
        command match {
          case _: Successful => None
          case _: Chargedback => None
          case _: SettlementTimeout => None
          case _: SettlementFailed => None
          case _: Refund => None
          case _ => Some("Invalid operation for a pending settlement")
        }
      case Authorized =>
        command match {

          case Settle(_, amount, _) =>
            val authorizedAmount = getBalances[Authorization](state.history).map(_.amount).sum
            val settledAmount = getBalances[Settlement](state.history).map(_.amount).sum
            val availableBalance = authorizedAmount - (settledAmount + amount)
            log.info(s"Balances: authorized=$authorizedAmount, settled=$settledAmount, available=$availableBalance")
            if (availableBalance >= 0)
              None
            else
              Some("Settle request exceeds authorization")

          case _ => None // left as an exercise to handle other cases
        }

      case Declined =>
        Some("No operations possible for declined transactions")

      case _ => None
      // TODO: Left as an exercise to add other business rules for different states and commands
    }

  }

  override def commandHandler: CommandHandler = CommandHandler { (ctx, state, command) =>
    command match {

      case sir: SimulatedIntegrationResult =>

        log.info("Integration result: " + sir + " prev state: " + transactionState(state.history))
        Effect.persist(sir.effect).andThen { newState =>
          // Integration results would likely entail other effects being triggered here
          log.info("Processed integration result: " + sir + " new state: " + transactionState(newState.history))
        }

      case tx: GetTransactionState =>
        tx.replyTo ! TransactionStateReply(tx.id, transactionState(state.history).toString, state.history)
        Effect.none

      case setBalanceCommand: SetBalance =>

        val validationResult = validate(state, setBalanceCommand)

        val events = Requested(setBalanceCommand) ::
          (if(validationResult.nonEmpty) Rejected(setBalanceCommand.id, validationResult.get, state.history.headOption) :: Nil
          else Nil)

        Effect.persist( events).andThen { _ =>
          setBalanceCommand match {

            case a: Authorize =>

              validationResult match {
                case None =>
                  ctx.timers.startSingleTimer(s"Authorization: ${a.id}",
                    IntegrationSimulator.integrationRequest(a.id, IntegrationSimulator.authorizationChoices(a.id, a.amount)),
                    5000.milliseconds)
                  a.replyTo ! SetBalanceResponse(a.id, a.amount, ReceivedSuccessfully(s"Authorization for id: ${a.id}, amount: ${a.amount}"))
                case Some(error) =>
                  a.replyTo ! SetBalanceResponse(a.id, a.amount, BalanceCommandError(s"id: ${a.id}, amount: ${a.amount}, authorization error: $error"))
              }


            case c: Chargeback =>

              validationResult match {
                case None =>
                  ctx.timers.startSingleTimer(s"Chargeback: ${c.id}",
                    IntegrationSimulator.integrationRequest(c.id, IntegrationSimulator.chargebackChoices(c.id, c.amount)),
                    5000.milliseconds)
                  c.replyTo ! SetBalanceResponse(c.id, c.amount, ReceivedSuccessfully(s"Chargeback for id: ${c.id}, amount: ${c.amount}"))
                case Some(error) =>
                  c.replyTo ! SetBalanceResponse(c.id, c.amount, BalanceCommandError(s"id: ${c.id}, amount: ${c.amount}, chargeback error: $error"))
              }

            case s: Settle =>

              validationResult match {
                case None =>
                  ctx.timers.startSingleTimer(s"Settlement: ${s.id}",
                    IntegrationSimulator.integrationRequest(s.id, IntegrationSimulator.settleChoices(s.id, s.amount)),
                    5000.milliseconds)
                  s.replyTo ! SetBalanceResponse(s.id, s.amount, ReceivedSuccessfully(s"Settle for id: ${s.id}, amount: ${s.amount}"))
                case Some(error) =>
                  val message = s"id: ${s.id}, amount: ${s.amount}, settlement error: $error"
                  s.replyTo ! SetBalanceResponse(s.id, s.amount, BalanceCommandError(message))
              }

            case r: Refund =>

              validationResult match {
                case None =>
                  ctx.timers.startSingleTimer(s"Authorization: ${r.id}",
                    IntegrationSimulator.integrationRequest(r.id, IntegrationSimulator.refundChoices(r.id, r.amount)),
                    5000.milliseconds)
                  r.replyTo ! SetBalanceResponse(r.id, r.amount, ReceivedSuccessfully(s"Refund for id: ${r.id}, amount: ${r.amount}"))
                case Some(error) =>
                  r.replyTo ! SetBalanceResponse(r.id, r.amount, BalanceCommandError(s"id: ${r.id}, amount: ${r.amount}, refund error: $error"))
              }
          }
        }

      case getBalanceCommand: GetBalance => getBalanceCommand match {

        case g: GetAuthorizationBalance =>
          g.replyTo ! BalanceResponse(getBalanceEvents[Authorization](state.history))
          Effect.none

        case g: GetSettledBalance =>
          g.replyTo ! BalanceResponse(getBalanceEvents[Settlement](state.history))
          Effect.none

        case g: GetRefundedBalance =>
          g.replyTo ! BalanceResponse(getBalanceEvents[Refunded](state.history))
          Effect.none

        case g: GetChargebackBalance =>
          g.replyTo ! BalanceResponse(getBalanceEvents[Chargedback](state.history))
          Effect.none
      }
    }
  }


  def getBalanceEvents[T <: Balance](events: List[Event])(implicit tag: ClassTag[T]): List[Event] =
    events.filter(isBalance[T])

  def getBalances[T <: Balance](events: List[Event])(implicit tag: ClassTag[T]): List[Balance] =
    getBalanceEvents[T](events).collect { case s: Successful => s.balance }

  override def eventHandler(state: State, event: Event): State = {
    State(event :: state.history)
  }

  def isBalance[T <: Balance](event: Event)(implicit tag: ClassTag[T]): Boolean = event match {
    case Successful(balance) => balance match {
      case _: T => true
      case _ => false
    }
    case _ => false
  }

}
