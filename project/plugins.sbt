addSbtPlugin(
  "org.scala-js" % "sbt-scalajs" % "0.6.22" excludeAll (
    "com.google.protobuf" % "protobuf-java"
  )
)

addSbtPlugin(
  "com.thesamet" % "sbt-protoc" % "0.99.13" excludeAll (
    "com.github.os72" % "protoc-jar"
  )
)

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.4")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.7")

libraryDependencies ++= Seq(
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value,
  "com.thesamet.scalapb" %% "compilerplugin" % "0.7.0-rc7",
  "com.github.os72" % "protoc-jar" % "3.5.1.1"
)
