package lisp.lib

import lisp.FunctionValue
import lisp.ParamMeta
import lisp.Position
import lisp.Scope
import lisp.runtime.Type

object StringLibrary: Library {
  override fun addLib(global: Scope) {
    global["contains"] = object: FunctionValue {
      override val name: String = "contains"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("string", Type.StringType, "string to search"),
        ParamMeta("pattern", Type.StringType, "pattern to look for in string")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val (strRaw, seekRaw) = args

        val seek = seekRaw as String
        val str = strRaw as String

        return str.contains(seek)
      }
    }

    global["trim"] = object: FunctionValue {
      override val name: String = "trim"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("string", Type.StringType, "string trim whitespace from")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val (strRaw) = args

        val str = strRaw as String

        return str.trim()
      }
    }

    global["indexOf"] = object: FunctionValue {
      override val name: String = "indexOf"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("string", Type.StringType, "string to search"),
        ParamMeta("pattern", Type.StringType, "pattern to search for")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val (strRaw, patternRaw) = args

        return (strRaw as String).indexOf(patternRaw as String)
      }
    }

    global["substring"] = object: FunctionValue {
      override val name: String = "substring"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("string", Type.StringType, "string take a slice from"),
        ParamMeta("startIndex", Type.IntegerType, "index to start at (inclusive)"),
        ParamMeta("endIndex", Type.IntegerType, "index to end at (exclusive; defaults to size of string)")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val strRaw = args[0] as String

        return strRaw.substring(args[1] as Int, (args[2] as Int?) ?: strRaw.length)
      }
    }

    global["replace"] = object: FunctionValue {
      override val name: String = "replace"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("string", Type.StringType, "string to translate"),
        ParamMeta("pattern", Type.StringType, "pattern to find in string"),
        ParamMeta("replacement", Type.StringType, "value to replace pattern with")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val (str, pattern, replace) = args

        return (str as String).replace(pattern as String, replace as String)
      }
    }

    global["isEmpty"] = object: FunctionValue {
      override val name: String = "isEmpty"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("string", Type.StringType, "string to check")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val str = args[0] as String

        return str.isEmpty()
      }
    }

    global["split"] = object: FunctionValue {
      override val name: String = "split"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("string", Type.StringType, "string to split"),
        ParamMeta("pattern", Type.StringType, "pattern to split the string by")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val (strRaw, patternRaw) = args

        val pattern = patternRaw as String
        val str = strRaw as String

        return str.split(pattern)
      }
    }
  }
}
