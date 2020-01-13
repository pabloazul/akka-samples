package sample.play
import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import javax.inject._
import sample.persistence.multidc.PaymentLifecycleTracking
import play.api.inject.{SimpleModule, _}
import sample.model.PaymentLifecycle
import akka.actor.typed.scaladsl.adapter._
import com.google.inject.TypeLiteral
import com.google.inject.AbstractModule
import com.google.inject.name.Names

class MultiDCModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[ClusterSharding])
      .toProvider(classOf[ClusterShardingProvider])
    bind(new TypeLiteral[ActorRef[PaymentLifecycle.Command]]() {})
      .toProvider(classOf[ReplicatedShardRefProvider])
  }
}

class ReplicatedShardRefProvider @Inject()(clusterSharding: ClusterSharding, system: ActorSystem) extends Provider[ActorRef[PaymentLifecycle.Command]] {

  import PaymentLifecycleTracking._

  override val get: ActorRef[PaymentLifecycle.Command] = {
    clusterSharding.start(
      shardingName,
      props(system),
      ClusterShardingSettings(system),
      extractEntityId,
      extractShardId
    ).toTyped[PaymentLifecycle.Command]
  }
}

class ClusterShardingProvider @Inject()(val actorSystem: ActorSystem) extends Provider[ClusterSharding] {
  val get: ClusterSharding = ClusterSharding.get(actorSystem)
}


