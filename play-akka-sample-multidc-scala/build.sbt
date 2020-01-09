import play.core.PlayVersion.{akkaVersion => AkkaVersion}

organization := "com.typesafe.akka.samples"
name := "play-akka-sample-multidc-scala"

enablePlugins(PlayScala)
disablePlugins(PlayLayoutPlugin)

scalaVersion := "2.13.1"

val AkkaAddOnsVersion = "1.1.12"
val AkkaPersistenceCassandraVersion = "0.100"
val AkkaHttpVersion = "10.1.11"
val AkkaClusterManagementVersion = "1.0.3"

credentials += Credentials(Path.userHome / ".lightbend" / "commercial.credentials")
resolvers += "com-mvn" at "https://repo.lightbend.com/commercial-releases/"
resolvers += Resolver.url("com-ivy",
  url("https://repo.lightbend.com/commercial-releases/"))(Resolver.ivyStylePatterns)

libraryDependencies ++= Seq(
  guice, logback,
  "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % AkkaVersion,
  "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-protobuf" % AkkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding" % AkkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-core" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-parsing" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding" % AkkaVersion,
  "com.typesafe.akka" %% "akka-persistence-query" % AkkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
  "com.lightbend.akka" %% "akka-persistence-multi-dc" % AkkaAddOnsVersion,
  "com.lightbend.akka" %% "akka-persistence-multi-dc-testkit" % AkkaAddOnsVersion,
  "com.lightbend.akka" %% "akka-split-brain-resolver" % AkkaAddOnsVersion,
  "com.lightbend.akka" %% "akka-diagnostics" % AkkaAddOnsVersion,
  "com.lightbend.akka.management" %% "akka-management" % AkkaClusterManagementVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-http" % AkkaClusterManagementVersion,
  "com.typesafe.akka" %% "akka-persistence-cassandra-launcher" % AkkaPersistenceCassandraVersion % "test",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % "test",
)

// transitive dependency of akka 2.5x that is brought in by addons but evicted
dependencyOverrides += "com.typesafe.akka" %% "akka-protobuf" % AkkaVersion

licenses := Seq(("CC0", url("http://creativecommons.org/publicdomain/zero/1.0")))
