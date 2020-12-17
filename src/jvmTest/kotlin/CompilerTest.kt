import lisp.BytecodeEvaluator
import lisp.File
import lisp.JvmCommand
import lisp.ScopeKind
import lisp.bytecode.BytecodeInterpreter
import lisp.lib.CoreLibrary
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

}
