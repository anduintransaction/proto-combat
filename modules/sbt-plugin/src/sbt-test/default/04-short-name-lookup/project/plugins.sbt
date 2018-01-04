{
  lazy val protoCompatVersion = System.getProperty("protoCompat.version")
  require(protoCompatVersion != null, "ProtoCompat version is not specified.")
  addSbtPlugin("com.anduintransact" % "sbt-proto-compat" % protoCompatVersion)
}

{
  lazy val scalapbCompilerVersion = System.getProperty("scalapbCompiler.version")
  require(scalapbCompilerVersion != null, "ScalaPB version is not specified.")
  libraryDependencies += "com.trueaccord.scalapb" %% "compilerplugin" % scalapbCompilerVersion
}
