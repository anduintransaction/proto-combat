import sbtprotoc.ProtocPlugin.{ProtobufConfig => Protobuf}
import scalapb.compiler.Version.{scalapbVersion => ScalapbVersion}

lazy val commonSettings = Seq(
  scalaVersion := "2.12.6",
  libraryDependencies ++= Seq(
    "com.thesamet.scalapb" %% "scalapb-runtime" % ScalapbVersion % Protobuf
  ),
  PB.protocVersion := "-v360",
  PB.targets in Compile := Seq(
    scalapb.gen(
      flatPackage = true,
      grpc = false
    ) -> (sourceManaged in Compile).value
  )
)

lazy val `new` = project
  .settings(commonSettings)
  .enablePlugins(ProtoCompatPlugin)

lazy val old = project
  .settings(commonSettings)
  .enablePlugins(ProtoCompatPlugin)

compatCheckRoots := Seq("Person")

aggregate in compatCheck := false
aggregate in compatCheckResult := false

enablePlugins(ProtoCompatPlugin)
