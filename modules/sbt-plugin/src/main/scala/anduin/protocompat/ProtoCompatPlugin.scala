package anduin.protocompat

import sbt.Keys._
import sbt._
import sbtprotoc.ProtocPlugin
import sbtprotoc.ProtocPlugin.autoImport.PB

import anduin.protocompat.check.{ProtoCheck, ProtoCheckResult}
import anduin.protocompat.report.ConsoleProtoReporter

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

    val compatOldPath = SettingKey[File](
      "compat-old-path",
      "The path that contains old proto files."
    )

    val compatNewPath = SettingKey[File](
      "compat-new-path",
      "The path that contains new proto files."
    )

    val compatCheckRoots = SettingKey[Seq[String]](
      "compat-check-roots",
      "Root protos to check for compatibility."
    )

    val compatCheckResult = TaskKey[ProtoCheckResult](
      "compat-check-result",
      "Check proto for compatibility and give result."
    )

    val compatCheck = TaskKey[Unit](
      "compat-check",
      "Check proto for compatibility and report."
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
        compatAggregate := compatAggregate.dependsOn(PB.unpackDependencies).value,
        compatCheckResult := {
          val oldPath = compatOldPath.?.value.getOrElse(
            throw new IllegalArgumentException("Old proto path is not set.")
          )

          val newPath = compatNewPath.?.value.getOrElse(
            throw new IllegalArgumentException("New proto path is not set.")
          )

          val roots = compatCheckRoots.?.value.getOrElse(
            throw new IllegalArgumentException("Proto roots are not set.")
          )

          // Use all proto files in include paths are sources for simplicity
          val oldSources = (PathFinder(oldPath) ** "*.proto").getPaths
          val newSources = (PathFinder(newPath) ** "*.proto").getPaths

          ProtoCheck.check(
            newProtoPaths = Vector(newPath.getAbsolutePath),
            newProtoSources = newSources.toVector,
            oldProtoPaths = Vector(oldPath.getAbsolutePath),
            oldProtoSources = oldSources.toVector,
            roots = roots.toVector
          )
        },
        compatCheck := {
          val result = compatCheckResult.value
          val reporter = new ConsoleProtoReporter(showFullPath = true)

          reporter.report(result)

          if (result.incompats.nonEmpty) {
            sys.error("Old and new protos are incompatible.")
          }
        }
      )
    )
  }

  override lazy val projectSettings: Seq[Def.Setting[_]] =
    projectDefaultSettings ++
      projectScopeSettings(Compile) ++
      projectScopeSettings(Test)
}
