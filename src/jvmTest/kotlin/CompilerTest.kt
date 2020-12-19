import lisp.*
import lisp.bytecode.BytecodeInterpreter
import lisp.compiler.Compiler
import lisp.ir.ClosureChecker
import lisp.ir.IrCompiler
import lisp.lib.CoreLibrary
import lisp.transform.AutoWrapTransformer
import lisp.transform.DefineTransformer
import kotlin.test.Test

class CompilerTest {

  @Test
  fun irTest() {
    val file = File.from("src\\jvmTest\\resources\\compilerTest.lisp")
      .readText()

    val evaluator = BytecodeEvaluator(BytecodeInterpreter(JvmCommand()))
    val global = CoreLibrary.coreLib()
    val module = global.child(ScopeKind.module)

    val result = evaluator.evaluate(module.child(), file, "compilerTest.lisp")

    println(result)
  }

  @Test
  fun closureChecker() {
    val file = File.from("src\\jvmTest\\resources\\compilerTest.lisp")
      .readText()

    val tokens = Lexer.lex(file, "compilerTest.lisp")
    val ast = Parser.parse(tokens)

    val transformed = DefineTransformer().transform(ast)

    val pos = transformed.first().pos
    val ir = IrCompiler().compileFunction(CallEx( listOf(VariableEx("do", pos)) + transformed, pos))

    println(ir)
  }

}
