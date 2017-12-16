package anduin.protocompat.check

import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos.{CodeGeneratorRequest, CodeGeneratorResponse}
import com.trueaccord.scalapb.Scalapb
import protocbridge.ProtocCodeGenerator

/** A Protocol Buffers code generator that does nothing
  * but capturing the passed [[CodeGeneratorRequest request]] each time it runs.
  */
private[check] final class DummyCodeGenerator extends ProtocCodeGenerator { self =>

  var request: CodeGeneratorRequest = CodeGeneratorRequest.getDefaultInstance

  def run(request: Array[Byte]): Array[Byte] = {
    val registry = ExtensionRegistry.newInstance()
    Scalapb.registerAllExtensions(registry)
    self.request = CodeGeneratorRequest.parseFrom(request, registry)
    CodeGeneratorResponse.getDefaultInstance.toByteArray
  }
}
