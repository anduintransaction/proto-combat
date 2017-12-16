scalaVersion in ThisBuild := "2.12.4"

lazy val `proto-compat-directives` = crossProject
  .crossType(CrossType.Pure)
  .in(file("modules") / "directives")

lazy val `proto-compat-directivesJVM` = `proto-compat-directives`.jvm
lazy val `proto-compat-directivesJS` = `proto-compat-directives`.js

lazy val `proto-compat-core` = project
  .in(file("modules") / "core")
  .dependsOn(`proto-compat-directivesJVM`)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "1.0.0-RC1",
      "com.google.protobuf" % "protobuf-java" % "3.5.0",
      "com.trueaccord.scalapb" %% "protoc-bridge" % "0.3.0-M1",
      "com.github.os72" % "protoc-jar" % "3.5.0",
      "com.trueaccord.scalapb" %% "scalapb-runtime" % "0.6.7" excludeAll (
        "com.google.protobuf" % "protobuf-java"
        )
    ),
    scalacOptions += "-Ypartial-unification",
    fork := true
  )

lazy val `sbt-proto-compat` = project
  .in(file("modules") / "sbt-plugin")
  .dependsOn(`proto-compat-core`)
  .settings(
    sbtPlugin := true
  )
