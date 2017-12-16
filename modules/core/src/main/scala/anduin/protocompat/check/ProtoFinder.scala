package anduin.protocompat.check

import java.io.File

import cats.implicits._

object ProtoFinder {

  private def listFiles(directory: File): Vector[File] = {
    val files = directory.listFiles()
    assert(files != null, s"Cannot list files inside ${directory.getAbsolutePath}.")
    files.toVector
  }

  private def inDirectory(dir: File): Vector[File] = {
    val files = listFiles(dir)

    files.flatMap { file =>
      if (file.isDirectory) {
        inDirectory(file)
      } else if (file.isFile && file.getName.endsWith(".proto")) {
        Vector(file)
      } else {
        Vector.empty
      }
    }
  }

  def inPath(path: String): Vector[File] = {
    val file = new File(path)
    require(file.isDirectory, s"${file.getAbsolutePath} is not a valid directory.")
    inDirectory(file)
  }
}
