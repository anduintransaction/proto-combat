package anduin.protocompat.check

import anduin.protocompat.tree.ProtoTree

final case class ProtoCheckResult(
  newBasePaths: Map[String, String],
  newTree: ProtoTree,
  oldBasePaths: Map[String, String],
  oldTree: ProtoTree,
  incompats: Vector[ProtoIncompat]
)
