package lisp.lib

import lisp.FunctionValue
import lisp.Position
import lisp.Scope
import lisp.coercion.coerceTo
import kotlin.math.PI

object MathLibrary: Library {
  override fun addLib(global: Scope) {
    global["math/pi"] = PI

    global["math/divInt"] = object: FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Expected exactly 2 arguments to math/divInt")
        }

        val (firstRaw, secondRaw) = args

        val first = firstRaw?.coerceTo(Int::class) ?: pos.interpretFail("Expected first arg to math/divInt to be integer")
        val second = secondRaw?.coerceTo(Int::class) ?: pos.interpretFail("Expected second arg to math/divInt to be integer")

        return first / second
      }
    }
  }
}
