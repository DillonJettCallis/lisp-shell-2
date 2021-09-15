package lisp

interface Command {

  fun execute(cwd: File, command: String, args: List<String>, flags: Int): String

  companion object {
    const val stdOut = 1
    const val stdErr = 2

    val flagMap = mapOf(
      "stdOut" to stdOut,
      "stdErr" to stdErr,
    )

    val defaultFlags = setOf("stdOut")

    fun evalFlags(flags: Set<String>, pos: Position): Int {
      var result = 0

      flags.forEach {
        val flag = flagMap[it] ?: pos.interpretFail("Invalid flag passed to exec: '$it'")

        result = result and flag
      }

      return result
    }
  }

}

object NoOpCommand: Command {
  override fun execute(cwd: File, command: String, args: List<String>, flags: Int): String {
    throw RuntimeException("Invalid call to the NoOpCommand execute!")
  }
}
