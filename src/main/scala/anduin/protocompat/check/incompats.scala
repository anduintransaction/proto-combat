package anduin.protocompat.check

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.{Type => FieldType}

final case class ProtoIncompat(
  newPath: ProtoCheckPath,
  oldPath: ProtoCheckPath,
  reason: ProtoIncompatReason
)

sealed abstract class ProtoIncompatReason

final case class FieldRemovedNotReserved(
  oldTagNumber: Int,
  oldFieldName: String
) extends ProtoIncompatReason

final case class FieldTypeConflicted(
  tagNumber: Int,
  newFieldName: String,
  newFieldType: FieldType,
  oldFieldName: String,
  oldFieldType: FieldType
) extends ProtoIncompatReason

final case class ScalaFieldTypeConflicted(
  tagNumber: Int,
  oldFieldName: String,
  oldScalaFieldType: String,
  newFieldName: String,
  newScalaFieldType: String
) extends ProtoIncompatReason

final case class FieldRenamed(
  tagNumber: Int,
  oldFieldName: String,
  newFieldName: String
) extends ProtoIncompatReason
