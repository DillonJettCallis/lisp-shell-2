package lisp.lib

import lisp.FunctionValue
import lisp.Position
import lisp.Scope
import lisp.coercion.coerceTo

object StringLibrary: Library {
  override fun addLib(global: Scope) {
    global["string/contains"] = object: FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Expected two arguments to 'string/contains'")
        }

        val (seekRaw, strRaw) = args

        val seek = seekRaw?.coerceTo(String::class) ?: pos.interpretFail("Expected first argument to 'string/contains' to be a string")
        val str = strRaw?.coerceTo(String::class) ?: pos.interpretFail("Expected second argument to 'string/contains' to be a string")

        return str.contains(seek)
      }
    }

    global["string/trim"] = object: FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 1) {
          pos.interpretFail("Expected one argument to 'string/trim'")
        }

        val (strRaw) = args

        val str = strRaw?.coerceTo(String::class) ?: pos.interpretFail("Expected first argument to 'string/trim' to be a string")

        return str.trim()
      }
    }

    global["string/isEmpty"] = object: FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 1) {
          pos.interpretFail("Expected one argument to 'string/isEmpty'")
        }

        val (strRaw) = args

        val str = strRaw?.coerceTo(String::class) ?: pos.interpretFail("Expected first argument to 'string/isEmpty' to be a string")

        return str.isEmpty()
      }
    }

    global["string/split"] = object: FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Expected two arguments to 'string/split'")
        }

        val (patternRaw, strRaw) = args

        val pattern = patternRaw?.coerceTo(String::class) ?: pos.interpretFail("Expected first argument to 'string/split' to be a string")
        val str = strRaw?.coerceTo(String::class) ?: pos.interpretFail("Expected second argument to 'string/split' to be a string")

        return str.split(pattern)
      }
    }
  }
}
