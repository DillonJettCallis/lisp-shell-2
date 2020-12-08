package lisp.lib

import lisp.*
import lisp.coercion.CoercionRegistry
import lisp.coercion.coerceTo

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

    global["cd"] = object: MacroFunctionValue {
      override fun call(interpreter: Interpreter, scope: Scope, args: List<Expression>, pos: Position): Any? {
        if (args.size != 1) {
          pos.interpretFail("Invalid args to 'cd'")
        }

        val prev = scope.getGlobal("cwd")?.coerceTo(Path::class) ?: pos.interpretFail("Expected cwd to be a Path object")

        val relative = interpreter.interpret(scope, args[0])?.coerceTo(Path::class) ?: pos.interpretFail("Expected first arg to cd to be a string")

        val result = prev.resolve(relative)
        scope.setGlobal("cwd", result)
        return result
      }
    }

    global["def"] = object: MacroFunctionValue {
      override fun call(interpreter: Interpreter, scope: Scope, args: List<Expression>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Invalid args to 'dev'")
        }

        val (name, body) = args

        if (name !is VariableEx) {
          name.pos.interpretFail("Expected variable declaration")
        }

        val value = interpreter.interpret(scope, body)

        scope.define(name.name, value)
        return value
      }
    }

    global["fn"] = object: MacroFunctionValue {
      override fun call(interpreter: Interpreter, scope: Scope, args: List<Expression>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Invalid args to 'fn'")
        }

        val (vars, body) = args

        if (vars !is ArrayEx) {
          vars.pos.interpretFail("Expected arguments array for function")
        }

        val varNames = vars.body.map { ((it as? VariableEx) ?: it.pos.interpretFail("Expected variable declaration")).name }

        return object: FunctionValue {
          override fun call(args: List<Any?>, pos: Position): Any? {
            val childScope = scope.child()

            args.forEachIndexed { index, value ->
              childScope["$index"] = value

              if (index < varNames.size) {
                childScope[varNames[index]] = value
              }
            }

            childScope["*"] = args

            return interpreter.interpret(childScope, body)
          }
        }
      }
    }

    global["defn"] = object: MacroFunctionValue {
      override fun call(interpreter: Interpreter, scope: Scope, args: List<Expression>, pos: Position): Any? {
        if (args.size != 3) {
          pos.interpretFail("Invalid args to 'defn'")
        }

        val (name, vars, body) = args

        return interpreter.interpret(scope,
          CallEx(listOf(OperatorEx("def", pos), name, CallEx(listOf(OperatorEx("fn", pos), vars, body), pos)), pos)
        )
      }
    }

    global["let"] = object : MacroFunctionValue {
      override fun call(interpreter: Interpreter, scope: Scope, args: List<Expression>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Invalid args to 'let'")
        }

        val (declares, body) = args

        if (declares !is MapEx) {
          declares.pos.interpretFail("Expected map of variables")
        }

        val childScope = scope.child()

        declares.body.forEach { (keyEx, valEx) ->
          if (keyEx !is VariableEx) {
            keyEx.pos.interpretFail("Expected variable declaration")
          }

          childScope[keyEx.name] = interpreter.interpret(childScope, valEx)
        }

        return interpreter.interpret(childScope, body)
      }
    }

    global["if"] = object : MacroFunctionValue {
      override fun call(interpreter: Interpreter, scope: Scope, args: List<Expression>, pos: Position): Any? {
        if (args.size != 3) {
          pos.interpretFail("Invalid args to 'if'")
        }

        val (condition, thenEx, elseEx) = args

        val maybeBool = interpreter.interpret(scope, condition)?.coerceTo(Boolean::class) ?: condition.pos.interpretFail("Expected if condition to be boolean")

        return if (maybeBool) {
          interpreter.interpret(scope, thenEx)
        } else {
          interpreter.interpret(scope, elseEx)
        }
      }
    }

    global["do"] = object: FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        return args.lastOrNull()
      }
    }

    global["include"] = object: MacroFunctionValue {
      override fun call(interpreter: Interpreter, scope: Scope, args: List<Expression>, pos: Position): Any? {
        val params = args.map { interpreter.interpret(scope, it) }

        if (params.size != 1) {
          pos.interpretFail("Expected include to have exactly 1 argument")
        }

        val include = params.single()?.coerceTo(File::class) ?: pos.interpretFail("Expected first argument to include to be a file")

        if (include.exists()) {
          val raw = include.readText()

          return interpreter.evaluate(scope, raw, include.toString())
        } else {
          pos.interpretFail("Could not include from path - no such file '${include}' exists")
        }
      }
    }

    global["as"] = object: FunctionValue {
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

    global["call"] = object: MacroFunctionValue {
      override fun call(interpreter: Interpreter, scope: Scope, args: List<Expression>, pos: Position): Any? {
        val params = args.map { interpreter.interpret(scope, it) }

        if (params.size != 2) {
          pos.interpretFail("Expected exactly 2 arguments to 'call'")
        }

        val (func, metaArgsRaw) = params

        val metaArgs = metaArgsRaw?.coerceTo(List::class) ?: pos.interpretFail("call expected second argument to be array")

        return when (func) {
          is MacroFunctionValue -> {
            // Calling a macro dynamically is weird. We can't pass the genuine syntax since it's dynamic
            // so instead we hack it - make a new scope and create vars for all the args and pass that
            // the macro is tricked into thinking all of it's args are just variables, which should
            // work most of the time. Obviously not always, but using call on 'defn', 'if' or something like that
            // would be really weird and not at all helpful. This is mostly so that it will work on commands
            // that need to be macros to get access to scope but not anything else
            val childScope = scope.child()
            var index = 0

            val varArgs = metaArgs.map {
              // var names have () in them to make them totally unrepresentable in syntax.
              // brackets, quotes and whitespace are the only illegal values in an identifier
              val hiddenName = "(hidden${index})"
              childScope[hiddenName] = it
              index++
              VariableEx(hiddenName, pos)
            }

            func.call(interpreter, childScope, varArgs, pos)
          }
          is FunctionValue -> func.call(metaArgs, args[0].pos)
          else -> pos.interpretFail("call expected first argument to be function")
        }
      }
    }

    global["\\"] = object: MacroFunctionValue, OperatorFunctionValue {
      override val op: String = "\\"

      override fun call(interpreter: Interpreter, scope: Scope, args: List<Expression>, pos: Position): Any? {
        return interpreter.interpret(scope,
          CallEx(listOf(OperatorEx("fn", pos), ArrayEx(emptyList(), pos), CallEx(args, pos)), pos)
        )
      }
    }

    global["&"] = object : FunctionValue, OperatorFunctionValue {
      override val op: String = "&"

      override fun call(args: List<Any?>, pos: Position): Any? {
        return args.joinToString("")
      }
    }

    global["+"] = object : FunctionValue, OperatorFunctionValue {
      override val op: String = "+"

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
      override val op: String = "-"

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
      override val op: String = "*"

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
      override val op: String = "/"

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
      override val op: String = "=="

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Invalid args to ==")
        }

        val (first, second) = args

        return first == second
      }
    }

    global["!="] = object : FunctionValue, OperatorFunctionValue {
      override val op: String = "!="

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Invalid args to !=")
        }

        val (first, second) = args

        return first != second
      }
    }

    global["<"] = object : FunctionValue, OperatorFunctionValue {
      override val op: String = "<"

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
      override val op: String = "<="

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
      override val op: String = ">"

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
      override val op: String = ">="

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
      override val op: String = "!"

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 1) {
          pos.interpretFail("Invalid args to !")
        }

        val value = args.single()?.coerceTo(Boolean::class) ?: pos.interpretFail("Invalid args to !")

        return !value
      }
    }

    global["&&"] = object : MacroFunctionValue, OperatorFunctionValue {
      override val op: String = "&&"

      override fun call(interpreter: Interpreter, scope: Scope, args: List<Expression>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Invalid args to &&")
        }

        val (firstEx, secondEx) = args

        val first = interpreter.interpret(scope, firstEx)?.coerceTo(Boolean::class) ?: pos.interpretFail("Invalid args to &&")

        return if (first) {
          interpreter.interpret(scope, secondEx)?.coerceTo(Boolean::class) ?: pos.interpretFail("Invalid args to &&")
        } else {
          false
        }
      }
    }

    global["||"] = object : MacroFunctionValue, OperatorFunctionValue {
      override val op: String = "||"

      override fun call(interpreter: Interpreter, scope: Scope, args: List<Expression>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Invalid args to ||")
        }

        val (firstEx, secondEx) = args

        val first = interpreter.interpret(scope, firstEx)?.coerceTo(Boolean::class) ?: pos.interpretFail("Invalid args to ||")

        return if (first) {
          true
        } else {
          interpreter.interpret(scope, secondEx)?.coerceTo(Boolean::class) ?: pos.interpretFail("Invalid args to ||")
        }
      }
    }


  }


}
