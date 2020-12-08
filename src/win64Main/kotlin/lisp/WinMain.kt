package lisp

fun main() {
  val command = WinCommand()
  val shellInterface = WinShellInterface()

  val shell = Shell(shellInterface, command)

  shell.run()
}
