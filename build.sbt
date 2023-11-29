name := """DistributedStroke"""
organization := "org.agh"

version := "1.0-SNAPSHOT"


lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.8"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test
libraryDependencies += "org.apache.spark" %% "spark-mllib" % "3.2.1"
libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-geode" % "3.0.4"
libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-json-streaming" % "3.0.4"
libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-file" % "3.0.4"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "org.agh.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "org.agh.binders._"
