package lisp.lib

import lisp.*
import lisp.runtime.Type

object ArrayLibrary: Library {

  override fun addLib(global: Scope) {
    global["size"] = object: FunctionValue {
      override val name: String = "size"
      override val params: List<ParamMeta> = listOf(ParamMeta("array", Type.ArrayType, "array to check"))

      override fun call(args: List<Any?>, pos: Position): Any? {
        val (arrayRaw) = args

        val array = arrayRaw as List<*>

        return array.size
      }
    }

    global["get"] = object: FunctionValue {
      override val name: String = "get"
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

    global["set"] = object: FunctionValue {
      override val name: String = "set"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("array", Type.ArrayType, "array to access"),
        ParamMeta("index", Type.IntegerType, "index of array"),
        ParamMeta("value", Type.AnyType, "value to set in array")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 3) {
          pos.interpretFail("Expected three arguments to 'set'")
        }

        val (arrayRaw, indexRaw, value) = args

        val index = indexRaw as Int
        val array = ArrayList(arrayRaw as List<Any?>)

        array[index] = value
        return array
      }
    }

    global["add"] = object: FunctionValue {
      override val name: String = "add"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("array", Type.ArrayType, "array to access"),
        ParamMeta("value", Type.AnyType, "value to add to end of array")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 2) {
          pos.interpretFail("Expected three arguments to 'add'")
        }

        val (arrayRaw, value) = args

        val array = arrayRaw as List<Any?>

