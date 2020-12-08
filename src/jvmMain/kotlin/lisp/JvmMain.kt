package lisp

fun main() {
  val command = JvmCommand()
  val shellInterface = JvmShellInterface()

  val shell = Shell(shellInterface, command)

  shell.run()
}
