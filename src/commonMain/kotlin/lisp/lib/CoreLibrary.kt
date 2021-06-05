package lisp.lib

import lisp.*
import lisp.runtime.Type

interface Library {
  fun addLib(global: Scope)
}

object CoreLibrary: Library {

  val nativeLibs = mapOf(
    "math" to MathLibrary,
    "string" to StringLibrary,
    "array" to ArrayLibrary,
    "map" to MapLibrary,
    "path" to PathLibrary,
    "file" to FileLibrary,
    "parse" to ParseLibrary
  )

  fun coreLib(): Scope {
    val global = Scope(ScopeKind.global, null)

    addLib(global)

    return global
  }

  override fun addLib(global: Scope) {
    global["cwd"] = File.base()

    global["(arrayBuild)"] = object: FunctionValue {
      override val name: String = "(arrayBuild)"
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): ArrayList<Any?> {
        return ArrayList()
      }
    }

    global["(arrayMutableAdd)"] = object: FunctionValue {
      override val name: String = "(arrayMutableAdd)"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("array", Type.ArrayType, "array to add to"),
        ParamMeta("next", Type.AnyType, "next item to add")
      )

      override fun call(args: List<Any?>, pos: Position): MutableList<Any?> {
        val arr = args[0] as MutableList<Any?>
        arr.add(args[1])
        return arr
      }
    }

    global["(mapBuild)"] = object : FunctionValue {
      override val name: String = "(mapBuild)"
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): HashMap<Any?, Any?> {
        return HashMap()
      }
    }

    global["(mapMutableSet)"] = object : FunctionValue {
      override val name: String = "(mapMutableSet)"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("map", Type.MapType, "map to mutate"),
        ParamMeta("key", Type.AnyType, "key to insert"),
        ParamMeta("value", Type.AnyType, "value to insert")
      )

      override fun call(args: List<Any?>, pos: Position): MutableMap<Any?, Any?> {
        val (rawMap, key, value) = args
        val map = rawMap as MutableMap<Any?, Any?>
        map[key] = value
        return map
      }
    }

    global["as"] = object: FunctionValue {
      override val name: String = "as"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("type", Type.TypeType, "name of type to coerce to"),
        ParamMeta("value", Type.AnyType, "value to coerce")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val (typeNameRaw, value) = args

        val type = typeNameRaw as Type

        if (value == null) {
          return null
        }

        return Type.coerce(type, value)
      }
    }

    global["is"] = object: FunctionValue {
      override val name: String = "is"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("type", Type.TypeType, "name of type to check for"),
        ParamMeta("value", Type.AnyType, "value to check")
      )

      override fun call(args: List<Any?>, pos: Position): Boolean {
        val (typeNameRaw, value) = args

        val type = typeNameRaw as Type

        if (value == null) {
          return false
        }

        return type == Type.typeOf(value)
      }
    }

    global["."] = object : FunctionValue {
      override val name: String = "."
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): String {
        return args.joinToString("")
      }
    }

    global["+"] = object : FunctionValue {
      override val name: String = "+"
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Any {
        if (args.size != 2) {
          pos.interpretFail("Invalid args to +")
        }

        val (first, second) = args

        return when {
          first is Int && second is Int -> first + second
          first is Number && second is Number -> first.toDouble() + second.toDouble()
          else -> pos.interpretFail("Invalid args to +")
        }
      }
    }

    global["-"] = object : FunctionValue {
      override val name: String = "-"
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Any {
        return when (args.size) {
          1 -> {
            when (val value = args[0]) {
              is Int -> -value
              is Double -> -value
              else -> pos.interpretFail("Invalid args to -")
            }
          }
          2 -> {
            val (first, second) = args
            when {
              first is Int && second is Int -> first - second
              first is Number && second is Number -> first.toDouble() - second.toDouble()
              else -> pos.interpretFail("Invalid args to -")
            }
          }
          else -> {
            pos.interpretFail("Invalid args to -")
          }
        }
      }
    }

    global["*"] = object : FunctionValue {
      override val name: String = "*"
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Any {
        if (args.size != 2) {
          pos.interpretFail("Invalid args to *")
        }

        val (first, second) = args

        return when {
          first is Int && second is Int -> first * second
          first is Number && second is Number -> first.toDouble() * second.toDouble()
          else -> pos.interpretFail("Invalid args to *")
        }
      }
    }

    global["/"] = object : FunctionValue {
      override val name: String = "/"
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Any {
        if (args.size != 2) {
          pos.interpretFail("Invalid args to /")
        }

        val (firstRaw, secondRaw) = args

        val first = when (firstRaw) {
          is Int -> firstRaw.toDouble()
          is Number -> firstRaw.toDouble()
          else -> pos.interpretFail("Invalid args to /")
        }

        val second = when (secondRaw) {
          is Int -> secondRaw.toDouble()
          is Number -> secondRaw.toDouble()
          else -> pos.interpretFail("Invalid args to /")
        }

        return first / second
      }
    }

    global["=="] = object : FunctionValue {
      override val name: String = "=="
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Boolean {
        if (args.size != 2) {
          pos.interpretFail("Invalid args to ==")
        }

        val (first, second) = args

        return first == second
      }
    }

    global["!="] = object : FunctionValue {
      override val name: String = "!="
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Boolean {
        if (args.size != 2) {
          pos.interpretFail("Invalid args to !=")
        }

        val (first, second) = args

        return first != second
      }
    }

    global["<"] = object : FunctionValue {
      override val name: String = "<"
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Boolean {
        if (args.size != 2) {
          pos.interpretFail("Invalid args to <")
        }

        val (first, second) = args

        return when {
          first is Int && second is Int -> first < second
          first is Number && second is Number -> first.toDouble() < second.toDouble()
          first is String && second is String -> first < second
          else -> pos.interpretFail("Invalid args to <")
        }
      }
    }

    global["<="] = object : FunctionValue {
      override val name: String = "<="
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Boolean {
        if (args.size != 2) {
          pos.interpretFail("Invalid args to <=")
        }

        val (first, second) = args

        return when {
          first is Int && second is Int -> first <= second
          first is Number && second is Number -> first.toDouble() <= second.toDouble()
          first is String && second is String -> first <= second
          else -> pos.interpretFail("Invalid args to <=")
        }
      }
    }

    global[">"] = object : FunctionValue {
      override val name: String = ">"
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Boolean {
        if (args.size != 2) {
          pos.interpretFail("Invalid args to >")
        }

        val (first, second) = args

        return when {
          first is Int && second is Int -> first > second
          first is Number && second is Number -> first.toDouble() > second.toDouble()
          first is String && second is String -> first > second
          else -> pos.interpretFail("Invalid args to >")
        }
      }
    }

    global[">="] = object : FunctionValue {
      override val name: String = ">="
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Any {
        if (args.size != 2) {
          pos.interpretFail("Invalid args to >=")
        }

        val (first, second) = args

        return when {
          first is Int && second is Int -> first >= second
          first is Number && second is Number -> first.toDouble() >= second.toDouble()
          first is String && second is String -> first >= second
          else -> pos.interpretFail("Invalid args to >=")
        }
      }
    }

    global["not"] = object : FunctionValue {
      override val name: String = "not"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("value", Type.BooleanType, "boolean value to invert")
      )

      override fun call(args: List<Any?>, pos: Position): Boolean {
        val value = args[0] as Boolean

        return !value
      }
    }

    global["or"] = object : FunctionValue {
      override val name: String = "or"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("left", Type.BooleanType, "boolean value to compare"),
        ParamMeta("right", Type.BooleanType, "boolean value to compare")
      )

      override fun call(args: List<Any?>, pos: Position): Boolean {
        val (rawLeft, rawRight) = args

        return (rawLeft as Boolean) || (rawRight as Boolean)
      }
    }

    global["and"] = object : FunctionValue {
      override val name: String = "and"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("left", Type.BooleanType, "boolean value to compare"),
        ParamMeta("right", Type.BooleanType, "boolean value to compare")
      )

      override fun call(args: List<Any?>, pos: Position): Boolean {
        val (rawLeft, rawRight) = args

        return (rawLeft as Boolean) && (rawRight as Boolean)
      }
    }

    global["do"] = object : FunctionValue {
      override val name: String = "do"
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Any? {
        return args.lastOrNull()
      }
    }
  }


}
