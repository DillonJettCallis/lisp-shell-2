package lisp

import lisp.runtime.Type

data class ParamMeta(val name: String, val type: Type, val desc: String) {
  constructor(name: String): this(name, Type.AnyType, "$name Any")
}

interface FunctionValue {
  val name: String
  val params: List<ParamMeta>
  fun call(args: List<Any?>, pos: Position): Any?
}

// marker interface
interface OperatorFunctionValue: FunctionValue
