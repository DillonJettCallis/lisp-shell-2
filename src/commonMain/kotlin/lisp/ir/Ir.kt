package lisp.ir

import lisp.ParamMeta
import lisp.Position

data class IrFunction(val body: List<Ir>, val params: List<ParamMeta>, val pos: Position)

sealed class Ir {
  abstract val pos: Position
}


data class PopIr(override val pos: Position): Ir()
data class DupIr(override val pos: Position): Ir()
data class SwapIr(override val pos: Position): Ir()
data class DefineIr(val name: String, override val pos: Position): Ir()
data class StoreIr(val name: String, override val pos: Position): Ir()
data class LoadIr(val name: String, override val pos: Position): Ir()
data class LoadConstIr(val value: Any?, override val pos: Position): Ir()
data class CallIr(val paramCount: Int, override val pos: Position): Ir()
data class CallDynamicIr(override val pos: Position): Ir()
data class ReturnIr(override val pos: Position): Ir()
data class BuildShellIr(val path: String, override val pos: Position): Ir()
data class BuildClosureIr(val func: IrFunction, override val pos: Position): Ir()
data class BranchIr(val thenEx: List<Ir>, val elseEx: List<Ir>, override val pos: Position): Ir()
data class LoopIr(val condition: List<Ir>, val body: List<Ir>, override val pos: Position): Ir()
