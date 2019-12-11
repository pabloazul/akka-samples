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
  extends ReplicatedEntity[Command, Event[Correlated], State] {

  import sample.model.PaymentLifecycle._

  override def initialState: State = State(List.empty)

  override def commandHandler: CommandHandler = CommandHandler { (ctx, state, command) =>
    command match {
            case c:Command  =>
              Effect.none
    }
  }

  override def eventHandler(state: State, event: Event[Correlated]): State = {
      event match {
        case _ => state
    }
  }

}
