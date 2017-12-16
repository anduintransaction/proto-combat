package anduin.protocompat

import sbt.Keys._
import sbt._
import sbtprotoc.ProtocPlugin
import sbtprotoc.ProtocPlugin.autoImport.PB

object ProtoCompatPlugin extends AutoPlugin {

  override lazy val trigger: PluginTrigger = noTrigger
  override lazy val requires: Plugins = ProtocPlugin

  object autoImport {

    val compatAggregatedPath = SettingKey[File](
      "compat-aggregated-path",
      "The path that contains proto files aggregated from all include paths."
    )

    val compatAggregate = TaskKey[File](
      "compat-aggregate",
      "Aggregate proto files from all include paths."
    )
  }

  import autoImport._

  private lazy val projectDefaultSettings: Seq[Def.Setting[_]] = Seq(
    )

  def projectScopeSettings(config: Configuration): Seq[Def.Setting[_]] = {
    inConfig(config)(
      Seq(
        compatAggregatedPath := resourceManaged.value / "protobuf",
        compatAggregate := {
          val aggregatedPath = compatAggregatedPath.value

          IO.createDirectory(aggregatedPath)

          PB.includePaths.value.foreach { includePath =>
            val protoFinder = PathFinder(includePath) ** "*.proto"
            val fileMap = Path.rebase(includePath, aggregatedPath)
            IO.copy(protoFinder.pair(fileMap))
          }

          aggregatedPath
        },
        compatAggregate := compatAggregate.dependsOn(PB.unpackDependencies).value
      )
    )
  }

  override lazy val projectSettings: Seq[Def.Setting[_]] =
    projectDefaultSettings ++
      projectScopeSettings(Compile) ++
      projectScopeSettings(Test)
}
