package lisp.lib

import lisp.*
import lisp.runtime.Type

object PathLibrary: Library {

  override fun addLib(global: Scope) {
    global["path"] = object: FunctionValue {
      override val name: String = "path"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("path", Type.StringType, "raw path")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val str = args[0] as String

        return Path.from(str)
      }
    }
  }

}
