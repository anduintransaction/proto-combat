import sbtprotoc.ProtocPlugin.{ProtobufConfig => Protobuf}
import com.trueaccord.scalapb.compiler.Version.{scalapbVersion => ScalapbVersion}

lazy val commonSettings = Seq(
  scalaVersion := "2.12.4",
  libraryDependencies ++= Seq(
    "com.thesamet.scalapb" %% "scalapb-runtime" % ScalapbVersion % Protobuf
  ),
  PB.protocVersion := "-v351",
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
