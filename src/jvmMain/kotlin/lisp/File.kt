package lisp

import java.nio.file.Paths
import java.io.File as JFile

actual class File(val src: JFile) {

  actual companion object {
    actual fun base() = from(".")

    actual fun from(path: String) = File(JFile(path).absoluteFile).toPath().resolve().toFile()

    actual fun from(path: Path): File {
      return File(JFile( (if (path.isAbsolute) "/" else "") + path.frags.joinToString("/")).absoluteFile)
    }
  }

  actual val frags
    get() = src.toPath().map { it.toString() }

  actual fun list(): List<File> {
    return src.listFiles()?.map { File(it) } ?: emptyList()
  }

  actual fun exists(): Boolean {
    return src.exists()
  }

  actual fun isFile(): Boolean {
    return src.isFile
  }

  actual fun isDir(): Boolean {
    return src.isDirectory
  }

  actual fun readText(): String {
    return src.readText()
  }

  actual fun toPath(): Path {
    return Path(true, frags)
  }

  actual override fun toString(): String = src.toString()

}
