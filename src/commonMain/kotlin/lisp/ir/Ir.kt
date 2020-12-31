package lisp.ir

import lisp.ParamMeta
import lisp.Position

data class IrFunction(
  val body: MutableList<Ir>,
  val params: MutableList<ParamMeta>,
  val pos: Position,
  val closureContext: ClosureContext = ClosureContext.empty
)

sealed class Ir {
  abstract val pos: Position
}


data class PopIr(override val pos: Position): Ir()
data class DupIr(override val pos: Position): Ir()
data class SwapIr(override val pos: Position): Ir()
data class IncrementIr(override val pos: Position): Ir()
data class DecrementIr(override val pos: Position): Ir()
data class DefineIr(val name: String, override val pos: Position): Ir()
data class StoreIr(val name: String, override val pos: Position): Ir()
data class LoadIr(val name: String, override val pos: Position): Ir()
data class LoadConstIr(val value: Any?, override val pos: Position): Ir()
data class LoadFuncIr(val func: IrFunction, override val pos: Position): Ir()
data class LoadRecurseIr(override val pos: Position): Ir()
data class LoadArgArrayIr(override val pos: Position): Ir()
data class FreeIr(val name: String, override val pos: Position): Ir()
data class CallIr(val paramCount: Int, override val pos: Position): Ir()
data class CallDynamicIr(override val pos: Position): Ir()
data class ReturnIr(override val pos: Position): Ir()
data class ReturnVoidIr(override val pos: Position): Ir()
data class BuildShellIr(val path: String, override val pos: Position): Ir()
data class BuildClosureIr(val paramCount: Int, override val pos: Position): Ir()
data class BranchIr(val thenEx: MutableList<Ir>, val elseEx: MutableList<Ir>, override val pos: Position): Ir()
data class LoopIr(val condition: MutableList<Ir>, val body: MutableList<Ir>, override val pos: Position): Ir()
