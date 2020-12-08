package lisp.lib

import lisp.FunctionValue
import lisp.Position
import lisp.Scope

object MapLibrary : Library {

  override fun addLib(global: Scope) {
    global["map/get"] = object : FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Expected two arguments to 'map/get'")
        }

        val (key, map) = args

        if (map !is Map<*, *>) {
          pos.interpretFail("Expected second argument to 'map/get' to be a map")
        }

        return map[key]
      }
    }

    global["map/set"] = object : FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 3) {
          pos.interpretFail("Expected three arguments to 'map/set'")
        }

        val (key, value, map) = args

        if (map !is MutableMap<*, *>) {
          pos.interpretFail("Expected third argument to 'map/set' to be a map")
        }

        (map as MutableMap<Any?, Any?>)[key] = value
        return value
      }
    }

    global["map/contains"] = object : FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Expected two arguments to 'map/contains'")
        }

        val (key, map) = args

        if (map !is Map<*, *>) {
          pos.interpretFail("Expected second argument to 'map/contains' to be a map")
        }

        return map.containsKey(key)
      }
    }

    global["map/entries"] = object : FunctionValue {
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
