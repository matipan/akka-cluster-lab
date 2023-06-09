lazy val akkaHttpVersion = "10.5.1"
lazy val akkaVersion    = "2.7.0"
lazy val akkaManagementVersion    = "1.3.0"

// Run in a separate JVM, to make sure sbt waits until all threads have
// finished before returning.
// If you want to keep the application running while executing other
// sbt tasks, consider https://github.com/spray/sbt-revolver/
fork := true


import sbtassembly.AssemblyPlugin.autoImport._

assemblyMergeStrategy in assembly := {
  case "reference.conf" => MergeStrategy.concat
  case PathList(rest @ _*) =>
    MergeStrategy.first // Use your project's version of the classes
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

mainClass in assembly := Some("dev.matiaspan.Main")

fork in run := true
javaOptions in run ++= Seq(
  "-Dconfig.resource=local.conf"
)

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "dev.matiaspan",
      scalaVersion    := "2.13.4"
    )),
    name := "cluster-chat",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "ch.qos.logback"    % "logback-classic"           % "1.2.11",

      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.2.9"         % Test,

      "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,

      "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaManagementVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaManagementVersion,
      "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % akkaManagementVersion,
      "com.lightbend.akka.management" %% "akka-rolling-update-kubernetes" % akkaManagementVersion
    )
  )
