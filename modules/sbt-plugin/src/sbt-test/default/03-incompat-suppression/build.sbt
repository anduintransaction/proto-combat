import com.trueaccord.scalapb.compiler.Version.{scalapbVersion => ScalapbVersion}
import sbtprotoc.ProtocPlugin.{ProtobufConfig => Protobuf}

lazy val ProtoCompatVersion = {
  val version = System.getProperty("protoCompat.version")
  require(version != null, "ProtoCompat version is not specified.")
  version
}

lazy val commonSettings = Seq(
  scalaVersion := "2.12.4",
  libraryDependencies ++= Seq(
    "com.trueaccord.scalapb" %% "scalapb-runtime" % ScalapbVersion % Protobuf,
    "com.anduintransact" %% "proto-compat-directives" % ProtoCompatVersion % Protobuf
  ),
  PB.targets in Compile := Seq(
    scalapb.gen(
      flatPackage = true,
      grpc = false
    ) -> (sourceManaged in Compile).value
  )
)

lazy val `new` = project
  .settings(
    commonSettings,
    inConfig(Compile)(
      Seq(
        compatOldPath := (compatAggregatedPath in Compile in old).value,
        compatNewPath := compatAggregatedPath.value,
        compatCheckRoots := Seq("example.Person"),
        compatCheckResult := compatCheckResult
          .dependsOn(
            compatAggregate in Compile in old,
            compatAggregate
          )
          .value
      )
    )
  )
  .enablePlugins(ProtoCompatPlugin)

lazy val old = project
  .settings(commonSettings)
  .enablePlugins(ProtoCompatPlugin)
