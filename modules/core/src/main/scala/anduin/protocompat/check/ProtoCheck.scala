package anduin.protocompat.check

import java.io.File

import cats.implicits._
import protocbridge.ProtocBridge

import anduin.protocompat.tree.ProtoTree

object ProtoCheck {

  private val tmpDir = System.getProperty("java.io.tmpdir")
  assert(tmpDir != null, "Temporary directory could not be found.")

  private val outParam = s"--dummy_out=$tmpDir"

  /** Builds a base path map to determine where a proto file comes from.
    *
    * @param protoPaths All proto paths.
    * @return A map from proto relative paths to their own base path.
    */
  private def basePaths(
    protoPaths: Vector[String]
  ): Map[String, String] = {
    protoPaths.foldMap { protoPath =>
      val protoPathFile = new File(protoPath)
      val absoluteProtoPath = protoPathFile.getAbsolutePath

      val protoFiles = ProtoFinder.inPath(protoPath)

      /** Extracts base and relative paths of a proto file.
        *
        * @param protoFile The proto file.
        * @return A tuple of 2 strings. The second string is proto base path. The first string
        *         is relative path (which is based on the base path) of the proto file.
        */
      def extractPaths(protoFile: File): (String, String) = {
        protoPathFile.toPath.relativize(protoFile.toPath).toString -> absoluteProtoPath
      }

      protoFiles.map(extractPaths).toMap
    }
  }

  /** Builds a proto tree by running protoc(1) and intercepting its metadata.
    *
    * @param protoPaths Proto paths (aka proto include paths).
    * @param protoSources Proto sources.
    * @return A proto tree.
    */
  def tree(
    protoPaths: Vector[String],
    protoSources: Vector[String]
  ): ProtoTree = {
    val protoPathParams = protoPaths.map { protoPath =>
      s"--proto_path=${new File(protoPath).getAbsolutePath}"
    }

    val generator = new DummyCodeGenerator

    ProtocBridge.runWithGenerators(
      protoc = args => com.github.os72.protocjar.Protoc.runProtoc(args.toArray),
      namedGenerators = Seq("dummy" -> generator),
      params = protoPathParams ++ Vector(outParam) ++ protoSources
    )

    ProtoTree.fromRequest(generator.request)
  }

  def check(
    newProtoPaths: Vector[String],
    newProtoSources: Vector[String],
    oldProtoPaths: Vector[String],
    oldProtoSources: Vector[String],
    roots: Vector[String]
  ): ProtoCheckResult = {
    val newBasePaths = basePaths(newProtoPaths)
    val newTree = tree(newProtoPaths, newProtoSources)

    val oldBasePaths = basePaths(oldProtoPaths)
    val oldTree = tree(oldProtoPaths, oldProtoSources)

    val rootPairs = roots.map { root =>
      val parts = root.split(":", 2)

      if (parts.size >= 2) {
        (parts(0), parts(1))
      } else {
        (root, root)
      }
    }

    val incompats = ProtoCheckInternals.check(newTree, oldTree, rootPairs)

    ProtoCheckResult(
      newBasePaths,
      newTree,
      oldBasePaths,
      oldTree,
      incompats
    )
  }
}
