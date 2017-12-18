package anduin.protocompat.report

import java.io.File

import anduin.protocompat.check._
import anduin.protocompat.tree.ProtoTree

final class ConsoleProtoReporter(
  showFullPath: Boolean
) extends ProtoReporter {

  import ConsoleProtoReporter._

  private[this] def protoPath(
    name: String,
    kind: ProtoKind,
    basePaths: Map[String, String],
    tree: ProtoTree
  ): String = {
    val optionalProtoAndFile = tree.findProtoAndFile(name)

    require(
      optionalProtoAndFile.isDefined,
      s"${kind.capitalizeName} proto with name $name doesn't exist."
    )

    val file = optionalProtoAndFile.get match { case (_, file) => file }
    val optionalBasePath = basePaths.get(file.proto.getName)

    require(
      optionalBasePath.isDefined,
      s"${kind.capitalizeName} proto file ${file.proto.getName} doesn't exist."
    )

    s"$name (${optionalBasePath.get}${File.separator}${file.proto.getName})"
  }

  private[this] def fullPath(
    path: ProtoCheckPath,
    kind: ProtoKind,
    basePaths: Map[String, String],
    tree: ProtoTree
  ): String = {
    val builder = StringBuilder.newBuilder

    builder.append(s"* ${kind.capitalizeName} proto:$NewLine")
    builder.append(protoPath(path.name, kind, basePaths, tree))
    builder.append(NewLine)

    if (showFullPath) {
      path.supers.foreach {
        case (name, tagNumber) =>
          builder.append(s"as tag $tagNumber in ${protoPath(name, kind, basePaths, tree)}$NewLine")
      }
    }

    builder.result()
  }

  def report(result: ProtoCheckResult): Unit = {
    val builder = StringBuilder.newBuilder

    result.incompats.foreach { incompat =>
      incompat.reason match {
        case FieldRemovedNotReserved(oldTagNumber, oldFieldName) =>
          builder.append(
            s"Field $oldFieldName (tag $oldTagNumber) removed but not reserved.$NewLine"
          )

        case FieldTypeConflicted(
            tagNumber,
            newFieldName,
            newFieldType,
            oldFieldName,
            oldFieldType
            ) =>
          builder.append(
            s"Type ${fieldType(oldFieldType)} of old field $oldFieldName (tag $tagNumber)$NewLine"
          )

          builder.append(
            s"conflicts with type ${fieldType(newFieldType)} of new field $newFieldName (tag $tagNumber)$NewLine"
          )

        case ScalaFieldTypeConflicted(
            tagNumber,
            oldFieldName,
            oldScalaFieldType,
            newFieldName,
            newScalaFieldType
            ) =>
          builder.append(
            s"Scala type $oldScalaFieldType of old field $oldFieldName (tag $tagNumber)$NewLine"
          )

          builder.append(
            s"conflicts with Scala type $newScalaFieldType of new field $newFieldName (tag $tagNumber)$NewLine"
          )

        case FieldRenamed(
            tagNumber,
            oldFieldName,
            newFieldName
            ) =>
          builder.append(s"Old field $oldFieldName (tag $tagNumber)$NewLine")
          builder.append(s"is renamed to new field $newFieldName (tag $tagNumber)$NewLine")

        case _ =>
        // Do nothing
      }

      // In most cases it's more natural to have old path coming first
      builder.append(fullPath(incompat.oldPath, ProtoKind.Old, result.oldBasePaths, result.oldTree))
      builder.append(fullPath(incompat.newPath, ProtoKind.New, result.newBasePaths, result.newTree))
      builder.append(NewLine)
    }

    println()
    print(builder.result())
    Console.flush()
  }
}

object ConsoleProtoReporter {

  private val NewLine = "\n"

  private sealed abstract class ProtoKind(val name: String) extends Product with Serializable {
    def capitalizeName: String = name.capitalize
  }

  private object ProtoKind {
    case object New extends ProtoKind("new")
    case object Old extends ProtoKind("old")
  }
}
