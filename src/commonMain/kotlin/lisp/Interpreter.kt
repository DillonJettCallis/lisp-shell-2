package lisp

import lisp.coercion.coerceTo
import lisp.transform.AutoWrapTransformer

class InterpreterException(message: String, cause: Throwable? = null): RuntimeException(message, cause)

class Interpreter(private val shell: Command) {

  fun evaluate(scope: Scope, raw: String, src: String, autoWrap: Boolean = false): Any? {
    val tokens = Lexer.lex(raw, src)
    val ast = Parser.parse(tokens)

    if (ast.isEmpty()) {
      return null
    }

    val exList = if (autoWrap) {
      AutoWrapTransformer().transform(ast)
    } else {
      ast
    }

    return exList.fold(null) { _: Any?, next -> interpret(scope, next) }
  }

  fun interpret(scope: Scope, ex: Expression): Any? {
    return when (ex) {
      is LiteralEx -> ex.value
      is CommandEx -> shellFunction(ex.value)
      is VariableEx -> scope[ex.name]
      is OperatorEx -> scope.getGlobal(ex.op)
      is CallEx -> {
        val funcEx = ex.body.first()

        val func = if (funcEx is StringLiteralEx && !funcEx.quoted) {
          scope[funcEx.value]
        } else {
          interpret(scope, funcEx)
        }

        when (func) {
          is MacroFunctionValue -> func.call(this, scope, ex.body.drop(1), ex.pos)
          is FunctionValue -> func.call(ex.body.drop(1).map { interpret(scope, it) }, ex.pos)
          else -> ex.pos.interpretFail("Expected function")
        }
      }
      is ArrayEx -> ex.body.map { interpret(scope, it) }
      is MapEx -> ex.body.associate { (k, v) -> interpret(scope, k) to interpret(scope, v) }
    }
  }

  private fun shellFunction(command: String): MacroFunctionValue {
    return object: MacroFunctionValue {
      override fun call(interpreter: Interpreter, scope: Scope, args: List<Expression>, pos: Position): String {
        val cwd = scope["cwd"]?.coerceTo(File::class) ?: pos.interpretFail("Expected 'cwd' to be a file")

        return shell.execute(cwd, command, args.map { interpreter.interpret(scope, it).stringify(it.pos) })
      }

      private fun Any?.stringify(pos: Position): String {
        return when(this) {
          null -> "null"
          is OperatorFunctionValue -> op
          is FunctionValue, is MacroFunctionValue -> pos.interpretFail("Cannot render function to string")
          else -> toString()
        }
      }
    }
  }

}

interface FunctionValue {
  fun call(args: List<Any?>, pos: Position): Any?
}

interface OperatorFunctionValue {
  val op: String
}

interface MacroFunctionValue {
  fun call(interpreter: Interpreter, scope: Scope, args: List<Expression>, pos: Position): Any?
}
