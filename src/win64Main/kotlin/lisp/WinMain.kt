package lisp

fun main() {
  val command = WinCommand()
  val shellInterface = WinShellInterface()
  val astEvaluator = AstEvaluator(Interpreter(command))

  val shell = Shell(shellInterface, astEvaluator)

  shell.run()
}
