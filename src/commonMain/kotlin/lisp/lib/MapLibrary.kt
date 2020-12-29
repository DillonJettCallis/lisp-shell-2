package lisp.lib

import lisp.FunctionValue
import lisp.ParamMeta
import lisp.Position
import lisp.Scope
import lisp.runtime.Type

object MapLibrary : Library {

  override fun addLib(global: Scope) {
    global["map/(build)"] = object : FunctionValue {
      override val name: String = "map/(build)"
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Any? {
        return HashMap<Any?, Any?>()
      }
    }

    global["map/(mutablePut)"] = object : FunctionValue {
      override val name: String = "map/(mutablePut)"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("map", Type.MapType, "map to mutate"),
        ParamMeta("key", Type.AnyType, "key to insert"),
        ParamMeta("value", Type.AnyType, "value to insert")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val (rawMap, key, value) = args
        val map = rawMap as MutableMap<Any?, Any?>
        map[key] = value
        return map
      }
    }

    global["map/get"] = object : FunctionValue {
      override val name: String = "map/get"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("map", Type.MapType, "map to access"),
        ParamMeta("key", Type.AnyType, "key to get from map")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val (map, key) = args

        return (map as Map<*, *>)[key]
      }
    }

    global["map/set"] = object : FunctionValue {
      override val name: String = "map/set"
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

    global["map/contains"] = object : FunctionValue {
      override val name: String = "map/contains"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("map", Type.MapType, "map to check"),
        ParamMeta("key", Type.AnyType, "key to check for")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val (map, key) = args

        return (map as Map<*, *>).containsKey(key)
      }
    }

    global["map/entries"] = object : FunctionValue {
      override val name: String = "map/entries"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("map", Type.MapType, "map to list entries")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        return (args[0] as Map<*, *>).entries.map { (k: Any?, v: Any?) -> listOf(k, v) }
      }
    }
  }


}
