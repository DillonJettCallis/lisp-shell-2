package lisp

import lisp.bytecode.BytecodeInterpreter

fun main() {
  val command = JvmCommand()
  val shellInterface = JvmShellInterface()
  val bytecodeEvaluator = BytecodeEvaluator(
    BytecodeInterpreter(command)
  )

  val shell = Shell(shellInterface, bytecodeEvaluator)

  shell.run()
}
