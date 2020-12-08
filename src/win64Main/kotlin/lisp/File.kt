package lisp

import pw.binom.*
import pw.binom.io.ByteArrayOutput
import pw.binom.io.file.isExist
import pw.binom.io.file.iterator
import pw.binom.io.file.read
import pw.binom.io.use
import pw.binom.io.file.File as BFile

actual class File(private val src: BFile) {

  actual companion object {
    actual fun base(): File = from(Environment.workDirectory)

    actual fun from(path: String): File {
      return File(BFile(path))
    }

    actual fun from(path: Path): File {
      return from(path.frags.joinToString("\\"))
    }
  }

  actual val frags: List<String>
    get() = Path.from(src.path).frags

  actual fun list(): List<File> {
    return src.iterator().asSequence().map { from(it.path) }.toList()
  }

  actual fun exists(): Boolean {
    return src.isExist
  }

  actual fun isFile(): Boolean = src.isFile
  actual fun isDir() = src.isDirectory

  actual fun readText(): String {
    val bufferPool = ByteBufferPool(10)
    val out = ByteArrayOutput()

    src.read().use {
      it.copyTo(out, bufferPool)
    }

    return out.data.asUTF8String()
  }

  actual fun toPath(): Path {
    return Path.from(src.path)
  }

  actual override fun toString(): String = src.toString()
}
