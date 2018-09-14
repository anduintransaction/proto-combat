
val crossProjectVersion = "0.6.0"

addSbtPlugin(
  "org.scala-js" % "sbt-scalajs" % "0.6.25" excludeAll (
    "com.google.protobuf" % "protobuf-java"
    )
)

addSbtPlugin(
  "org.portable-scala" % "sbt-crossproject" % crossProjectVersion excludeAll
    ExclusionRule(organization = "org.portable-scala", name = "sbt-platform-deps")
)

addSbtPlugin(
  "org.portable-scala" % "sbt-scalajs-crossproject" % crossProjectVersion excludeAll (
    ExclusionRule(organization = "org.portable-scala", name = "sbt-platform-deps"),
    ExclusionRule(organization = "org.scala-js", name = "sbt-scalajs")
  )
)

addSbtPlugin(
  "com.thesamet" % "sbt-protoc" % "0.99.18" excludeAll (
    "com.github.os72" % "protoc-jar"
  )
)

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.4")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.7")

libraryDependencies ++= Seq(
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value,
  "com.thesamet.scalapb" %% "compilerplugin-shaded" % "0.8.0-RC2",
  "com.github.os72" % "protoc-jar" % "3.6.0"
)
