package anduin.protocompat.check

import scala.collection.JavaConverters._
import scala.collection.mutable

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.{Type => FieldType}
import com.google.protobuf.DescriptorProtos.{DescriptorProto, FieldDescriptorProto, FieldOptions}
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.GeneratedMessage.GeneratedExtension
import com.trueaccord.scalapb.Scalapb

import anduin.protocompat.Compat
import anduin.protocompat.tree.{ProtoEnum, ProtoMessage, ProtoTree}

private[check] object ProtoCheckInternals {

  private val CompatibleTypeGroups = Map(
    FieldDescriptorProto.Type.TYPE_INT32 -> 0,
    FieldDescriptorProto.Type.TYPE_INT64 -> 0,
    FieldDescriptorProto.Type.TYPE_UINT32 -> 0,
    FieldDescriptorProto.Type.TYPE_UINT64 -> 0,
    FieldDescriptorProto.Type.TYPE_BOOL -> 0,
    // Enum and bool are not really compatible with each other
    // but technically it is okay to have them in the same group
    FieldDescriptorProto.Type.TYPE_ENUM -> 0,
    FieldDescriptorProto.Type.TYPE_SINT32 -> 1,
    FieldDescriptorProto.Type.TYPE_SINT64 -> 1,
    FieldDescriptorProto.Type.TYPE_FIXED32 -> 2,
    FieldDescriptorProto.Type.TYPE_SFIXED32 -> 2,
    FieldDescriptorProto.Type.TYPE_FIXED64 -> 3,
    FieldDescriptorProto.Type.TYPE_SFIXED64 -> 3
  )

  private val CompatibleTypePairs = Set(
    FieldDescriptorProto.Type.TYPE_STRING -> FieldDescriptorProto.Type.TYPE_BYTES,
    FieldDescriptorProto.Type.TYPE_BYTES -> FieldDescriptorProto.Type.TYPE_STRING,
    FieldDescriptorProto.Type.TYPE_MESSAGE -> FieldDescriptorProto.Type.TYPE_BYTES,
    FieldDescriptorProto.Type.TYPE_BYTES -> FieldDescriptorProto.Type.TYPE_MESSAGE
  )

  private val JavaNamePart = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*"
  private val JavaNameRegex = s"$JavaNamePart(\\.$JavaNamePart)*".r

  private val extensionRegistry = ExtensionRegistry.newInstance()
  Scalapb.registerAllExtensions(extensionRegistry)
  Compat.registerAllExtensions(extensionRegistry)

  private def checkEnum(
    newTree: ProtoTree,
    newPath: ProtoCheckPath,
    newEnum: ProtoEnum,
    oldTree: ProtoTree,
    oldPath: ProtoCheckPath,
    oldEnum: ProtoEnum,
    callStack: mutable.Set[(String, String)]
  ): Vector[ProtoIncompat] = {
    if (!callStack.contains((newPath.name, oldPath.name))) {
      callStack += ((newPath.name, oldPath.name))

      // No check for now
      Vector.empty
    } else {
      Vector.empty
    }
  }

  private def isTagNumberReserved(tagNumber: Int, message: DescriptorProto): Boolean = {
    message.getReservedRangeList.asScala.exists { reservedRange =>
      tagNumber >= reservedRange.getStart && tagNumber <= reservedRange.getEnd
    }
  }

  private def isTypeCompatible(
    newType: FieldType,
    oldType: FieldType
  ): Boolean = {
    val newTypeGroup = CompatibleTypeGroups.get(newType)
    val oldTypeGroup = CompatibleTypeGroups.get(oldType)
    newTypeGroup.exists(oldTypeGroup.contains) || CompatibleTypePairs.contains((newType, oldType))
  }

  private def scalaFieldType(field: FieldDescriptorProto): String = {
    val fieldOptions = FieldOptions
      .parseFrom(field.getOptions.toByteArray, extensionRegistry)
      .getExtension(Scalapb.field)

    val matcher = JavaNameRegex.pattern.matcher(fieldOptions.getType)
    val buffer = new StringBuffer

    while (matcher.find()) {
      matcher.appendReplacement(buffer, matcher.group().split("\\.").last)
    }

    matcher.appendTail(buffer)
    buffer.toString
  }

  private def checkMessage(
    newTree: ProtoTree,
    newPath: ProtoCheckPath,
    newMessage: ProtoMessage,
    oldTree: ProtoTree,
    oldPath: ProtoCheckPath,
    oldMessage: ProtoMessage,
    callStack: mutable.Set[(String, String)]
  ): Vector[ProtoIncompat] = {
    if (!callStack.contains((newPath.name, oldPath.name))) {
      callStack += ((newPath.name, oldPath.name))

      def incompat(reason: ProtoIncompatReason): ProtoIncompat = {
        ProtoIncompat(newPath, oldPath, reason)
      }

      val newFields = newMessage.proto.getFieldList.asScala.toVector
      val oldFields = oldMessage.proto.getFieldList.asScala.toVector

      val incompats = Vector.newBuilder[ProtoIncompat]

      // Get all old fields which are not present in the new message
      val removedNotReservedFields = oldFields.filterNot { oldField =>
        newFields.exists(_.getNumber == oldField.getNumber) ||
        isTagNumberReserved(oldField.getNumber, newMessage.proto)
      }

      removedNotReservedFields.foreach { field =>
        incompats += incompat(FieldRemovedNotReserved(field.getNumber, field.getName))
      }

      // Get all fields which share the same tag number in old and new messages
      val intersectedFields = newFields.flatMap { newField =>
        oldFields.find(_.getNumber == newField.getNumber).map((newField, _))
      }

      intersectedFields.foreach {
        case (newField, oldField) =>
          val newType = newField.getType
          val oldType = oldField.getType

          if (newType == FieldType.TYPE_MESSAGE && oldType == FieldType.TYPE_MESSAGE) {
            // New
            val nextNewName = newField.getTypeName.drop(1) // drop the leading dot
            val optionalNextNewMessage = newTree.findMessage(nextNewName)

            require(
              optionalNextNewMessage.isDefined,
              s"New message with name $nextNewName doesn't exist."
            )

            // Old
            val nextOldName = oldField.getTypeName.drop(1) // drop the leading dot
            val optionalNextOldMessage = oldTree.findMessage(nextOldName)

            require(
              optionalNextOldMessage.isDefined,
              s"Old message with name $nextOldName doesn't exist."
            )

            incompats ++= checkMessage(
              newTree,
              newPath.goDeeper(newField.getNumber, nextNewName),
              optionalNextNewMessage.get,
              oldTree,
              oldPath.goDeeper(oldField.getNumber, nextOldName),
              optionalNextOldMessage.get,
              callStack
            )
          } else if (newType == oldType) {
            val newScalaType = scalaFieldType(newField)
            val oldScalaType = scalaFieldType(oldField)

            if (newScalaType != oldScalaType) {
              incompats += incompat(
                ScalaFieldTypeConflicted(
                  newField.getNumber,
                  newField.getName,
                  newScalaType,
                  oldField.getName,
                  oldScalaType
                )
              )
            }
          } else if (!isTypeCompatible(newType, oldType)) {
            incompats += incompat(
              FieldTypeConflicted(
                newField.getNumber,
                newField.getName,
                newField.getType,
                oldField.getName,
                oldField.getType
              )
            )
          }

          if (newField.getName != oldField.getName) {
            val fieldOptions = FieldOptions
              .parseFrom(newField.getOptions.toByteArray, extensionRegistry)
              .getExtension(Compat.field: GeneratedExtension[FieldOptions, Compat.FieldOptions])

            // Field renamed without any suppression
            if (fieldOptions.getRenamedFrom != oldField.getName) {
              incompats += incompat(
                FieldRenamed(
                  newField.getNumber,
                  newField.getName,
                  oldField.getName
                )
              )
            }
          }
      }

      incompats.result()
    } else {
      Vector.empty
    }
  }

  private def checkProto(
    newTree: ProtoTree,
    newPath: ProtoCheckPath,
    newProto: Either[ProtoMessage, ProtoEnum],
    oldTree: ProtoTree,
    oldPath: ProtoCheckPath,
    oldProto: Either[ProtoMessage, ProtoEnum],
    callStack: mutable.Set[(String, String)]
  ): Vector[ProtoIncompat] = {
    (newProto, oldProto) match {
      case (Left(newMessage), Left(oldMessage)) =>
        checkMessage(newTree, newPath, newMessage, oldTree, oldPath, oldMessage, callStack)

      case (Right(newEnum), Right(oldEnum)) =>
        checkEnum(newTree, newPath, newEnum, oldTree, oldPath, oldEnum, callStack)

      case (Left(_), Right(_)) =>
        require(
          requirement = false,
          s"Cannot compare new message ${newPath.name} with old enum ${oldPath.name}."
        )
        Vector.empty

      case (Right(_), Left(_)) =>
        require(
          requirement = false,
          s"Cannot compare new enum ${newPath.name} with old message ${oldPath.name}."
        )
        Vector.empty
    }
  }

  def check(
    newTree: ProtoTree,
    oldTree: ProtoTree,
    roots: Vector[(String, String)]
  ): Vector[ProtoIncompat] = {
    val callStack = mutable.Set.empty[(String, String)]

    roots.flatMap {
      case (newName, oldName) =>
        val optionalIncompats = for {
          newProto <- newTree.findProto(newName)
          oldProto <- oldTree.findProto(oldName)
        } yield {
          val newPath = ProtoCheckPath(Vector.empty, newName)
          val oldPath = ProtoCheckPath(Vector.empty, oldName)
          checkProto(newTree, newPath, newProto, oldTree, oldPath, oldProto, callStack)
        }

        optionalIncompats.getOrElse(Vector.empty)
    }
  }
}
