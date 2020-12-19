package lisp

import kotlin.reflect.KClass

data class ParamMeta(val name: String, val type: KClass<*>, val desc: String) {
  constructor(name: String): this(name, Any::class, "$name Any")
}

interface FunctionValue: SpecialFunctionValue {
  fun call(args: List<Any?>, pos: Position): Any?

  override fun call(scope: Scope, args: List<Any?>, pos: Position): Any? {
    return call(args, pos)
  }
}

interface SpecialFunctionValue {
  val name: String
  val params: List<ParamMeta>

  fun call(scope: Scope, args: List<Any?>, pos: Position): Any?
}

// marker interface
interface OperatorFunctionValue: FunctionValue
