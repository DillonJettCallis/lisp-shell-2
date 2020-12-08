package lisp.lib

import lisp.FunctionValue
import lisp.Path
import lisp.Position
import lisp.Scope
import lisp.coercion.coerceTo

object PathLibrary: Library {

  override fun addLib(global: Scope) {
    global["path"] = object: FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 1) {
          pos.interpretFail("Expected one argument to 'path'")
        }

        val (strRaw) = args

        val str = strRaw?.coerceTo(String::class) ?: pos.interpretFail("Expected first argument to 'path' to be a string")

        return Path.from(str)
      }
    }
  }

}
