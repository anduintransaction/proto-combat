package anduin.protocompat.report

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto

import anduin.protocompat.check.ProtoCheckResult

trait ProtoReport {

  protected def fieldType(tpe: FieldDescriptorProto.Type): String = {
    tpe match {
      case FieldDescriptorProto.Type.TYPE_DOUBLE => "double"
      case FieldDescriptorProto.Type.TYPE_FLOAT => "float"
      case FieldDescriptorProto.Type.TYPE_INT64 => "int64"
      case FieldDescriptorProto.Type.TYPE_UINT64 => "uint64"
      case FieldDescriptorProto.Type.TYPE_INT32 => "int32"
      case FieldDescriptorProto.Type.TYPE_FIXED64 => "fixed64"
      case FieldDescriptorProto.Type.TYPE_FIXED32 => "fixed32"
      case FieldDescriptorProto.Type.TYPE_BOOL => "bool"
      case FieldDescriptorProto.Type.TYPE_STRING => "string"
      case FieldDescriptorProto.Type.TYPE_GROUP => "group"
      case FieldDescriptorProto.Type.TYPE_MESSAGE => "message"
      case FieldDescriptorProto.Type.TYPE_BYTES => "bytes"
      case FieldDescriptorProto.Type.TYPE_UINT32 => "uint32"
      case FieldDescriptorProto.Type.TYPE_ENUM => "enum"
      case FieldDescriptorProto.Type.TYPE_SFIXED32 => "sfixed32"
      case FieldDescriptorProto.Type.TYPE_SFIXED64 => "sfixed64"
      case FieldDescriptorProto.Type.TYPE_SINT32 => "sint32"
      case FieldDescriptorProto.Type.TYPE_SINT64 => "sint64"
      case _ => "unknown"
    }
  }

  def report(result: ProtoCheckResult): Unit
}
