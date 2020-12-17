package lisp.ir

import lisp.*
import lisp.coercion.coerceTo
import kotlin.reflect.KClass

class IrCompiler {

  fun compileFunction(ex: Expression, params: List<ParamMeta> = emptyList()): IrFunction {
    return IrFunction(
      body = compile(ex) + ReturnIr(ex.pos),
      params = params,
      pos = ex.pos
    )
  }

  private fun compile(ex: Expression): List<Ir> {
    return ArrayList<Ir>().also { internalCompile(ex, it) }
  }

  private fun internalCompile(ex: Expression, init: MutableList<Ir>) {
    when (ex) {
      is LiteralEx -> init += LoadConstIr(ex.value, ex.pos)
      is CommandEx -> init += BuildShellIr(ex.value, ex.pos)
      is VariableEx -> init += LoadIr(ex.name, ex.pos)
      is OperatorEx -> init += LoadIr(ex.op, ex.pos)
      is ArrayEx -> {
        if (ex.body.all { it is LiteralEx }) {
          val value = ex.body.map { (it as LiteralEx?)?.value }

          init += LoadConstIr(value, ex.pos)
        } else {
          val pos = ex.pos

          init += LoadIr("array/(build)", pos) // prepare the build function => [.., array/build]
          init += CallIr(0, pos) // leaves new array on the stack => [.., arr]

          ex.body.forEach {
            init += LoadIr("array/(mutableAdd)", pos) // push the add function onto the stack => [..,  arr, array/add]
            init += SwapIr(pos) // swap so the add is lower => [..,  array/add, arr]
            internalCompile(it, init) // push the next item on the stack => [.., array/add, arr, item]
            init += CallIr(3, pos) // call add to push the new item into the array [.., arr]
          }
        }
      }
      is MapEx -> {
        if (ex.body.all { (key, value) -> key is LiteralEx && value is LiteralEx }) {
          val value = ex.body.associate { (key, value) -> (key as LiteralEx?)?.value to (value as LiteralEx?)?.value }

          init += LoadConstIr(value, ex.pos)
        } else {
          val pos = ex.pos

          init += LoadIr("map/(build)", pos) // prepare the build function => [.., map/build]
          init += CallIr(0, pos) // leaves new map on the stack => [.., map]

          ex.body.forEach { (key, value) ->
            init += LoadIr("map/(mutablePut)", pos) // push the add function onto the stack => [..,  map, map/add]
            init += SwapIr(pos) // swap so the add is lower => [..,  map/add, map]
            internalCompile(key, init) // push the next key on the stack => [.., map/add, map, key]
            internalCompile(value, init) // push the next value on the stack =>  [.., map/add, map, key, value]
            init += CallIr(4, pos) // call add to push the new pair into the map [.., map]
          }
        }
      }
      is CallEx -> {
        val head = ex.body.first()
        val tail = ex.body.drop(1)

        val funcName = when {
          head is VariableEx -> head.name
          head is StringLiteralEx && !head.quoted -> head.value
          else -> null
        }

        // here we list all the special forms and deal with them specially
        when(funcName) {
          "def" -> {
            val (nameEx, body) = tail

            val name = if (nameEx is VariableEx) {
              nameEx.name
            } else {
              nameEx.pos.compileFail("def expected first arg to be variable name")
            }

            internalCompile(body, init)

            init += DefineIr(name, nameEx.pos)
          }
          "let" -> {
            val (declares, body) = tail

            if (declares !is MapEx) {
              declares.pos.compileFail("Expected first argument of let to be map of variable names to expressions")
            }

            declares.body.forEach { (nameEx, content) ->
              if (nameEx !is VariableEx) {
                nameEx.pos.compileFail("Expected to find variable name in let statement")
              }
              val name = nameEx.name

              internalCompile(content, init)

              init += StoreIr(name, nameEx.pos)
            }

            internalCompile(body, init)
          }
          "fn" -> {
            val (paramList, body) = tail

            val params = if (paramList is ArrayEx) {
              paramList.body.map {
                when (it) {
                  is VariableEx -> ParamMeta(it.name, Any::class, "${it.name} Any")
                  is ArrayEx -> {
                    if (it.body.isEmpty()) {
                      it.pos.compileFail("Expected variable declaration")
                    }

                    val nameEx = it.body.first()

                    val name = if (nameEx !is VariableEx) {
                      nameEx.pos.compileFail("Expected variable declaration")
                    } else {
                      nameEx.name
                    }

                    val type: KClass<*> = if (it.body.size > 1) {
                      val typeEx = it.body[1]

                      if (typeEx !is StringLiteralEx) {
                        typeEx.pos.compileFail("Expected variable type declaration")
                      } else {
                        typeEx.value.coerceTo(KClass::class::class) ?: typeEx.pos.compileFail("Unknown type")
                      }
                    } else {
                      Any::class
                    }

                    val desc = if (it.body.size > 2) {
                      val descEx = it.body[2]

                      if (descEx !is StringLiteralEx) {
                        descEx.pos.compileFail("Expected variable description")
                      } else {
                        descEx.value
                      }
                    } else {
                      "$name $type"
                    }

                    if (it.body.size > 3) {
                      it.pos.interpretFail("Expected variable declaration to contain 1 to 3 arguments. Actually contained ${it.body.size}")
                    }

                    ParamMeta(name, type, desc)
                  }
                  else -> it.pos.compileFail("Expected all args of array to be variable names or array of variable descriptions")
                }
              }
            } else {
              paramList.pos.compileFail("Expected first arg to fn to be an array of variable names")
            }

            val func = compileFunction(body, params)

            init += BuildClosureIr(func, ex.pos)
          }
          "if" -> {
            val (conditionEx, thenEx) = tail

            internalCompile(conditionEx, init)

            val thenBlock = compile(thenEx)
            val elseBlock = if (tail.size == 3) compile(tail[2]) else emptyList()

            init += BranchIr(thenBlock, elseBlock, ex.pos)
          }
          "||" -> {
            val (first, second) = tail

            internalCompile(first, init)
            val whenFalse = compile(second)

            init += BranchIr(emptyList(), whenFalse, ex.pos)
          }
          "&&" -> {
            val (first, second) = tail

            internalCompile(first, init)
            val whenTrue = compile(second)

            init += BranchIr(whenTrue, emptyList(), ex.pos)
          }
          "do" -> {
            // do is flattened out of existence and a pop added after all but the last expression

            tail.forEach {
              internalCompile(it, init)

              val last = init.last()

              if (last is DefineIr || last is StoreIr) {
                // define and store leave nothing on stack. Nothing to pop
              } else {
                init += PopIr(it.pos) // pop each item
              }
            }

            val last = init.last()

            if (last is DefineIr || last is StoreIr) {
              // define and store leave nothing on stack. Nothing to pop
            } else {
              init.removeLast() // don't pop the last item, leave it on the stack
            }
          }
          "call" -> {
            // takes a function and an array of the args

            if (tail.size != 2) {
              head.pos.compileFail("Expected exactly two arguments to function 'call'")
            }

            val (funcEx, argsEx) = tail

            internalCompile(funcEx, init)
            internalCompile(argsEx, init)

            init += CallDynamicIr(head.pos)
          }
          null -> { // null means the function isn't a variable or unquoted string
            ex.body.forEach {
              internalCompile(it, init)
            }

            init += CallIr(tail.size, ex.pos)
          }
          else -> { // else means it's either a variable or unquoted string, we'll transform it into a variable regardless
            init += LoadIr(funcName, head.pos)

            tail.forEach {
              internalCompile(it, init)
            }

            init += CallIr(tail.size, ex.pos)
          }
        }
      }
    }
  }
}