scalaVersion in ThisBuild := "2.12.4"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "1.0.0-RC1",
  "com.google.protobuf" % "protobuf-java" % "3.5.0",
  "com.trueaccord.scalapb" %% "protoc-bridge" % "0.3.0-M1",
  "com.github.os72" % "protoc-jar" % "3.5.0",
  "com.trueaccord.scalapb" %% "scalapb-runtime" % "0.6.7" excludeAll (
    "com.google.protobuf" % "protobuf-java"
  )
)

scalacOptions += "-Ypartial-unification"

fork := true
