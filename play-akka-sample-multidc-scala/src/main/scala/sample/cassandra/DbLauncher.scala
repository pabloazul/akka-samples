package sample.cassandra

import java.io.File
import java.util.concurrent.CountDownLatch

import akka.persistence.cassandra.testkit.CassandraLauncher

object DbLauncher {

  def main(args: Array[String]) = {
    startCassandraDatabase()
    println("Started Cassandra, press Ctrl + C to kill")
    new CountDownLatch(1).await()
  }

  def startCassandraDatabase(): Unit = {
    val databaseDirectory = new File("target/cassandra-db")
    CassandraLauncher.start(
      databaseDirectory,
      CassandraLauncher.DefaultTestConfigResource,
      clean = false,
      port = 9042)
  }
}
