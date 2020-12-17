package lisp

import lisp.bytecode.BytecodeInterpreter

fun main() {
  val command = WinCommand()
  val shellInterface = WinShellInterface()
  val evaluator = BytecodeEvaluator(BytecodeInterpreter(command))

  val shell = Shell(shellInterface, evaluator)

  shell.run()
}
