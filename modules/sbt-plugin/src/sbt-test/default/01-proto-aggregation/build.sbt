import sbtprotoc.ProtocPlugin.{ProtobufConfig => Protobuf}
import scalapb.compiler.Version.{scalapbVersion => ScalapbVersion}

lazy val root = project
  .in(file("."))
  .settings(
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
  .enablePlugins(ProtoCompatPlugin)
