package anduin.protocompat.report

import anduin.protocompat.check.ProtoCheckResult

trait ProtoReport {
  def report(result: ProtoCheckResult): Unit
}
