package lisp

import lisp.bytecode.BytecodeInterpreter
import lisp.compiler.Compiler
import lisp.ir.IrCompiler
import lisp.runtime.Type
import lisp.transform.AutoWrapTransformer
import lisp.transform.DefineTransformer
import lisp.transform.Transformer

interface Evaluator {
  fun evaluate(scope: Scope, raw: String, src: String, baseDir: File, autoWrap: Boolean = false, isModule: Boolean = false): Any?
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
  override fun evaluate(scope: Scope, raw: String, src: String, baseDir: File, autoWrap: Boolean, isModule: Boolean): Any? {
    val ast = prepareAst(raw, src, autoWrap)

    if (ast.isEmpty()) {
      return null
    }

    val transformed = transformers.fold(ast) { sum, next -> next.transform(sum) }

    val pos = transformed.first().pos
    val ir = IrCompiler().compileBlock(CallEx( listOf(VariableEx("do", pos)) + transformed, pos), isModule = isModule)

    val compiled = Compiler().compile(ir)

    val childScope = scope.child()
    val importer = ImportFunction(scope, this, baseDir)
    val includer = IncludeFunction(scope, importer)

    childScope["import"] = importer
    childScope["include"] = includer

    return interpreter.interpret(childScope, compiled, emptyArray(), emptyArray())
  }
}

fun Evaluator.evaluateFile(scope: Scope, file: File): Any? {
  val raw = file.readText()
  val src = file.toString()

  return evaluate(scope, raw, src, file.parent())
}

fun Evaluator.evaluateModule(scope: Scope, file: File): Scope {
  val moduleScope = scope.global().child(ScopeKind.module)

  val raw = file.readText()
  val src = file.toString()

  evaluate(moduleScope, raw, src, file.parent(), isModule = true)

  return moduleScope
}

class ImportFunction(private val targetScope: Scope,
                     private val evaluator: Evaluator,
                     private val baseFile: File,
                     ): FunctionValue {

  override val name = "import"
  override val params = listOf(
    ParamMeta("path", Type.PathType, "relative or absolute path of file to import")
  )

  override fun call(args: List<Any?>, pos: Position): Map<String, Any?> {
    val path = args[0] as Path
    val file = baseFile.resolve(path)

    return evaluator.evaluateModule(targetScope, file)
      .export()
  }
}

class IncludeFunction(
  private val targetScope: Scope,
  private val importer: ImportFunction
) : FunctionValue {

  override val name = "include"
  override val params = listOf(
    ParamMeta("values", Type.AnyType, "map of values to include or a string pointing to a file to include"),
    ParamMeta("prefix", Type.StringType, "optional prefix to apply to included values")
  )

  override fun call(args: List<Any?>, pos: Position): Any? {
    if (args.isEmpty()) {
      pos.interpretFail("Expected function 'include' to have one or two arguments")
    }

    val values = when (val raw = args[0]) {
      is String, is File, is Path -> importer.call(listOf(Type.coerce(Type.PathType, raw)), pos)
      is Map<*, *> -> raw as Map<String, *>
      else -> pos.interpretFail("Expected first argument to include to be a path or a map")
    }
    val prefix = if (args.size > 1) args[1] as String? else null

    if (prefix == null) {
      values.forEach { (key, value) -> targetScope.define(key, value) }
    } else {
      values.forEach { (key, value) -> targetScope.define("$prefix/$key", value) }
    }

    return null
  }
}

