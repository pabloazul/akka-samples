package sample.persistence.multidc

import akka.actor.ActorSystem
import akka.cluster.sharding.ShardRegion
import akka.persistence.multidc.{PersistenceMultiDcSettings, SpeculativeReplicatedEvent}
import akka.persistence.multidc.scaladsl.ReplicatedEntity
import sample.model.PaymentLifecycle.{Command, Event, State}

import scala.reflect.ClassTag

object PaymentLifecycleTracking {

  val shardingName = "PaymentLifecycleTracking"
  val maxNumberOfShards = 11

  def shardId(entityId: String): String =
    math.abs(entityId.hashCode % maxNumberOfShards).toString

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: Command => (cmd.id, cmd)
    case evt: SpeculativeReplicatedEvent  => (evt.entityId, evt)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: Command => shardId(cmd.id)
    case ShardRegion.StartEntity(entityId) => shardId(entityId)
  }

  def props(system:ActorSystem) = ReplicatedEntity.clusterShardingProps(
    entityTypeName = shardingName,
    entityFactory = () => new PaymentLifecycleTracking,
    settings = PersistenceMultiDcSettings(system))

}

class PaymentLifecycleTracking
  extends ReplicatedEntity[Command, Event, State] {

  import sample.model.PaymentLifecycle._

  override def initialState: State = State(List.empty)

  override def commandHandler: CommandHandler = CommandHandler { (ctx, state, command) =>
    command match {

      case setBalanceCommand: SetBalance =>
        Effect.persist(Requested(setBalanceCommand)).andThen { _ =>
            setBalanceCommand match {
              case a: Authorize =>
                // TODO : Add side effect to simulate sending at-most-once-command to downstream card network/aquirer
                println(s"Authorizing: $a")
                a.replyTo.tell(SetBalanceResponse(a.id,a.amount,ReceivedSuccessfully))
              case c: Chargeback =>
                println(s"Processing chargeback: $c")
              case s: Settle =>
                println(s"Processing settlement: $s")
              case r: Refund =>
                println(s"Processing refund: $r")
            }
        }

      case getBalanceCommand: GetBalance => getBalanceCommand match {
        case g:GetAuthorizationBalance =>
          val balance = state.history.filter(isBalance[Authorization](_))
          g.replyTo ! BalanceResponse(balance)
          Effect.none
        case g:GetSettledBalance =>
          val balance = state.history.filter(isBalance[Settlement](_))
          g.replyTo ! BalanceResponse(balance)
          Effect.none
        case g:GetRefundedBalance =>
          val balance = state.history.filter(isBalance[Refunded](_))
          g.replyTo ! BalanceResponse(balance)
          Effect.none
        case g:GetChargebackBalance =>
          val balance = state.history.filter(isBalance[Chargedback](_))
          g.replyTo ! BalanceResponse(balance)
          Effect.none
      }
    }
  }

  override def eventHandler(state: State, event: Event): State = {
    State(event :: state.history)
  }

  def isBalance[T <: Balance](event: Event)(implicit tag: ClassTag[T]):Boolean = event match {
    // TODO: is this Requested or Successful?
    case Successful(balance) => balance match {
      case _ : T => true
      case _ => false
    }
    case _ => false
  }

}
