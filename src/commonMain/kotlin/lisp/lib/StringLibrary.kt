package lisp.lib

import lisp.FunctionValue
import lisp.ParamMeta
import lisp.Position
import lisp.Scope
import lisp.coercion.coerceTo

object StringLibrary: Library {
  override fun addLib(global: Scope) {
    global["string/contains"] = object: FunctionValue {
      override val name: String = "string/contains"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("string", String::class, "string to search"),
        ParamMeta("pattern", String::class, "pattern to look for in string")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Expected two arguments to 'string/contains'")
        }

        val (strRaw, seekRaw) = args

        val seek = seekRaw?.coerceTo(String::class) ?: pos.interpretFail("Expected first argument to 'string/contains' to be a string")
        val str = strRaw?.coerceTo(String::class) ?: pos.interpretFail("Expected second argument to 'string/contains' to be a string")

        return str.contains(seek)
      }
    }

    global["string/trim"] = object: FunctionValue {
      override val name: String = "string/trim"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("string", String::class, "string trim whitespace from")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 1) {
          pos.interpretFail("Expected one argument to 'string/trim'")
        }

        val (strRaw) = args

        val str = strRaw?.coerceTo(String::class) ?: pos.interpretFail("Expected first argument to 'string/trim' to be a string")

        return str.trim()
      }
    }

    global["string/indexOf"] = object: FunctionValue {
      override val name: String = "string/indexOf"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("string", String::class, "string to search"),
        ParamMeta("pattern", String::class, "pattern to search for")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val (strRaw, patternRaw) = args

        return (strRaw as String).indexOf(patternRaw as String)
      }
    }

    global["string/substring"] = object: FunctionValue {
      override val name: String = "string/substring"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("string", String::class, "string take a slice from"),
        ParamMeta("startIndex", Int::class, "index to start at (inclusive)"),
        ParamMeta("endIndex", Int::class, "index to end at (exclusive; defaults to size of string)")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val strRaw = args[0] as String

        return strRaw.substring(args[1] as Int, (args[2] as Int?) ?: strRaw.length)
      }
    }

    global["string/replace"] = object: FunctionValue {
      override val name: String = "string/replace"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("string", String::class, "string to translate"),
        ParamMeta("pattern", String::class, "pattern to find in string"),
        ParamMeta("replacement", String::class, "value to replace pattern with")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val (str, pattern, replace) = args

        return (str as String).replace(pattern as String, replace as String)
      }
    }

    global["string/isEmpty"] = object: FunctionValue {
      override val name: String = "string/isEmpty"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("string", String::class, "string to check")
      )

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
      override val name: String = "string/split"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("string", String::class, "string to split"),
        ParamMeta("pattern", String::class, "pattern to split the string by")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Expected two arguments to 'string/split'")
        }

        val (strRaw, patternRaw) = args

        val pattern = patternRaw?.coerceTo(String::class) ?: pos.interpretFail("Expected first argument to 'string/split' to be a string")
        val str = strRaw?.coerceTo(String::class) ?: pos.interpretFail("Expected second argument to 'string/split' to be a string")

        return str.split(pattern)
      }
    }
  }
}
