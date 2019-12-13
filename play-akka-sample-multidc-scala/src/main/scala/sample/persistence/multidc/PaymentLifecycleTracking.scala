package sample.persistence.multidc

import akka.actor.ActorSystem
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.persistence.multidc.{PersistenceMultiDcSettings, SpeculativeReplicatedEvent}
import akka.persistence.multidc.scaladsl.ReplicatedEntity
import sample.model.PaymentLifecycle.{Command, Event, State}

import scala.reflect.ClassTag

object PaymentLifecycleTracking {

  val shardingName = "PaymentLifecycleTracking"
  val maxNumberOfShards = 11

  def shardId(entityId: String): String =
    (math.abs(entityId.hashCode % maxNumberOfShards)).toString

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
        // Send authorization over TCP
        Effect.persist(Requested(setBalanceCommand))
      case getBalanceCommand: GetBalance => getBalanceCommand match {
        case g:GetAuthorizationBalance =>
          val balance = state.history.filter(isBalance[Authorization](_))
          Effect.none
        case g:GetSettledBalance =>
          val balance = state.history.filter(isBalance[Settlement](_))
          Effect.none
        case g:GetRefundedBalance =>
          val balance = state.history.filter(isBalance[Refunded](_))
          Effect.none
        case g:GetChargebackBalance =>
          val balance = state.history.filter(isBalance[Chargedback](_))
          Effect.none
      }
    }
  }

  override def eventHandler(state: State, event: Event): State = {
    State(event :: state.history)
  }

  def isBalance[T <: Balance](event: Event)(implicit tag: ClassTag[T]):Boolean = event match {
    case Requested(balance) => balance match {
      case a:T => true
      case _ => false
    }
    case _ => false
  }
}
