package lisp

import lisp.bytecode.ClosureFunction
import lisp.compiler.Compiler
import lisp.ir.*


class IrFunctionBuilder(private val pos: Position) {

  private val body = ArrayList<Ir>()

  val build: List<Ir>
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

  fun loop(loopBuilder: IrFunctionLoopBuilder.() -> Unit) {
    val builder = IrFunctionLoopBuilder(pos)

    loopBuilder(builder)

    body += builder.build()
  }

}

class IrFunctionLoopBuilder(private val pos: Position) {
  private var condition: List<Ir> = emptyList()
  private var body: List<Ir> = emptyList()

  infix fun condition(conditionBlock: IrFunctionBuilder.() -> Unit) {
    condition = IrFunctionBuilder(pos).also { conditionBlock(it) }.build
  }

  infix fun body(bodyBlock: IrFunctionBuilder.() -> Unit) {
    body = IrFunctionBuilder(pos).also { bodyBlock(it) }.build
  }

  fun build() = LoopIr(condition, body, pos)
}


fun Scope.compileNative(name: String, params: List<ParamMeta>, builder: IrFunctionBuilder.() -> Unit) {
  val pos = Position(0, 0, "$name <native code>")
  val ir = IrFunctionBuilder(pos).also { builder(it) }.build
  val compiler = Compiler()

  val bytecode = compiler.compile(IrFunction(ir, params, pos))

  this[name] = ClosureFunction(this, bytecode)
}

