package lisp.lib

import lisp.FunctionValue
import lisp.ParamMeta
import lisp.Position
import lisp.Scope
import lisp.runtime.Type

object MapLibrary : Library {

  override fun addLib(global: Scope) {
    global["get"] = object : FunctionValue {
      override val name: String = "get"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("map", Type.MapType, "map to access"),
        ParamMeta("key", Type.AnyType, "key to get from map")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val (map, key) = args

        return (map as Map<*, *>)[key]
      }
    }

    global["set"] = object : FunctionValue {
      override val name: String = "set"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("map", Type.MapType, "map to access"),
        ParamMeta("key", Type.AnyType, "key to set"),
        ParamMeta("value", Type.AnyType, "value to set")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val (oldMap, key, value) = args

        val map = HashMap(oldMap as Map<*, *>)

        map[key] = value
        return map
      }
    }

    global["contains"] = object : FunctionValue {
      override val name: String = "contains"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("map", Type.MapType, "map to check"),
        ParamMeta("key", Type.AnyType, "key to check for")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val (map, key) = args

        return (map as Map<*, *>).containsKey(key)
      }
    }

    global["entries"] = object : FunctionValue {
      override val name: String = "entries"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("map", Type.MapType, "map to list entries")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        return (args[0] as Map<*, *>).entries.map { (k: Any?, v: Any?) -> listOf(k, v) }
      }
    }
  }


}
