import sbt.Keys._
import sbtassembly.AssemblyPlugin.autoImport._

val scalatest = "org.scalatest" %% "scalatest" % "3.1.1" % "test"
val logging_library = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4"
val slf4j = "org.slf4j" % "slf4j-api" % "1.7.36"
val logback_core = "ch.qos.logback" % "logback-core" % "1.2.10"
val logback_classic = "ch.qos.logback" % "logback-classic" % "1.2.10"
val junit = "junit" % "junit" % "4.12" % "test"
val scopt = "com.github.scopt" %% "scopt" % "3.7.1"
val gstlib = "com.github.guillaumedd" %% "gstlib" % "0.1.3"

lazy val commonSettings = Seq(
  organization := "com.github.guillaumedd",
  version := "2.0",
  scalaVersion := "2.13.12"
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "dialign",
    libraryDependencies += scalatest,
    libraryDependencies += logging_library,
    libraryDependencies += slf4j,
    libraryDependencies += logback_core,
    libraryDependencies += logback_classic,
    libraryDependencies += junit,
    libraryDependencies += scopt,
    libraryDependencies += gstlib,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % "10.2.10",
      "com.typesafe.akka" %% "akka-stream" % "2.6.19",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.2.10"
    )
  )

scalacOptions ++= Seq("-deprecation", "-Ywarn-unused", "-Ywarn-dead-code",
                      "-opt:l:inline", "-opt-inline-from:**", "-Ywarn-unused:imports")

Compile / mainClass := Some("dialign.app.DialignWebService")
assembly / mainClass := Some("dialign.app.DialignWebService")

assembly / assemblyJarName := "dialign-webservice.jar"

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case "reference.conf" => MergeStrategy.concat
  case x => MergeStrategy.first
}

//assemblyJarName in assembly := "dialign-online.jar"
//mainClass in assembly := Some("dialign.app.DialignOnlineApp")
