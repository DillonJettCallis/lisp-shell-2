package lisp.lib

import lisp.*
import lisp.runtime.Type

interface Library {
  fun addLib(global: Scope)
}

object CoreLibrary: Library {

  fun coreLib(): Scope {
    val global = Scope(ScopeKind.global, null)


    val libs = listOf(
      CoreLibrary,
      MathLibrary,
      StringLibrary,
      ArrayLibrary,
      MapLibrary,
      PathLibrary,
      FileLibrary,
      ParseLibrary
    )

    libs.forEach { it.addLib(global) }

    return global
  }

  override fun addLib(global: Scope) {
    global["cwd"] = File.base()

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

      override fun call(args: List<Any?>, pos: Position): Any? {
        val (typeNameRaw, value) = args

        val type = typeNameRaw as Type

        if (value == null) {
          return false
        }

        return type == Type.typeOf(value)
      }
    }

    global["&"] = object : FunctionValue, OperatorFunctionValue {
      override val name: String = "&"
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Any? {
        return args.joinToString("")
      }
    }

    global["+"] = object : FunctionValue, OperatorFunctionValue {
      override val name: String = "+"
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Any? {
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

    global["-"] = object : FunctionValue, OperatorFunctionValue {
      override val name: String = "-"
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Any? {
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

    global["*"] = object : FunctionValue, OperatorFunctionValue {
      override val name: String = "*"
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Any? {
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

    global["/"] = object : FunctionValue, OperatorFunctionValue {
      override val name: String = "/"
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Any? {
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

    global["=="] = object : FunctionValue, OperatorFunctionValue {
      override val name: String = "=="
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Invalid args to ==")
        }

        val (first, second) = args

        return first == second
      }
    }

    global["!="] = object : FunctionValue, OperatorFunctionValue {
      override val name: String = "!="
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Invalid args to !=")
        }

        val (first, second) = args

        return first != second
      }
    }

    global["<"] = object : FunctionValue, OperatorFunctionValue {
      override val name: String = "<"
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Any? {
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

    global["<="] = object : FunctionValue, OperatorFunctionValue {
      override val name: String = "<="
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Any? {
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

    global[">"] = object : FunctionValue, OperatorFunctionValue {
      override val name: String = ">"
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Any? {
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

    global[">="] = object : FunctionValue, OperatorFunctionValue {
      override val name: String = ">="
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Any? {
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

    global["!"] = object : FunctionValue, OperatorFunctionValue {
      override val name: String = "!"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("value", Type.BooleanType, "boolean value to invert")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val value = args[0] as Boolean

        return !value
      }
    }
  }


}
