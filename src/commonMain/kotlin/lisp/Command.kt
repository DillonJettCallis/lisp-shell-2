package lisp

interface Command {

  fun execute(cwd: File, command: String, args: List<String>): String

}

object NoOpCommand: Command {
  override fun execute(cwd: File, command: String, args: List<String>): String {
    throw RuntimeException("Invalid call to the NoOpCommand execute!")
  }
}
