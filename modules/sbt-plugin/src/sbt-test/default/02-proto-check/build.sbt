import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.{Type => FieldType}
import com.trueaccord.scalapb.compiler.Version.{scalapbVersion => ScalapbVersion}
import sbtprotoc.ProtocPlugin.{ProtobufConfig => Protobuf}

import anduin.protocompat.check._

lazy val commonSettings = Seq(
  scalaVersion := "2.12.4",
  libraryDependencies ++= Seq(
    "com.trueaccord.scalapb" %% "scalapb-runtime" % ScalapbVersion % Protobuf
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

TaskKey[Unit]("check") := {
  val result = (compatCheckResult in Compile in `new`).value

  def requireIncompat(
    newPath: ProtoCheckPath,
    oldPath: ProtoCheckPath,
    reason: ProtoIncompatReason
  ): Unit = {
    val incompat = ProtoIncompat(newPath, oldPath, reason)

    if (!result.incompats.contains(incompat)) {
      sys.error(s"Incompatibility not found: $incompat.")
    }
  }

  requireIncompat(
    ProtoCheckPath(Vector.empty, "example.Person"),
    ProtoCheckPath(Vector.empty, "example.Person"),
    FieldRemovedNotReserved(2, "age")
  )

  requireIncompat(
    ProtoCheckPath(Vector("example.Person" -> 3), "example.Info"),
    ProtoCheckPath(Vector("example.Person" -> 3), "example.Info"),
    FieldTypeConflicted(1, "nid", FieldType.TYPE_STRING, "nid", FieldType.TYPE_INT32)
  )

  requireIncompat(
    ProtoCheckPath(Vector("example.Person" -> 3), "example.Info"),
    ProtoCheckPath(Vector("example.Person" -> 3), "example.Info"),
    ScalaFieldTypeConflicted(2, "sid", "BigInt", "sid", "BigInteger")
  )

  requireIncompat(
    ProtoCheckPath(Vector("example.Person" -> 3), "example.Info"),
    ProtoCheckPath(Vector("example.Person" -> 3), "example.Info"),
    FieldRenamed(3, "iid", "insurance_id")
  )
}
