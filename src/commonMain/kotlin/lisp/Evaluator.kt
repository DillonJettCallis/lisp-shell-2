package lisp

import lisp.bytecode.BytecodeInterpreter
import lisp.compiler.Compiler
import lisp.ir.IrCompiler
import lisp.transform.AutoWrapTransformer
import lisp.transform.DefineTransformer
import lisp.transform.Transformer

interface Evaluator {
  fun evaluate(scope: Scope, raw: String, src: String, baseDir: File, autoWrap: Boolean = false): Any?
}

private fun prepareAst(raw: String, src: String, autoWrap: Boolean): List<Expression> {
  val tokens = Lexer.lex(raw, src)
  val ast = Parser.parse(tokens)

  if (ast.isEmpty()) {
    return emptyList()
  }

  return if (autoWrap) {
    AutoWrapTransformer().transform(ast)
  } else {
    ast
  }
}

class BytecodeEvaluator(private val interpreter: BytecodeInterpreter, private val transformers: List<Transformer> = listOf(DefineTransformer())): Evaluator {
  override fun evaluate(scope: Scope, raw: String, src: String, baseDir: File, autoWrap: Boolean): Any? {
    val ast = prepareAst(raw, src, autoWrap)

    if (ast.isEmpty()) {
      return null
    }

    val transformed = transformers.fold(ast) { sum, next -> next.transform(sum) }

    val pos = transformed.first().pos
    val ir = IrCompiler().compileBlock(CallEx( listOf(VariableEx("do", pos)) + transformed, pos))

    val compiled = Compiler().compile(ir)

    val childScope = scope.child()
    childScope["(baseDir)"] = baseDir
    childScope["(evaluator)"] = this

    return interpreter.interpret(childScope, compiled, emptyArray(), emptyArray())
  }
}

fun Evaluator.evaluateFile(scope: Scope, file: File): Any? {
  val raw = file.readText()
  val src = file.toString()

  return evaluate(scope, raw, src, file.parent(), false)
}
