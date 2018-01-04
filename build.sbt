import com.trueaccord.scalapb.compiler.Version.{scalapbVersion => ScalapbVersion}
import sbtprotoc.ProtocPlugin.{ProtobufConfig => Protobuf}
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

lazy val runScriptedTests = taskKey[Unit]("Run all scripted tests.")

inThisBuild(
  Seq(
    organization := "com.anduintransact",
    scalaVersion := "2.12.4",
    // https://github.com/sbt/sbt/issues/3570
    updateOptions := updateOptions.value.withGigahorse(false),
    publishTo := Some(
      Resolver.url(
        "Anduin Transactions Artifactory",
        url("https://artifactory.anduintransact.com/artifactory/anduin-internal-libraries/")
      )(Resolver.ivyStylePatterns)
    ),
    publishMavenStyle := false
  )
)

lazy val `proto-compat-directives` = crossProject
  .crossType(CrossType.Pure)
  .in(file("modules") / "directives")
  .settings(
    libraryDependencies ++= Seq(
      "com.trueaccord.scalapb" %% "scalapb-runtime" % ScalapbVersion % Protobuf
    ),
    unmanagedResourceDirectories in Compile += {
      baseDirectory.value.getParentFile / "src" / "main" / "protobuf"
    },
    PB.protoSources in Compile := Seq(
      baseDirectory.value.getParentFile / "src" / "main" / "protobuf"
    ),
    PB.targets in Compile := Seq(
      protocbridge.gens.java -> (sourceManaged in Compile).value
    )
  )

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
    scalacOptions += "-Ypartial-unification"
  )

lazy val `sbt-proto-compat` = project
  .in(file("modules") / "sbt-plugin")
  .dependsOn(`proto-compat-core`)
  .settings(
    sbtPlugin := true,
    libraryDependencies ++= Seq(
      Defaults.sbtPluginExtra(
        "com.thesamet" % "sbt-protoc" % "0.99.12",
        (sbtBinaryVersion in pluginCrossBuild).value,
        (scalaBinaryVersion in pluginCrossBuild).value
      )
    ),
    runScriptedTests := scripted.toTask("").value,
    publishLocal := publishLocal
      .dependsOn(
        // Hacky, why does this project have to know all transitive dependencies?
        publishLocal in `proto-compat-directivesJVM`,
        publishLocal in `proto-compat-core`
      )
      .value,
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      "-DscalapbCompiler.version=0.6.6",
      "-DprotoCompat.version=" + version.value
    ),
    scriptedBufferLog := false
  )

lazy val `proto-compat` = project
  .in(file("."))
  .aggregate(
    `proto-compat-directivesJVM`,
    `proto-compat-directivesJS`,
    `proto-compat-core`,
    `sbt-proto-compat`
  )
  .settings(
    publish := {},
    publishLocal := {},
    publishArtifact := false,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      releaseStepInputTask(scripted in `sbt-proto-compat`),
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      publishArtifacts,
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  )
