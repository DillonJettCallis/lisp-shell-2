package lisp

import java.nio.file.Paths
import java.io.File as JFile

actual class File(val src: JFile) {

  actual companion object {
    actual fun base() = File(JFile(".").absoluteFile.parentFile)

    actual fun from(path: String) = File(JFile(path))

    actual fun from(path: Path): File {
      return File(Paths.get(".", *path.frags.toTypedArray()).toFile().absoluteFile)
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
    return Path(frags)
  }

  actual override fun toString(): String = src.toString()

}
