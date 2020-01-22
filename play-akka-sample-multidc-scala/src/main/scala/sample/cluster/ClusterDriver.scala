package sample.cluster

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.management.scaladsl.AkkaManagement
import akka.persistence.multidc.PersistenceMultiDcSettings
import com.typesafe.config.{Config, ConfigFactory}
import sample.persistence.multidc.PaymentLifecycleTracking

object ClusterDriver {

  def main(args: Array[String]) : Unit = {

    args.headOption match {

      case None =>
        startClusterInSameJvm()
      case Some(portString) if portString.matches("""\d+""") =>
        val port = portString.toInt
        val dc = args.tail.headOption.getOrElse("eu-west")
        startNode(port, dc)
    }
  }

  def startClusterInSameJvm(): Unit = {
    startNode(2551, "eu-west")
    startNode(2552, "eu-central")
  }

  def startNode(port: Int, dc: String): Unit = {
    val system = ActorSystem("application", config(port, dc))

    val persistenceMultiDcSettings = PersistenceMultiDcSettings(system)

    val counterRegion = ClusterSharding(system).start(
      typeName = PaymentLifecycleTracking.shardingName,
      entityProps = PaymentLifecycleTracking.props(system),
      settings = ClusterShardingSettings(system),
      extractEntityId = PaymentLifecycleTracking.extractEntityId,
      extractShardId = PaymentLifecycleTracking.extractShardId)

    // The speculative replication requires sharding proxies to other DCs
    if (persistenceMultiDcSettings.useSpeculativeReplication) {
      persistenceMultiDcSettings.otherDcs(Cluster(system).selfDataCenter).foreach { d =>
        ClusterSharding(system).startProxy(PaymentLifecycleTracking.shardingName, role = None,
          dataCenter = Some(d), PaymentLifecycleTracking.extractEntityId, PaymentLifecycleTracking.extractShardId)
      }
    }

//    if (port != 0) {
//      ThumbsUpHttp.startServer("0.0.0.0", 20000 + port, counterRegion)(system)
//
//      AkkaManagement(system).start()
//    }

  }

  def config(port: Int, dc: String): Config =
    ConfigFactory.parseString(s"""
      akka.remote.artery.canonical.port = $port

      akka.management.http.port = 1$port

      akka.cluster.multi-data-center.self-data-center = $dc
    """).withFallback(ConfigFactory.load("application.conf"))

}
