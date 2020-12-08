package lisp.lib

import lisp.FunctionValue
import lisp.Position
import lisp.Scope
import lisp.coercion.coerceTo

object ArrayLibrary: Library {

  override fun addLib(global: Scope) {
    global["array/get"] = object: FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Expected two arguments to 'array/get'")
        }

        val (arrayRaw, indexRaw) = args

        val index = indexRaw?.coerceTo(Int::class) ?: pos.interpretFail("Expected first argument to 'array/get' to be an int")
        val array = arrayRaw?.coerceTo(List::class) ?: pos.interpretFail("Expected second argument to 'array/get' to be an array")

        return array[index]
      }
    }

    global["array/set"] = object: FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 3) {
          pos.interpretFail("Expected three arguments to 'array/set'")
        }

        val (indexRaw, value, arrayRaw) = args

        val index = indexRaw?.coerceTo(Int::class) ?: pos.interpretFail("Expected first argument to 'array/set' to be an int")
        val array = arrayRaw?.coerceTo(List::class) ?: pos.interpretFail("Expected third argument to 'array/set' to be an array")

        (array as MutableList<Any?>)[index] = value
        return value
      }
    }

    global["array/map"] = object: FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Expected two arguments to 'array/map'")
        }

        val (func, arrayRaw) = args

        if (func !is FunctionValue) {
          pos.interpretFail("Expected first argument to 'array/map' to be mapper function")
        }

        val array = arrayRaw?.coerceTo(List::class) ?: pos.interpretFail("Expected second argument to 'array/map' to be an array")

        return array.map {func.call(listOf(it), pos) }
      }
    }

    global["array/flatMap"] = object: FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Expected two arguments to 'array/flatMap'")
        }

        val (func, arrayRaw) = args

        if (func !is FunctionValue) {
          pos.interpretFail("Expected first argument to 'array/flatMap' to be mapper function")
        }

        val array = arrayRaw?.coerceTo(List::class) ?: pos.interpretFail("Expected second argument to 'array/flatMap' to be an array")

        return array.flatMap {
          val out = func.call(listOf(it), pos)

          out?.coerceTo(List::class) ?: pos.interpretFail("Expected function of 'array/flatMap' to always return an array")
        }
      }
    }

    global["array/filter"] = object: FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Expected two arguments to 'array/filter'")
        }

        val (func, arrayRaw) = args

        if (func !is FunctionValue) {
          pos.interpretFail("Expected first argument to 'array/filter' to be filter function")
        }

        val array = arrayRaw?.coerceTo(List::class) ?: pos.interpretFail("Expected second argument to 'array/filter' to be an array")

        return array.filter {
          val out = func.call(listOf(it), pos)

          out?.coerceTo(Boolean::class) ?: pos.interpretFail("Expected function of 'array/filter' to always return a boolean")
        }
      }
    }

    global["array/fold"] = object: FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 3) {
          pos.interpretFail("Expected three arguments to 'array/fold'")
        }

        val (func, init, arrayRaw) = args

        if (func !is FunctionValue) {
          pos.interpretFail("Expected first argument to 'array/fold' to be a function")
        }

        val array = arrayRaw?.coerceTo(List::class) ?: pos.interpretFail("Expected third argument to 'array/fold' to be an array")

        return array.fold(init) { sum, next -> func.call(listOf(sum, next), pos) }
      }
    }

    global["array/forEach"] = object: FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Expected two arguments to 'array/forEach'")
        }

        val (func, arrayRaw) = args

        if (func !is FunctionValue) {
          pos.interpretFail("Expected first argument to 'array/forEach' to be a function")
        }

        val array = arrayRaw?.coerceTo(List::class) ?: pos.interpretFail("Expected second argument to 'array/forEach' to be an array")

        array.forEach { func.call(listOf(it), pos) }
        return null
      }
    }
  }

}
