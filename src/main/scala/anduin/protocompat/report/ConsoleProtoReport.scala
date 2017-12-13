package anduin.protocompat.report

import anduin.protocompat.check.ProtoCheckResult

final class ConsoleProtoReport {

  def report(result: ProtoCheckResult): Unit = {
    println("OK lah.")
  }
}
