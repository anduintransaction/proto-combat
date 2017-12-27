package anduin.protocompat

import scala.util.control.NoStackTrace

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

    val compatAggregatedPath = settingKey[File](
      "The path that contains proto files aggregated from all include paths."
    )

    val compatAggregate = taskKey[File](
      "Aggregate proto files from all include paths."
    )

    val compatAggregateTo = inputKey[File](
      "Aggregate proto files from all include paths to the given path."
    )

    val compatCheckRoots = settingKey[Seq[String]](
      "Root protos to check for compatibility."
    )

    val compatCheckResult = inputKey[ProtoCheckResult](
      "Check proto for compatibility and give result."
    )

    val compatCheck = inputKey[Unit](
      "Check proto for compatibility and report."
    )
  }

  import autoImport._

  final class CheckFailedException(message: String) extends NoStackTrace {
    override def getMessage: String = message
  }

  private lazy val projectDefaultSettings: Seq[Def.Setting[_]] = Seq(
    )

  private def aggregate(inputs: Seq[File], output: File): File = {
    IO.createDirectory(output)

    inputs.foreach { includePath =>
      val protoFinder = PathFinder(includePath) ** "*.proto"
      val fileMap = Path.rebase(includePath, output)
      IO.copy(protoFinder.pair(fileMap))
    }

    output
  }

  def projectScopeSettings(config: Configuration): Seq[Def.Setting[_]] = {
    inConfig(config)(
      Seq(
        compatAggregatedPath := resourceManaged.value / "protobuf",
        compatAggregate := {
          PB.unpackDependencies.value
          aggregate(PB.includePaths.value, compatAggregatedPath.value)
        },

        compatAggregateTo := {
          val Seq(path) = Def.spaceDelimited("path").parsed
          PB.unpackDependencies.value
          // TODO: Better argument parsing.
          aggregate(PB.includePaths.value, new File(path))
        },

        compatCheckResult := {
          // TODO: Better argument parsing.
          val Seq(oldPath, newPath) = Def.spaceDelimited("path").parsed.map(new File(_))
          val roots = compatCheckRoots.?.value.getOrElse(
            throw new IllegalArgumentException("compatCheckRoots must be set.")
          )

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
          val result = compatCheckResult.parsed.value
          val reporter = new ConsoleProtoReporter(showFullPath = true)

          reporter.report(result)

          if (result.incompats.nonEmpty) {
            throw new CheckFailedException("Old and new protos are incompatible.")
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
