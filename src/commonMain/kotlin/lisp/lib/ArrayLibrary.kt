package lisp.lib

import lisp.FunctionValue
import lisp.ParamMeta
import lisp.Position
import lisp.Scope
import lisp.bytecode.ClosureFunction
import lisp.coercion.coerceTo
import lisp.compiler.Compiler
import lisp.ir.*

object ArrayLibrary: Library {

  override fun addLib(global: Scope) {
    global["array/(build)"] = object: FunctionValue {
      override val name: String = "array/(build)"
      override val params: List<ParamMeta> = emptyList()

      override fun call(args: List<Any?>, pos: Position): Any? {
        return ArrayList<Any?>()
      }
    }

    global["array/(mutableAdd)"] = object: FunctionValue {
      override val name: String = "array/(mutableAdd)"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("array", List::class, "array to add to"),
        ParamMeta("next", Any::class, "next item to add")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val arr = (args[0] as MutableList<Any?>)
        arr.add(args[1])
        return arr
      }
    }

    global["array/size"] = object: FunctionValue {
      override val name: String = "array/size"
      override val params: List<ParamMeta> = listOf(ParamMeta("array", List::class, "array to check"))

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 1) {
          pos.interpretFail("Expected two arguments to 'array/get'")
        }

        val (arrayRaw) = args

        val array = arrayRaw?.coerceTo(List::class) ?: pos.interpretFail("Expected first argument to 'array/size' to be an array")

        return array.size
      }
    }

    global["array/get"] = object: FunctionValue {
      override val name: String = "array/get"
      override val params: List<ParamMeta> = listOf(ParamMeta("array", List::class, "array to access"), ParamMeta("index", Int::class, "index of array"))

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
      override val name: String = "array/set"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("array", List::class, "array to access"),
        ParamMeta("index", Int::class, "index of array"),
        ParamMeta("value", Any::class, "value to set in array")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 3) {
          pos.interpretFail("Expected three arguments to 'array/set'")
        }

        val (arrayRaw, indexRaw, value) = args

        val index = indexRaw?.coerceTo(Int::class) ?: pos.interpretFail("Expected first argument to 'array/set' to be an int")
        val array = arrayRaw?.coerceTo(List::class) ?: pos.interpretFail("Expected third argument to 'array/set' to be an array")

        (array as MutableList<Any?>)[index] = value
        return value
      }
    }

    global["array/add"] = object: FunctionValue {
      override val name: String = "array/add"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("array", List::class, "array to access"),
        ParamMeta("value", Any::class, "value to add to end of array")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Expected three arguments to 'array/add'")
        }

        val (arrayRaw, value) = args

        val array = arrayRaw?.coerceTo(List::class) ?: pos.interpretFail("Expected first argument to 'array/add' to be an array")

        return array + value
      }
    }

    val pos = Position(1, 1, "array/map <native>")

    global["array/map"] = ClosureFunction(global, Compiler().compile(IrFunction(
      body = listOf(
        LoadIr("array/size", pos), // [array/size]
        LoadIr("arr", pos), // [array/size, arr]
        CallIr(1, pos), // [size]
        StoreIr("size", pos), // []
        LoadIr("array/(build)", pos), // [array/(build)]
        CallIr(0, pos), // [res]
        LoadConstIr(0, pos), // [res, index]
        StoreIr("index", pos), // [res]

        LoopIr(
          condition = listOf(
            // [res]
            LoadIr("!=", pos), // [res, !=]
            LoadIr("size", pos), // [res, !=, size]
            LoadIr("index", pos), // [res, !=, size, index]
            CallIr(2, pos), // [res, isNotEqual]
          ),
          body = listOf(
            // [res]
            LoadIr("array/(mutableAdd)", pos), // [res, array/(mutableAdd)]
            SwapIr(pos), // [array/(mutableAdd), res]
            LoadIr("mapper", pos), // [array/(mutableAdd), res, mapper]
            LoadIr("array/get", pos), // [array/(mutableAdd), res, mapper, array/get]
            LoadIr("arr", pos), // [array/(mutableAdd), res, mapper, array/get, arr]
            LoadIr("index", pos), // [array/(mutableAdd), res, mapper, array/get, arr, index]
            CallIr(2, pos), // [array/(mutableAdd), res, mapper, nextBefore]
            CallIr(1, pos), // [array/(mutableAdd), res, nextAfter]
            CallIr(2, pos), // [res]
            LoadIr("+", pos), // [res, +]
            LoadIr("index", pos), // [res, +, index]
            LoadConstIr(1, pos), // [res, +, index, 1]
            CallIr(2, pos), // [res, index]
            StoreIr("index", pos), // [res]
          ),
          pos = pos
        ),
        // [res]
        ReturnIr(pos)
      ),
      params = listOf(
        ParamMeta("arr", List::class, "array to loop"),
        ParamMeta("mapper", ClosureFunction::class, "function to do mapping")
      ),
      pos = pos
    )))

    global["array/flatMap"] = object: FunctionValue {
      override val name: String = "array/flatMap"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("array", List::class, "array to loop"),
        ParamMeta("mapper", FunctionValue::class, "function to do mapping")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Expected two arguments to 'array/flatMap'")
        }

        val (arrayRaw, func) = args

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
      override val name: String = "array/filter"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("array", List::class, "array to filter"),
        ParamMeta("test", FunctionValue::class, "function to perform testing of values")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Expected two arguments to 'array/filter'")
        }

        val (arrayRaw, func) = args

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
      override val name: String = "array/fold"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("array", List::class, "array fold"),
        ParamMeta("init", Any::class, "starting value for fold"),
        ParamMeta("test", FunctionValue::class, "function to perform merging of values")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 3) {
          pos.interpretFail("Expected three arguments to 'array/fold'")
        }

        val (arrayRaw, init, func) = args

        if (func !is FunctionValue) {
          pos.interpretFail("Expected first argument to 'array/fold' to be a function")
        }

        val array = arrayRaw?.coerceTo(List::class) ?: pos.interpretFail("Expected third argument to 'array/fold' to be an array")

        return array.fold(init) { sum, next -> func.call(listOf(sum, next), pos) }
      }
    }

    global["array/forEach"] = object: FunctionValue {
      override val name: String = "array/forEach"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("array", List::class, "array fold"),
        ParamMeta("action", FunctionValue::class, "function to run on each value")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Expected two arguments to 'array/forEach'")
        }

        val (arrayRaw, func) = args

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
