addSbtPlugin(
  "org.scala-js" % "sbt-scalajs" % "0.6.21" excludeAll (
    "com.google.protobuf" % "protobuf-java"
  )
)

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.12")

libraryDependencies ++= Seq(
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value,
  "com.trueaccord.scalapb" %% "compilerplugin" % "0.6.6"
)
