package lisp

class JvmCommand: Command {
  override fun execute(cwd: File, command: String, args: List<String>, flags: Int): String {
    val process = ProcessBuilder(command, *args.toTypedArray())
      .directory(cwd.src)
      .redirectErrorStream(true)
      .start()

    process.waitFor()
    return process.inputStream.bufferedReader().readText()
  }
}
