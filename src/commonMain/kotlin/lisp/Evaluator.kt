package lisp

import lisp.Command.Companion.defaultFlags
import lisp.bytecode.BytecodeInterpreter
import lisp.compiler.Compiler
import lisp.ir.IrCompiler
import lisp.lib.CoreLibrary
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

    val ir = IrCompiler().compileBlock("body:${src}", transformed, isModule = isModule)

    val compiled = Compiler().compile(ir)

    val childScope = scope.child()
    val importer = ImportFunction(scope, this, baseDir)

    childScope["import"] = importer

    if (isModule) {
      childScope["cwd"] = baseDir
    }

    return interpreter.interpret(childScope, compiled, emptyArray(), emptyArray())
  }
}

fun Evaluator.evaluateFile(scope: Scope, file: File): Any? {
  val raw = file.readText()
  val src = file.toString()

  return evaluate(scope, raw, src, file.parent())
}

fun Evaluator.evaluateModule(scope: Scope, file: File): Scope {
  val (exportScope, moduleScope) = scope.global().constructModuleScope()

  val raw = file.readText()
  val src = file.toString()

  evaluate(moduleScope, raw, src, file.parent(), isModule = true)

  return exportScope
}

class ImportFunction(private val targetScope: Scope,
                     private val evaluator: Evaluator,
                     private val baseFile: File,
                     ): FunctionValue {

  override val name = "import"
  override val params = listOf(
    ParamMeta("path", Type.AnyType, "relative or absolute path of file to import"),
    ParamMeta("prefix", Type.StringType, "optional prefix to apply to imported items")
  )

  override fun call(args: List<Any?>, pos: Position) {
    val prefix = if (args.size > 1) {
      args[1] as String
    } else {
      null
    }

    targetScope.include(resolve(args[0], pos), prefix)
  }

  private fun resolve(raw: Any?, pos: Position): Map<String, Any?> {
    return when (raw) {
      is File -> resolveFile(raw)
      is Path -> resolveFile(baseFile.resolve(raw))
      is String -> {
        if (raw.startsWith("#")) {
          resolveNative(raw, pos)
        } else {
          resolveFile(baseFile.resolve(Path.from(raw)))
        }
      }
      is Map<*, *> -> raw as Map<String, *>
      else -> pos.interpretFail("Expected only argument to import to be a File, Path, String, or a map")
    }
  }

  private fun resolveFile(file: File): Map<String, Any?> {
    return evaluator.evaluateModule(targetScope, file)
      .export()
  }

  private fun resolveNative(raw: String, pos: Position): Map<String, Any?> {
    val lib = CoreLibrary.nativeLibs[raw.substring(1)] ?: pos.interpretFail("No such native library $raw found")
    val module = targetScope.global().child(ScopeKind.module)

    lib.addLib(module)

    return module.export()
  }
}

class ExecFunction(private val sh: Command) : FunctionValue {
  override val name = "exec"
  override val params: List<ParamMeta> = listOf(
    ParamMeta("workingDir", Type.FileType, "working directory"),
    ParamMeta("command", Type.StringType, "shell command to run"),
    ParamMeta("args", Type.ArrayType, "array of shell arguments"),
    ParamMeta("flags", Type.ArrayType, "array of flags to use in command"),
  )

  override fun call(args: List<Any?>, pos: Position): Any? {
    if (args.size < 2) {
      pos.interpretFail("Not enough args passed to exec. workingDir and command are required")
    }

    val rawCwd = args[0] as File
    val rawCommand = args[1] as String

    val rawCmdArgs = if (args.size >= 3) (args[2] as List<Any?>).map { Type.coerce(Type.StringType, it)!! } as List<String> else listOf()

    val rawFlags = if (args.size >= 4) {
      (args[3] as List<Any?>)
        .mapTo(HashSet()) { Type.coerce(Type.StringType, it)!! } as Set<String>
    } else {
      defaultFlags
    }

    val flags = Command.evalFlags(rawFlags, pos)

    return sh.execute(rawCwd, rawCommand, rawCmdArgs, flags)
  }


}

