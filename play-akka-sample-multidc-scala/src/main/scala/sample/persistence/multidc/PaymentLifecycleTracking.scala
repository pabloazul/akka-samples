package sample.persistence.multidc

import akka.actor.ActorSystem
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.persistence.multidc.{PersistenceMultiDcSettings, SpeculativeReplicatedEvent}
import akka.persistence.multidc.scaladsl.ReplicatedEntity
import sample.model.PaymentLifecycle.{Command, Correlated, Event, State}


object PaymentLifecycleTracking {

  val shardingName = "PaymentLifecycleTracking"
  val maxNumberOfShards = 11

  def shardId(entityId: String): String =
    (math.abs(entityId.hashCode) % maxNumberOfShards).toString

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

  def region(system:ActorSystem) =
    ClusterSharding(system).start(shardingName, props(system), ClusterShardingSettings(system),
      extractEntityId, extractShardId)

}

class PaymentLifecycleTracking
  extends ReplicatedEntity[Command, Event, State] {

  import sample.model.PaymentLifecycle._

  override def initialState: State = State(List.empty)

  override def commandHandler: CommandHandler = CommandHandler { (ctx, state, command) =>
    command match {
            case authorize:Authorize   => Effect.persist(AuthorizationRequested(authorize))
            case settle:Settle         => Effect.persist(SettlementRequested(settle))
            case refund:Refund         => Effect.persist(RefundRequested(refund))
            case chargeback:Chargeback => Effect.persist(ChargebackRequested(chargeback))
    }

  }

  override def eventHandler(state: State, event: Event): State = {
      State(event :: state.history)
  }

}

