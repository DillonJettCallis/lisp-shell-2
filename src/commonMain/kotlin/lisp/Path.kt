package lisp

class Path(val isAbsolute: Boolean, val frags: List<String>) {

  companion object {

    fun from(path: String): Path {
      val cleaned = path.trim().replace('\\', '/')

      return Path(cleaned.startsWith('/'), cleaned.split("/")).resolve()
    }
  }


  fun resolve(other: Path): Path {
    return Path(isAbsolute, frags + other.frags).resolve()
  }

  // replace cases of . or .. with their navigations, if possible
  fun resolve(): Path {
    val working = arrayListOf<String>()

    for (next in frags) {
      when (next) {
        "." -> {
          if (working.isEmpty()) {
            working += next
          }
        }
        ".." -> {
          if (working.isEmpty()) {
            working += next
          } else {
            when ( working.last()) {
              "." -> {
                working.removeLast()
                working += next
              }
              ".." -> working += next
              else -> {
                working.removeLast()
              }
            }
          }
        }
        else -> working += next
      }
    }

    return Path(isAbsolute, working)
  }

  fun toFile(): File = File.from(this)

  override fun toString(): String = frags.joinToString("/")

}
