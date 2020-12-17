package lisp.lib

import lisp.FunctionValue
import lisp.ParamMeta
import lisp.Position
import lisp.Scope

object MapLibrary : Library {

  override fun addLib(global: Scope) {
    global["map/(build)"] = object: FunctionValue {
      override val name: String = "map/(build)"
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Any? {
        return HashMap<Any?, Any?>()
      }
    }

    global["map/(mutablePut)"] = object: FunctionValue {
      override val name: String = "map/(mutablePut)"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("map", List::class, "map to mutate"),
        ParamMeta("key", Any::class, "key to insert"),
        ParamMeta("value", Any::class, "value to insert")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        return (args[0] as MutableMap<Any?, Any?>).put(args[1], args[2])
      }
    }

    global["map/get"] = object : FunctionValue {
      override val name: String = "map/get"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("map", Map::class, "map to access"),
        ParamMeta("key", Any::class, "key to get from map")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Expected two arguments to 'map/get'")
        }

        val (map, key) = args

        if (map !is Map<*, *>) {
          pos.interpretFail("Expected second argument to 'map/get' to be a map")
        }

        return map[key]
      }
    }

    global["map/set"] = object : FunctionValue {
      override val name: String = "map/set"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("map", Map::class, "map to access"),
        ParamMeta("key", Any::class, "key to set"),
        ParamMeta("value", Any::class, "value to set")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 3) {
          pos.interpretFail("Expected three arguments to 'map/set'")
        }

        val (map, key, value) = args

        if (map !is MutableMap<*, *>) {
          pos.interpretFail("Expected third argument to 'map/set' to be a map")
        }

        (map as MutableMap<Any?, Any?>)[key] = value
        return value
      }
    }

    global["map/contains"] = object : FunctionValue {
      override val name: String = "map/contains"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("map", Map::class, "map to check"),
        ParamMeta("key", Any::class, "key to check for")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Expected two arguments to 'map/contains'")
        }

        val (map, key) = args

        if (map !is Map<*, *>) {
          pos.interpretFail("Expected second argument to 'map/contains' to be a map")
        }

        return map.containsKey(key)
      }
    }

    global["map/entries"] = object : FunctionValue {
      override val name: String = "map/entries"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("map", Map::class, "map to list entries")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 1) {
          pos.interpretFail("Expected one argument to 'map/entries'")
        }

        val (map) = args

        if (map !is Map<*, *>) {
          pos.interpretFail("Expected first argument to 'map/entries' to be a map")
        }

        return map.entries.map { (k: Any?, v: Any?) -> listOf(k, v) }
      }
    }
  }


}
