package lisp.lib

import lisp.*
import lisp.bytecode.BytecodeInterpreter
import lisp.coercion.CoercionRegistry
import lisp.coercion.coerceTo
import kotlin.reflect.KClass

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

    global["include"] = object: SpecialFunctionValue {
      override val name = "include"
      override val params = listOf(ParamMeta("path", File::class, "path to file to include"))

      override fun call(scope: Scope, args: List<Any?>, pos: Position): Any? {
        if (args.size != 1) {
          pos.interpretFail("Expected include to have exactly 1 argument")
        }

        val evaluator = scope.getGlobal("(evaluator)") as Evaluator
        val file = args[0] as File

        if (file.exists()) {
          val raw = file.readText()
          val moduleScope = global.child(ScopeKind.module)

          evaluator.evaluate(moduleScope, raw, file.toString())

          // slap all values into the current module scope
          moduleScope.all().forEach { (key, value) -> scope.define(key, value) }

          return null
        } else {
          pos.interpretFail("Could not include from path - no such file '${file}' exists")
        }
      }
    }

    global["as"] = object: FunctionValue {
      override val name: String = "as"
      override val params: List<ParamMeta> = listOf(ParamMeta("typeName", String::class, "name of type to coerce to"), ParamMeta("value", Any::class, "value to coerce"))

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Expected as to have exactly 2 arguments")
        }

        val (typeNameRaw, value) = args

        val typeName = typeNameRaw?.coerceTo(String::class) ?: pos.interpretFail("Expected first argument to as to be a string")

        if (value == null) {
          return null
        }

        return CoercionRegistry.tryCoerce(value, typeName) ?: pos.interpretFail("Failed to coerce value")
      }
    }

    global["is"] = object: FunctionValue {
      override val name: String = "is"
      override val params: List<ParamMeta> = listOf(ParamMeta("typeName", String::class, "name of type to check for"), ParamMeta("value", Any::class, "value to check"))

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Expected is to have exactly 2 arguments")
        }

        val (typeNameRaw, value) = args

        val typeName = typeNameRaw?.coerceTo(String::class) ?: pos.interpretFail("Expected first argument to is to be a string")

        if (value == null) {
          return false
        }

        return CoercionRegistry.checkType(value, typeName)
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

        val first = firstRaw?.coerceTo(Double::class) ?: pos.interpretFail("Invalid args to /")
        val second = secondRaw?.coerceTo(Double::class) ?: pos.interpretFail("Invalid args to /")

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
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 1) {
          pos.interpretFail("Invalid args to !")
        }

        val value = args.single()?.coerceTo(Boolean::class) ?: pos.interpretFail("Invalid args to !")

        return !value
      }
    }
  }


}
