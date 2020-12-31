package lisp

expect class File {

  companion object {
    fun base(): File

    fun from(path: String): File

    fun from(path: Path): File
  }

  val frags: List<String>

  fun list(): List<File>

  fun exists(): Boolean
  fun isFile(): Boolean
  fun isDir(): Boolean

  fun readText(): String

  fun toPath(): Path

  override fun toString(): String

}

fun File.resolve(path: Path): File = toPath().resolve(path).toFile()

fun File.parent(): File = toPath().resolve(Path.from("..")).toFile()
