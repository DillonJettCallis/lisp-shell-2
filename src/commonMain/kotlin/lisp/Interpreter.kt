package lisp

import lisp.coercion.CoercionRegistry
import lisp.coercion.coerceTo
import lisp.transform.AutoWrapTransformer
import lisp.transform.DefineTransformer
import lisp.transform.Transformer
import kotlin.reflect.KClass

data class ParamMeta(val name: String, val type: KClass<*>, val desc: String)

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
