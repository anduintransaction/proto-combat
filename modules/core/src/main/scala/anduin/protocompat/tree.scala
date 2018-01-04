package anduin.protocompat

import scala.collection.JavaConverters._

import cats.Monoid
import cats.implicits._
import com.google.protobuf.DescriptorProtos.{
  DescriptorProto,
  EnumDescriptorProto,
  FileDescriptorProto
}
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest

object tree {

  // Note that this is not a lawful implementation
  private implicit def overrideMapMonoid[A, B]: Monoid[Map[A, B]] = new Monoid[Map[A, B]] {
    final def empty: Map[A, B] = Map.empty
    final def combine(x: Map[A, B], y: Map[A, B]): Map[A, B] = x ++ y
  }

  final case class ProtoTree(
    fileProtos: Vector[FileDescriptorProto]
  ) {

    lazy val files: Map[String, ProtoFile] = fileProtos.map { fileProto =>
      fileProto.getName -> ProtoFile(fileProto)
    }.toMap

    lazy val flattenMessagesAndFile: Map[String, (ProtoMessage, ProtoFile)] =
      files.values.toVector.foldMap { file =>
        file.flattenMessages.mapValues(_ -> file)
      }

    def findMessageAndFile(canonicalName: String): Option[(ProtoMessage, ProtoFile)] = {
      flattenMessagesAndFile
        .collectFirst {
          case (name, (message, file)) if name == canonicalName =>
            (message, file)
        }
        .orElse(
          flattenMessagesAndFile.collectFirst {
            case (name, (message, file)) if name.split('.').last == canonicalName =>
              (message, file)
          }
        )
    }

    def findMessage(canonicalName: String): Option[ProtoMessage] = {
      findMessageAndFile(canonicalName).map { case (message, _) => message }
    }

    lazy val flattenEnumsAndFile: Map[String, (ProtoEnum, ProtoFile)] = files.values.toVector
      .foldMap { file =>
        file.flattenEnums.mapValues(_ -> file)
      }

    def findEnumAndFile(canonicalName: String): Option[(ProtoEnum, ProtoFile)] = {
      flattenEnumsAndFile
        .collectFirst {
          case (name, (enum, file)) if name == canonicalName =>
            (enum, file)
        }
        .orElse(
          flattenEnumsAndFile.collectFirst {
            case (name, (enum, file)) if name.split('.').last == canonicalName =>
              (enum, file)
          }
        )
    }

    def findEnum(canonicalName: String): Option[ProtoEnum] = {
      findEnumAndFile(canonicalName).map { case (enum, file) => enum }
    }

    def findProtoAndFile(
      canonicalName: String
    ): Option[(Either[ProtoMessage, ProtoEnum], ProtoFile)] = {
      findMessageAndFile(canonicalName)
        .map {
          case (message, file) => (Left(message), file)
        }
        .orElse(
          findEnumAndFile(canonicalName).map { case (enum, file) => (Right(enum), file) }
        )
    }

    def findProto(canonicalName: String): Option[Either[ProtoMessage, ProtoEnum]] = {
      findProtoAndFile(canonicalName).map { case (messageOrEnum, _) => messageOrEnum }
    }
  }

  object ProtoTree {

    def fromRequest(request: CodeGeneratorRequest): ProtoTree = {
      ProtoTree(request.getProtoFileList.asScala.toVector)
    }
  }

  final case class ProtoFile(
    proto: FileDescriptorProto
  ) {

    lazy val messages: Map[String, ProtoMessage] =
      proto.getMessageTypeList.asScala.map { messageProto =>
        messageProto.getName -> ProtoMessage(messageProto)
      }.toMap

    lazy val enums: Map[String, ProtoEnum] = proto.getEnumTypeList.asScala.map { enumProto =>
      enumProto.getName -> ProtoEnum(enumProto)
    }.toMap

    private[this] def concatPrefix(prefix: String, name: String): String = {
      if (prefix.nonEmpty) {
        s"$prefix.$name"
      } else {
        name
      }
    }

    private[this] def findFlattenMessages(
      message: ProtoMessage,
      prefix: String
    ): Map[String, ProtoMessage] = {
      val ownPrefix = concatPrefix(prefix, message.proto.getName)

      message.messages.values.toVector
        .foldMap(findFlattenMessages(_, ownPrefix)) + (ownPrefix -> message)
    }

    lazy val flattenMessages: Map[String, ProtoMessage] =
      messages.values.toVector.foldMap(findFlattenMessages(_, proto.getPackage))

    private[this] def findFlattenEnums(
      message: ProtoMessage,
      prefix: String
    ): Map[String, ProtoEnum] = {
      val ownPrefix = concatPrefix(prefix, message.proto.getName)

      message.messages.values.toVector
        .foldMap(findFlattenEnums(_, ownPrefix))
    }

    lazy val flattenEnums: Map[String, ProtoEnum] = enums.values.map { enum =>
      concatPrefix(proto.getPackage, enum.proto.getName) -> enum
    }.toMap ++
      messages.values.toVector.foldMap(findFlattenEnums(_, proto.getPackage))
  }

  final case class ProtoMessage(
    proto: DescriptorProto
  ) {

    lazy val messages: Map[String, ProtoMessage] =
      proto.getNestedTypeList.asScala.map { messageProto =>
        messageProto.getName -> ProtoMessage(messageProto)
      }.toMap

    lazy val enums: Map[String, ProtoEnum] = proto.getEnumTypeList.asScala.map { enumProto =>
      enumProto.getName -> ProtoEnum(enumProto)
    }.toMap
  }

  final case class ProtoEnum(
    proto: EnumDescriptorProto
  )
}
