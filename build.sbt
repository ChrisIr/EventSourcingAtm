import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

ThisBuild / version := "1.0"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .aggregate(common, atm, notifier)
  .settings(
    name := "EventSourcingAtm"
  )
lazy val common = project
  .settings(
    name := "common-lib",
    libraryDependencies ++= dependencies
  )

lazy val atm = project
  .settings(
    name := "atm-application",
    libraryDependencies ++= dependencies,
    Compile / mainClass := Some("com.eventsourcing.atm.AtmApp"),
    dockerCommands ++= Seq(
      Cmd("USER", "root"),
      ExecCmd("RUN", "mkdir", "-p", "/opt/docker/logs/"),
      ExecCmd("RUN", "chmod", "777", "/opt/docker/logs/"),
      Cmd("USER", "1001:0")
    )
  )
  .dependsOn(common)
  .enablePlugins(DockerPlugin)
  .enablePlugins(JavaAppPackaging)

lazy val notifier = project
  .settings(
    name := "notifier-application",
    libraryDependencies ++= dependencies,
    Compile / mainClass := Some("com.eventsourcing.notifier.NotifierApp"),
    dockerCommands ++= Seq(
      Cmd("USER", "root"),
      ExecCmd("RUN", "mkdir", "-p", "/opt/docker/logs/"),
      ExecCmd("RUN", "chmod", "777", "/opt/docker/logs/"),
      Cmd("USER", "1001:0")
    )
  )
  .dependsOn(common)
  .enablePlugins(DockerPlugin)
  .enablePlugins(JavaAppPackaging)

val dependencies = Seq(
  "org.apache.kafka" % "kafka-streams" % "3.1.0",
  "org.apache.kafka" %% "kafka-streams-scala" % "3.1.0",
  "org.apache.kafka" % "kafka-streams-test-utils" % "3.1.0",
  "org.scalactic" %% "scalactic" % "3.2.11",
  "org.scalatest" %% "scalatest" % "3.2.11" % "test",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
  "ch.qos.logback" % "logback-classic" % "1.2.11",
  "com.typesafe.play" %% "play-json" % "2.9.2",
  "org.julienrf" %% "play-json-derived-codecs" % "10.0.2",
  "com.github.pureconfig" %% "pureconfig" % "0.17.1"
)