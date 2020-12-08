package lisp

class Path(val frags: List<String>) {

  companion object {
    fun from(path: String): Path = Path(path.split("""[/\\]""".toRegex())).resolve()
  }


  fun resolve(other: Path): Path {
    return Path(frags + other.frags).resolve()
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

    return Path(working)
  }

  fun toFile(): File = File.from(this)

  override fun toString(): String = frags.joinToString("/")

}
