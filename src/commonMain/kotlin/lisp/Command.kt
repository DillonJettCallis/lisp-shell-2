package lisp

interface Command {

  fun execute(cwd: File, command: String, args: List<String>): String

}
