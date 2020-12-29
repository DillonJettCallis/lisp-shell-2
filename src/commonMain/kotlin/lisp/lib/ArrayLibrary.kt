package lisp.lib

import lisp.*
import lisp.runtime.Type

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
        ParamMeta("array", Type.ArrayType, "array to add to"),
        ParamMeta("next", Type.AnyType, "next item to add")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val arr = args[0] as ArrayList<Any?>
        arr.add(args[1])
        return arr
      }
    }

    global["array/size"] = object: FunctionValue {
      override val name: String = "array/size"
      override val params: List<ParamMeta> = listOf(ParamMeta("array", Type.ArrayType, "array to check"))

      override fun call(args: List<Any?>, pos: Position): Any? {
        val (arrayRaw) = args

        val array = arrayRaw as List<*>

        return array.size
      }
    }

    global["array/get"] = object: FunctionValue {
      override val name: String = "array/get"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("array", Type.ArrayType, "array to access"),
        ParamMeta("index", Type.IntegerType, "index of array")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val (arrayRaw, indexRaw) = args

        val index = indexRaw as Int
        val array = arrayRaw as List<*>

        return array[index]
      }
    }

    global["array/set"] = object: FunctionValue {
      override val name: String = "array/set"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("array", Type.ArrayType, "array to access"),
        ParamMeta("index", Type.IntegerType, "index of array"),
        ParamMeta("value", Type.AnyType, "value to set in array")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 3) {
          pos.interpretFail("Expected three arguments to 'array/set'")
        }

        val (arrayRaw, indexRaw, value) = args

        val index = indexRaw as Int
        val array = ArrayList(arrayRaw as List<Any?>)

        array[index] = value
        return array
      }
    }

    global["array/add"] = object: FunctionValue {
      override val name: String = "array/add"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("array", Type.ArrayType, "array to access"),
        ParamMeta("value", Type.AnyType, "value to add to end of array")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Expected three arguments to 'array/add'")
        }

        val (arrayRaw, value) = args

        val array = arrayRaw as List<Any?>

        return array + value
      }
    }

    global.compileNative(
      name = "array/map",
      params = mutableListOf(
        ParamMeta("arr", Type.ArrayType, "array to loop"),
        ParamMeta("mapper", Type.FunctionType, "function to do mapping")
      )
    ) {
      load("array/size") // [array/size]
      load("arr") // [array/size, arr]
      call(1) // [size]
      store("size") // []
      load("array/(build)") // [array/(build)]
      call(0) // [res]
      loadConst(0) // [res, index]
      store("index") // [res]

      loop (
        conditionBlock = {
          // [res]
          load("!=") // [res, !=]
          load("size") // [res, !=, size]
          load("index") // [res, !=, size, index]
          call(2) // [res, isNotEqual]
        },
        bodyBlock = {
          // [res]
          load("array/(mutableAdd)") // [res, array/(mutableAdd)]
          swap() // [array/(mutableAdd), res]
          load("mapper") // [array/(mutableAdd), res, mapper]
          load("array/get") // [array/(mutableAdd), res, mapper, array/get]
          load("arr") // [array/(mutableAdd), res, mapper, array/get, arr]
          load("index") // [array/(mutableAdd), res, mapper, array/get, arr, index]
          call(2) // [array/(mutableAdd), res, mapper, nextBefore]
          call(1) // [array/(mutableAdd), res, nextAfter]
          call(2) // [res]
          load("index") // [res, index]
          increment() // [res, index]
          store("index") // [res]
        }
      )
      // [res]
      returnIr()
    }

    global.compileNative(
      name = "array/flatMap",
      params = mutableListOf(
        ParamMeta("arr", Type.ArrayType, "array to loop"),
        ParamMeta("mapper", Type.FunctionType, "function to do mapping")
      )
    ) {
      // init size for loop
      load("array/size") // [array/size]
      load("arr") // [array/size, arr]
      call(1) // [size]
      store("size") // []

      // init index
      loadConst(0) // [index]
      store("index") // []

      // build new array
      load("array/(build)") // [array/(build)]
      call(0) // [res]

      loop (
        conditionBlock = {
          // [res]

          load("!=") // [res, !=]
          load("index") // [res, !=, index]
          load("size") // [res, !=, index, size])
          call(2) // [res, isNotEqual])
        },

        bodyBlock = {
          load("as") // [res, as]
          loadConst("Array") // [res, as, Array]
          load("mapper") // [res, as, Array, mapper]
          load("array/get") // [res, as, Array, mapper, array/get]
          load("arr") // [res, as, Array, mapper, array/get, arr]
          load("index") // [res, as, Array, mapper, array/get, arr, index]
          dup() // [res, as, Array, mapper, array/get, arr, index, index]
          inc() // [res, as, Array, mapper, array/get, arr, index, nextIndex]
          store("index") // [res, as, Array, mapper, array/get, arr, index]
          call(2) // [res, as, Array, mapper, nextBefore]
          call(1) // [res, as, Array, nextMapped]
          call(2) // [res, nextArray]

          loadConst(0) // [res, nextArray, 0]
          store("innerIndex") // [res, nextArray]
          dup() // [res, nextArray, nextArray]
          store("nextArray") // [res, nextArray]
          load("array/size") // [res, nextArray, array/size]
          swap() // [res, array/size, nextArray]
          call(1) // [res, innerSize]
          store("innerSize") // [res]

          loop (
            conditionBlock = {
              // [res]

              load("!=") // [res, !=]
              load("innerIndex") // [res, !=, innerIndex]
              load("innerSize") // [res, !=, innerIndex, innerSize])
              call(2) // [res, isNotEqual])
            },

            bodyBlock = {
              // [res]

              load("array/(mutableAdd)") // [res, array/(mutableAdd)]
              swap() // [array/(mutableAdd), res]
              load("array/get") // [array/(mutableAdd), res, array/get]
              load("nextArray") // [array/(mutableAdd), res, array/get, nextArray]
              load("innerIndex") // [array/(mutableAdd), res, array/get, nextArray, innerIndex]
              dup() // [array/(mutableAdd), res, array/get, nextArray, innerIndex, innerIndex]
              inc() // [array/(mutableAdd), res, array/get, nextArray, innerIndex, nextInnerIndex]
              store("innerIndex") // [array/(mutableAdd), res, array/get, nextArray, innerIndex]
              call(2) // [array/(mutableAdd), res, nextItem]
              call(2) // [res]
            }
          )
        }
      )

      // [res]
      returnIr()
    }

    global.compileNative(
      name = "array/filter",
      params = arrayListOf(
        ParamMeta("arr", Type.ArrayType, "array to filter"),
        ParamMeta("test", Type.FunctionType, "function to test each item against")
      )
    ) {
      // init size for loop
      load("array/size") // [array/size]
      load("arr") // [array/size, arr]
      call(1) // [size]
      store("size") // []

      // init index
      loadConst(0) // [index]
      store("index") // []

      // build new array
      load("array/(build)") // [array/(build)]
      call(0) // [res]
      store("res") // []

      loop (
        conditionBlock = {
          load("!=") // [!=]
          load("index") // [!=, index]
          load("size") // [!=, index, size])
          call(2) // [isNotEqual])
        },

        bodyBlock = {
          load("array/get") // [array/get]
          load("arr") // [array/get, arr]
          load("index") // [array/get, arr, index]
          dup() // [array/get, arr, index, index]
          inc() // [array/get, arr, index, nextIndex]
          store("index") // [array/get, arr, index]
          call(2) // [next]
          dup() // [next, next]
          load("test") // [next, next, test]
          swap() // [next, test, next]
          call(1) // [next, isPassed]

          branch(
            thenBlock = {
              // [next]
              load("array/(mutableAdd)") // [next, array/(mutableAdd)]
              swap() // [array/(mutableAdd), next]
              load("res") // [array/(mutableAdd), next, res]
              swap() // [array/(mutableAdd), res, next]
              call(2) // [res]
              store("res") // []
            },
            elseBlock = {
              // [next]
              pop() // []
            }
          )
        }
      )

      // []
      load("res") // [res]
      returnIr() // []
    }

    global.compileNative(
      name = "array/fold",
      params = arrayListOf(
        ParamMeta("arr", Type.ArrayType, "array to fold"),
        ParamMeta("init", Type.AnyType, "starting value for fold"),
        ParamMeta("merge", Type.FunctionType, "function to perform merging of values")
      )
    ) {
      load("array/size") // [array/size]
      load("arr") // [array/size, arr]
      call(1) // [size]
      store("size")  // []

      loadConst(0) // [0]
      store("index") // []

      load("init") // [res]

      loop(
        conditionBlock = {
          // [res]
          load("!=") // [res, !=]
          load("size") // [res, !=, size]
          load("index") // [res, !=, size, index]
          call(2) // [res, isNotDone]
        },
        bodyBlock = {
          // [res]
          load("merge") // [res, merge]
          swap() // [merge, res]
          load("array/get") // [merge, res, array/get]
          load("arr") // [merge, res, array/get, arr]
          load("index") // [merge, res, array/get, arr, index]
          dup() // [merge, res, array/get, arr, index, index]
          inc() // [merge, res, array/get, arr, index, nextIndex]
          store("index") // [merge, res, array/get, arr, index]
          call(2) // [merge, res, next]
          call(2) // [res]
        }
      )
      // [res]
      returnIr() // []
    }

    global.compileNative(
      name = "array/forEach",
      params = arrayListOf(
        ParamMeta("arr", Type.ArrayType, "array fold"),
        ParamMeta("action", Type.FunctionType, "function to run on each value")
      )
    ) {
      load("array/size") // [array/size]
      load("arr") // [array/size, arr]
      call(1) // [size]
      store("size")  // []

      loadConst(0) // [0]
      store("index") // []

      loop(
        conditionBlock = {
          // []
          load("!=") // [!=]
          load("size") // [!=, size]
          load("index") // [!=, size, index]
          call(2) // [isNotDone]
        },
        bodyBlock = {
          // []
          load("action") // [action]
          load("array/get") // [action, array/get]
          load("arr") // [action, array/get, arr]
          load("index") // [action, array/get, arr, index]
          dup() // [action, array/get, arr, index, index]
          inc() // [action, array/get, arr, index, nextIndex]
          store("index") // [action, array/get, arr, index]
          call(2) // [action, next]
          call(1) // [null]
          pop() // []
        }
      )
      // []
      loadConst(null) // [null]
      returnIr() // []
    }
  }

}
