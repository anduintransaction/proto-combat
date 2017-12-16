lazy val scalapbVersion = System.getProperty("scalapb.version")
lazy val protoCompatVersion = System.getProperty("protoCompat.version")

{
  require(scalapbVersion != null, "ScalaPB version is not specified.")
  require(protoCompatVersion != null, "ProtoCompat version is not specified.")
  addSbtPlugin("com.anduintransact" % "sbt-proto-compat" % protoCompatVersion)
}

libraryDependencies ++= Seq(
  "com.trueaccord.scalapb" %% "compilerplugin" % "0.6.6"
)