        return array + value
      }
    }

    global.compileNative(
      name = "map",
      params = mutableListOf(
        ParamMeta("arr", Type.ArrayType, "array to loop"),
        ParamMeta("mapper", Type.FunctionType, "function to do mapping")
      )
    ) {
      load("size") // [size]
      load("arr") // [size, arr]
      call(1) // [arrSize]
      store("arrSize") // []
      load("(arrayBuild)") // [(arrayBuild)]
      call(0) // [res]
      loadConst(0) // [res, index]
      store("index") // [res]

      loop (
        conditionBlock = {
          // [res]
          load("!=") // [res, !=]
          load("arrSize") // [res, !=, arrSize]
          load("index") // [res, !=, arrSize, index]
          call(2) // [res, isNotEqual]
        },
        bodyBlock = {
          // [res]
          load("(arrayMutableAdd)") // [res, (arrayMutableAdd)]
          swap() // [(arrayMutableAdd), res]
          load("mapper") // [(arrayMutableAdd), res, mapper]
          load("get") // [(arrayMutableAdd), res, mapper, get]
          load("arr") // [(arrayMutableAdd), res, mapper, get, arr]
          load("index") // [(arrayMutableAdd), res, mapper, get, arr, index]
          call(2) // [(arrayMutableAdd), res, mapper, nextBefore]
          call(1) // [(arrayMutableAdd), res, nextAfter]
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
      name = "flatMap",
      params = mutableListOf(
        ParamMeta("arr", Type.ArrayType, "array to loop"),
        ParamMeta("mapper", Type.FunctionType, "function to do mapping")
      )
    ) {
      // init size for loop
      load("size") // [size]
      load("arr") // [size, arr]
      call(1) // [arrSize]
      store("arrSize") // []

      // init index
      loadConst(0) // [index]
      store("index") // []

      // build new array
      load("(arrayBuild)") // [(arrayBuild)]
      call(0) // [res]

      loop (
        conditionBlock = {
          // [res]

          load("!=") // [res, !=]
          load("index") // [res, !=, index]
          load("arrSize") // [res, !=, index, arrSize])
          call(2) // [res, isNotEqual])
        },

        bodyBlock = {
          load("as") // [res, as]
          loadConst("Array") // [res, as, Array]
          load("mapper") // [res, as, Array, mapper]
          load("get") // [res, as, Array, mapper, get]
          load("arr") // [res, as, Array, mapper, get, arr]
          load("index") // [res, as, Array, mapper, get, arr, index]
          dup() // [res, as, Array, mapper, get, arr, index, index]
          inc() // [res, as, Array, mapper, get, arr, index, nextIndex]
          store("index") // [res, as, Array, mapper, get, arr, index]
          call(2) // [res, as, Array, mapper, nextBefore]
          call(1) // [res, as, Array, nextMapped]
          call(2) // [res, nextArray]

          loadConst(0) // [res, nextArray, 0]
          store("innerIndex") // [res, nextArray]
          dup() // [res, nextArray, nextArray]
          store("nextArray") // [res, nextArray]
          load("size") // [res, nextArray, size]
          swap() // [res, size, nextArray]
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

              load("(arrayMutableAdd)") // [res, (arrayMutableAdd)]
              swap() // [(arrayMutableAdd), res]
              load("get") // [(arrayMutableAdd), res, get]
              load("nextArray") // [(arrayMutableAdd), res, get, nextArray]
              load("innerIndex") // [(arrayMutableAdd), res, get, nextArray, innerIndex]
              dup() // [(arrayMutableAdd), res, get, nextArray, innerIndex, innerIndex]
              inc() // [(arrayMutableAdd), res, get, nextArray, innerIndex, nextInnerIndex]
              store("innerIndex") // [(arrayMutableAdd), res, get, nextArray, innerIndex]
              call(2) // [(arrayMutableAdd), res, nextItem]
              call(2) // [res]
            }
          )
        }
      )

      // [res]
      returnIr()
    }

    global.compileNative(
      name = "filter",
      params = arrayListOf(
        ParamMeta("arr", Type.ArrayType, "array to filter"),
        ParamMeta("test", Type.FunctionType, "function to test each item against")
      )
    ) {
      // init size for loop
      load("size") // [size]
      load("arr") // [size, arr]
      call(1) // [arrSize]
      store("arrSize") // []

      // init index
      loadConst(0) // [index]
      store("index") // []

      // build new array
      load("(arrayBuild)") // [(arrayBuild)]
      call(0) // [res]
      store("res") // []

      loop (
        conditionBlock = {
          load("!=") // [!=]
          load("index") // [!=, index]
          load("arrSize") // [!=, index, arrSize])
          call(2) // [isNotEqual])
        },

        bodyBlock = {
          load("get") // [get]
          load("arr") // [get, arr]
          load("index") // [get, arr, index]
          dup() // [get, arr, index, index]
          inc() // [get, arr, index, nextIndex]
          store("index") // [get, arr, index]
          call(2) // [next]
          dup() // [next, next]
          load("test") // [next, next, test]
          swap() // [next, test, next]
          call(1) // [next, isPassed]

          branch(
            thenBlock = {
              // [next]
              load("(arrayMutableAdd)") // [next, (arrayMutableAdd)]
              swap() // [(arrayMutableAdd), next]
              load("res") // [(arrayMutableAdd), next, res]
              swap() // [(arrayMutableAdd), res, next]
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
      name = "fold",
      params = arrayListOf(
        ParamMeta("arr", Type.ArrayType, "array to fold"),
        ParamMeta("init", Type.AnyType, "starting value for fold"),
        ParamMeta("merge", Type.FunctionType, "function to perform merging of values")
      )
    ) {
      load("size") // [size]
      load("arr") // [size, arr]
      call(1) // [arrSize]
      store("arrSize")  // []

      loadConst(0) // [0]
      store("index") // []

      load("init") // [res]

      loop(
        conditionBlock = {
          // [res]
          load("!=") // [res, !=]
          load("arrSize") // [res, !=, arrSize]
          load("index") // [res, !=, arrSize, index]
          call(2) // [res, isNotDone]
        },
        bodyBlock = {
          // [res]
          load("merge") // [res, merge]
          swap() // [merge, res]
          load("get") // [merge, res, get]
          load("arr") // [merge, res, get, arr]
          load("index") // [merge, res, get, arr, index]
          dup() // [merge, res, get, arr, index, index]
          inc() // [merge, res, get, arr, index, nextIndex]
          store("index") // [merge, res, get, arr, index]
          call(2) // [merge, res, next]
          call(2) // [res]
        }
      )
      // [res]
      returnIr() // []
    }

    global.compileNative(
      name = "forEach",
      params = arrayListOf(
        ParamMeta("arr", Type.ArrayType, "array fold"),
        ParamMeta("action", Type.FunctionType, "function to run on each value")
      )
    ) {
      load("size") // [size]
      load("arr") // [size, arr]
      call(1) // [size]
      store("arrSize")  // []

      loadConst(0) // [0]
      store("index") // []

      loop(
        conditionBlock = {
          // []
          load("!=") // [!=]
          load("arrSize") // [!=, arrSize]
          load("index") // [!=, arrSize, index]
          call(2) // [isNotDone]
        },
        bodyBlock = {
          // []
          load("action") // [action]
          load("get") // [action, get]
          load("arr") // [action, get, arr]
          load("index") // [action, get, arr, index]
          dup() // [action, get, arr, index, index]
          inc() // [action, get, arr, index, nextIndex]
          store("index") // [action, get, arr, index]
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
