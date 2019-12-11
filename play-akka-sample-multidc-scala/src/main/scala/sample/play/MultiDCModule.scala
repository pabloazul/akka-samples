package sample.play

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import javax.inject._
import sample.persistence.multidc.PaymentLifecycleTracking
import play.api.inject.{SimpleModule, _}

class MultiDCModule extends SimpleModule(
    bind[ClusterSharding].toProvider[ClusterShardingProvider],
    bind[ActorRef].qualifiedWith(PaymentLifecycleTracking.shardingName)
      .toProvider[ReplicatedShardRefProvider]
)

class ReplicatedShardRefProvider @Inject()(clusterSharding: ClusterSharding, system: ActorSystem) extends Provider[akka.actor.ActorRef] {

  import PaymentLifecycleTracking._

  override val get: akka.actor.ActorRef = {
    clusterSharding.start(
      shardingName,
      props(system),
      ClusterShardingSettings(system),
      extractEntityId,
      extractShardId
    )
  }
}

class ClusterShardingProvider @Inject()(val actorSystem: ActorSystem) extends Provider[ClusterSharding] {
  val get: ClusterSharding = ClusterSharding.get(actorSystem)
}


