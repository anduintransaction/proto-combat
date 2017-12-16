package anduin.protocompat.check

/** Represents path to a message/enum when doing proto check.
  *
  * Those paths are obtained by recursively visiting messages through their parent's field
  * (which is represented by a tag number).
  *
  * @param supers Super messages and their tag number to recursively visit deeper.
  * @param name Name of the message/enum we're currently referring to.
  */
final case class ProtoCheckPath(
  supers: Vector[(String, Int)],
  name: String
) {

  def goDeeper(tagNumber: Int, subName: String): ProtoCheckPath = {
    ProtoCheckPath(
      supers = supers :+ (name -> tagNumber),
      name = subName
    )
  }
}
