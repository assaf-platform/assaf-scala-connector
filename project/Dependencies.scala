import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5"
  lazy val akka = "com.typesafe.akka" %% "akka-stream" % "2.5.21"
  lazy val rabbitmq =  "com.lightbend.akka" %% "akka-stream-alpakka-amqp" % "1.0-RC1"
  lazy val upickle = "com.lihaoyi" %% "upickle" % "0.7.1"
  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime

}
