package lisp.lib

import lisp.FunctionValue
import lisp.ParamMeta
import lisp.Position
import lisp.Scope
import lisp.runtime.Type
import kotlin.math.PI

object MathLibrary: Library {
  override fun addLib(global: Scope) {
    global["math/pi"] = PI

    global["math/divInt"] = object: FunctionValue {
      override val name: String = "math/divInt"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("dividend", Type.IntegerType, "base number to be divided"),
        ParamMeta("divisor", Type.IntegerType, "number by which base is devided")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val (firstRaw, secondRaw) = args

        val first = firstRaw as Int
        val second = secondRaw as Int

        return first / second
      }
    }
  }
}
