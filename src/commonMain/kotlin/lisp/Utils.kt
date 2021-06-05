package lisp

import lisp.bytecode.ClosureFunction
import lisp.compiler.Compiler
import lisp.ir.*


class IrFunctionBuilder(private val pos: Position) {

  private val body = ArrayList<Ir>()

  val build: ArrayList<Ir>
    get() = body

  fun pop() {
    body += PopIr(pos)
  }

  fun dup() {
    body += DupIr(pos)
  }

  fun swap() {
    body += SwapIr(pos)
  }

  fun inc() {
    body += IncrementIr(pos)
  }

  fun dec() {
    body += DecrementIr(pos)
  }

  fun define(name: String) {
    body += DefineIr(name, pos)
  }

  fun store(name: String) {
    body += StoreIr(name, pos)
  }

  fun load(name: String) {
    body += LoadIr(name, pos)
  }

  fun loadConst(value: Any?) {
    body += LoadConstIr(value, pos)
  }

  fun increment() {
    body += IncrementIr(pos)
  }

  fun decrement() {
    body += DecrementIr(pos)
  }

  fun call(paramCount: Int) {
    body += CallIr(paramCount, pos)
  }

  fun callDynamic() {
    body += CallDynamicIr(pos)
  }

  fun returnIr() {
    body += ReturnIr(pos)
  }

  fun shell(path: String) {
    body += BuildShellIr(path, pos)
  }

  fun branch(thenBlock: IrFunctionBuilder.() -> Unit, elseBlock: IrFunctionBuilder.() -> Unit) {
    body += BranchIr(
      thenEx = IrFunctionBuilder(pos).also { thenBlock(it) }.body,
      elseEx = IrFunctionBuilder(pos).also { elseBlock(it) }.body,
      pos = pos
    )
  }

  fun loop(conditionBlock: IrFunctionBuilder.() -> Unit, bodyBlock: IrFunctionBuilder.() -> Unit) {
    body += LoopIr(
      condition = IrFunctionBuilder(pos).also { conditionBlock(it) }.body,
      body = IrFunctionBuilder(pos).also { bodyBlock(it) }.body,
      pos = pos
    )
  }

}

fun Scope.compileNative(name: String, params: MutableList<ParamMeta>, builder: IrFunctionBuilder.() -> Unit) {
  val pos = Position(0, 0, "$name <native code>")
  val body = IrFunctionBuilder(pos).also { builder(it) }.build

  val ir = IrCompiler().constructFunction(name, body, params, pos)
  val bytecode = Compiler().compile(ir)

  this[name] = ClosureFunction(this, emptyArray(), bytecode)
}

