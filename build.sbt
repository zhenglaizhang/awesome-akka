name := "awesome-akka"

version := "0.1"

scalaVersion := "2.13.6"


val AkkaVersion = "2.6.15"
val AkkaHttpVersion = "10.2.6"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.5"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.1.4" % Test
)
