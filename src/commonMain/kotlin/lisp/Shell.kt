package lisp

import lisp.coercion.coerceTo
import lisp.lib.CoreLibrary

interface ShellInterface {

  fun read(): String

  fun write(content: String)

  fun writeln(line: String) = write("$line\n")

}

class Shell(private val sh: ShellInterface, command: Command) {

  private val interpreter = Interpreter(command)

  fun run() {
    val globalScope = CoreLibrary.coreLib()
    val shellScope = globalScope.child(ScopeKind.shell)
    val resultScope = shellScope.child(ScopeKind.shell)
    val moduleScope = resultScope.child(ScopeKind.module)

    var resultIndex = 0
    var lineIndex = 0
    var exit = false

    shellScope["exit"] = object: FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        exit = true
        return null
      }
    }

    shellScope["echo"] = object: FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        return args.joinToString(" ") { it.toString() }
      }
    }

    shellScope["clearResults"] = object: FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        resultIndex = 0
        resultScope.clear()
        return null
      }
    }

    shellScope["clearDefs"] = object: FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        resultIndex = 0
        resultScope.clear()
        moduleScope.clear()
        return null
      }
    }


    while (!exit) {
      val cwd = cd(moduleScope)
      sh.writeln(cwd.toString())
      sh.write("λ ")

      val nextLine = sh.read()

      try {
        lineIndex++

        val result = interpreter.evaluate(moduleScope, nextLine, "shell$lineIndex", autoWrap = true)

        if (result != null && result != "") {
          val index = resultIndex
          resultScope["result$index"] = result
          resultIndex++

          val strResult = result.toString()

          if (strResult.contains('\n')) {
            sh.writeln("result$index:\n$strResult")
          } else {
            sh.writeln("result$index: $strResult")
          }

        } else {
          sh.writeln("")
        }
      } catch (e: Exception) {
        sh.writeln(e.message ?: "unknown exception")
      }
    }
  }

  private fun cd(moduleScope: Scope): File {
    return moduleScope["cwd"]?.coerceTo(File::class) ?: run {
      sh.writeln("\$cwd is not a file. Resetting \$cwd to current directory")
      val cwd = File.base()
      moduleScope.delete("cwd")
      moduleScope.setGlobal("cwd", cwd)
      cwd
    }
  }
}
