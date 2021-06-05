import lisp.*
import lisp.bytecode.BytecodeInterpreter
import lisp.ir.IrCompiler
import lisp.lib.CoreLibrary
import lisp.transform.DefineTransformer
import kotlin.test.Test

class CompilerTest {

  @Test
  fun irTest() {
    val file = File.from("src\\jvmTest\\resources\\compilerTest.lisp")

    val evaluator = BytecodeEvaluator(BytecodeInterpreter(JvmCommand()))
    val global = CoreLibrary.coreLib()
    val module = global.child(ScopeKind.module)

    val result = evaluator.evaluateFile(module, file)

    println(result)
  }

  @Test
  fun closureChecker() {
    val file = File.from("src\\jvmTest\\resources\\compilerTest.lisp")
      .readText()

    val tokens = Lexer.lex(file, "compilerTest.lisp")
    val ast = Parser.parse(tokens)

    val transformed = DefineTransformer().transform(ast)

    val ir = IrCompiler().compileBlock("body:compilerTest", transformed)

    println(ir)
  }

  @Test
  fun importTest() {
    val file = File.from("src\\jvmTest\\resources\\importTest.lisp")

    val evaluator = BytecodeEvaluator(BytecodeInterpreter(JvmCommand()))
    val global = CoreLibrary.coreLib()
    val module = global.child(ScopeKind.module)

    val result = evaluator.evaluateFile(module, file)

    println(result)
  }

  @Test
  fun moduleTest() {
    val file = File.from("src\\jvmTest\\resources\\moduleTest.lisp")

    val evaluator = BytecodeEvaluator(BytecodeInterpreter(JvmCommand()))
    val global = CoreLibrary.coreLib()
    val module = global.child(ScopeKind.module)

    val result = evaluator.evaluateFile(module, file)

    println(result)
  }
}
