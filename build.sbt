import Dependencies._

ThisBuild / scalaVersion     := "2.12.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.wonderkind"
ThisBuild / organizationName := "wonderkind"

lazy val root = (project in file("."))
  .settings(
    name := "assaf-scala-connector",
    libraryDependencies ++= Seq(scalaTest % Test, akka, rabbitmq, upickle, logback)  
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
