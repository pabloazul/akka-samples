package sample.play
import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import javax.inject._
import sample.persistence.multidc.PaymentLifecycleTracking
import play.api.inject.{SimpleModule, _}
import sample.model.PaymentLifecycle.Command
import akka.actor.typed.scaladsl.adapter._

class MultiDCModule extends SimpleModule(
    bind[ClusterSharding].toProvider[ClusterShardingProvider],
    bind[ActorRef[Command]].qualifiedWith(PaymentLifecycleTracking.shardingName)
      .toProvider[ReplicatedShardRefProvider]
)

class ReplicatedShardRefProvider @Inject()(clusterSharding: ClusterSharding, system: ActorSystem) extends Provider[ActorRef[Command]] {

  import PaymentLifecycleTracking._

  override val get: ActorRef[Command] = {
    clusterSharding.start(
      shardingName,
      props(system),
      ClusterShardingSettings(system),
      extractEntityId,
      extractShardId
    ).toTyped[Command]
  }
}

class ClusterShardingProvider @Inject()(val actorSystem: ActorSystem) extends Provider[ClusterSharding] {
  val get: ClusterSharding = ClusterSharding.get(actorSystem)
}


