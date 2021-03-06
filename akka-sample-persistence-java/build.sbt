organization := "com.lightbend.akka.samples"
name := "akka-sample-persistence-java"
version := "1.0"

scalaVersion := "2.13.1"
def akkaVersion = "2.6.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "junit" % "junit" % "4.12" % Test,
  "com.novocode" % "junit-interface" % "0.11" % Test)

Global / cancelable := false // ctrl-c

// disable parallel tests
parallelExecution in Test := false
testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")
// show full stack traces and test case durations
testOptions in Test += Tests.Argument("-oDF")
logBuffered in Test := false

licenses := Seq(("CC0", url("http://creativecommons.org/publicdomain/zero/1.0")))
