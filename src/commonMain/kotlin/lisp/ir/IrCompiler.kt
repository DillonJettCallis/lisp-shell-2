package lisp.ir

import lisp.*
import lisp.runtime.Type

class IrCompiler {

  companion object {

    // these are functions that can't be used dynamically because they all require special compiler support
    // they are forbidden as variable names, and can ONLY be used as functions, not any normal variable
    val reservedWords = setOf("def", "defn", "fn", "let", "if", "return", "export", "module", "call", "recurse")

  }

  private fun List<Expression>.unify(): Expression {
    val pos = first().pos
    return CallEx( listOf(VariableEx("do", pos)) + this, pos)
  }

  fun compileBlock(name: String, rawBody: List<Expression>, params: MutableList<ParamMeta> = ArrayList(), isModule: Boolean = false): IrFunction {
    val ex = rawBody.unify()

    val body = compile(ex)

    if (isModule) {
      body += ReturnVoidIr(ex.pos)
    } else {
      if (body.last() is DefineIr) {
        val def = body.removeLast()
        body += DupIr(def.pos)
        body += def
      }

      body += ReturnIr(ex.pos)
    }

    return constructFunction(name, body, params, ex.pos)
  }

  fun constructFunction(name: String, body: MutableList<Ir>, params: MutableList<ParamMeta>, pos: Position): IrFunction {
    AnonArgumentRemover.resolve(body, params)
    val context = ClosureChecker.check(body, params)

    FreeLocalInserter.addFrees(body, params)
    UnusedLocalRemove.removeUnusedLocals(body)

    return IrFunction(
      name = name,
      body = body,
      params = params,
      pos = pos,
      closureContext = context
    )
  }

  private fun compileFunction(name: String, ex: Expression, params: MutableList<ParamMeta>): IrFunction {
    val body = compile(ex)
    body += ReturnIr(ex.pos)

    return IrFunction(
      name = name,
      body = body,
      params = params,
      pos = ex.pos
    )
  }

  private fun compile(ex: Expression): MutableList<Ir> {
    return ArrayList<Ir>().also { internalCompile(ex, it) }
  }

  private fun internalCompile(ex: Expression, init: MutableList<Ir>) {
    when (ex) {
      is LiteralEx -> init += LoadConstIr(ex.value, ex.pos)
      is CommandEx -> {
        init += LoadIr("exec", ex.pos)
        init += LoadConstIr(ex.value, ex.pos)
        init += BuildClosureIr(1, ex.pos)
      }
      is VariableEx -> init += LoadIr(ex.name, ex.pos)
      is OperatorEx -> init += LoadIr(ex.op, ex.pos)
      is ArrayEx -> {
        if (ex.body.all { it is LiteralEx }) {
          val value = ex.body.map { (it as LiteralEx?)?.value }

          init += LoadConstIr(value, ex.pos)
        } else {
          val pos = ex.pos

          init += LoadIr("(arrayBuild)", pos) // prepare the build function => [.., array/build]
          init += CallIr(0, pos) // leaves new array on the stack => [.., arr]

          ex.body.forEach {
            init += LoadIr("(arrayMutableAdd)", pos) // push the add function onto the stack => [..,  arr, array/add]
            init += SwapIr(pos) // swap so the add is lower => [..,  array/add, arr]
            internalCompile(it, init) // push the next item on the stack => [.., array/add, arr, item]
            init += CallIr(2, pos) // call add to push the new item into the array [.., arr]
          }
        }
      }
      is MapEx -> {
        if (ex.body.all { (key, value) -> key is LiteralEx && value is LiteralEx }) {
          val value = ex.body.associate { (key, value) -> (key as LiteralEx?)?.value to (value as LiteralEx?)?.value }

          init += LoadConstIr(value, ex.pos)
        } else {
          val pos = ex.pos

          init += LoadIr("(mapBuild)", pos) // prepare the build function => [.., map/build]
          init += CallIr(0, pos) // leaves new map on the stack => [.., map]

          ex.body.forEach { (key, value) ->
            init += LoadIr("(mapMutableSet)", pos) // push the add function onto the stack => [..,  map, map/add]
            init += SwapIr(pos) // swap so the add is lower => [..,  map/add, map]
            internalCompile(key, init) // push the next key on the stack => [.., map/add, map, key]
            internalCompile(value, init) // push the next value on the stack =>  [.., map/add, map, key, value]
            init += CallIr(3, pos) // call add to push the new pair into the map [.., map]
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
          "cd" -> {
            when (tail.size) {
              1 -> {
                // just a string
                val path = tail.first()

                init += LoadGlobalIr("cwd", path.pos)
                compile(path)

                init += StoreGlobalIr("cwd", path.pos)
              }
              2 -> {
                // a string and a function
              }
              else -> head.pos.compileFail("Invalid use of 'cd'. Must be either one or two arguments")
            }
          }
          "def" -> {
            val (nameEx, body) = tail

            val name = if (nameEx is VariableEx) {
              nameEx.name
            } else {
              nameEx.pos.compileFail("def expected first arg to be variable name")
            }

            if (name in reservedWords) {
              nameEx.pos.compileFail("Reserved name cannot be used as variable name '$name'")
            }

            internalCompile(body, init)

            init += DefineIr(name, nameEx.pos)
          }
          "let" -> {
            val (declares, body) = tail

            if (declares !is MapEx) {
              declares.pos.compileFail("Expected first argument of let to be map of variable names to expressions")
            }

            val letBody = ArrayList<Ir>()
            val names = HashSet<String>()

            declares.body.forEach { (nameEx, content) ->
              if (nameEx !is VariableEx) {
                nameEx.pos.compileFail("Expected to find variable name in let statement")
              }
              val name = nameEx.name
              names += name

              if (name in reservedWords) {
                nameEx.pos.compileFail("Reserved name cannot be used as variable name '$name'")
              }

              internalCompile(content, letBody)

              letBody += StoreIr(name, nameEx.pos)
            }

            internalCompile(body, letBody)

            init += letBody
          }
          "fn" -> {
            val (name, paramList, body) = when(tail.size) {
              2 -> {
                val (paramList, body) = tail

                Triple("anon:${head.pos.src}:${head.pos.line}:${head.pos.col}", paramList, body)
              }
              3 -> {
                val (nameEx, paramList, body) = tail

                if (nameEx !is StringLiteralEx) {
                  nameEx.pos.compileFail("Expected optional first argument to 'fn' to be a function name")
                }

                Triple(nameEx.value, paramList, body)
              }
              else -> head.pos.compileFail("Expected 'fn' to contain either two or three arguments")
            }

            val params = if (paramList is ArrayEx) {
              paramList.body.map {
                when (it) {
                  is VariableEx -> {
                    if (it.name in reservedWords) {
                      it.pos.compileFail("Reserved name cannot be used as variable name '${it.name}'")
                    }

                    ParamMeta(it.name)
                  }
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

                    if (name in reservedWords) {
                      nameEx.pos.compileFail("Reserved name cannot be used as variable name '$name'")
                    }

                    val type: Type = if (it.body.size > 1) {
                      val typeEx = it.body[1]

                      if (typeEx !is StringLiteralEx) {
                        typeEx.pos.compileFail("Expected variable type declaration")
                      } else {
                        Type.coerce(Type.TypeType, typeEx.value) as? Type ?: typeEx.pos.compileFail("Unknown type")
                      }
                    } else {
                      Type.AnyType
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

            val func = compileFunction(name, body, params.toMutableList())

            init += LoadFuncIr(func, ex.pos)
          }
          "if" -> {
            val (conditionEx, thenEx) = tail

            internalCompile(conditionEx, init)

            val thenBlock = compile(thenEx)
            val elseBlock = if (tail.size == 3) compile(tail[2]) else ArrayList()

            init += BranchIr(thenBlock, elseBlock, ex.pos)
          }
          "return" -> {
            if (tail.size != 1) {
              head.pos.compileFail("Expected exactly one argument to function return")
            }

            internalCompile(tail[0], init)
            init += ReturnIr(head.pos)
          }
          "or" -> {
            val (first, second) = tail

            internalCompile(first, init)
            val whenFalse = compile(second)

            init += BranchIr(ArrayList(), whenFalse, ex.pos)
          }
          "and" -> {
            val (first, second) = tail

            internalCompile(first, init)
            val whenTrue = compile(second)

            init += BranchIr(whenTrue, ArrayList(), ex.pos)
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
          "export" -> {
            val (nameEx, valueEx) = when (tail.size) {
              1 -> tail[0] to tail[0]
              2 -> {
                val (nameEx, valueEx) = tail

                nameEx to valueEx
              }
              else -> head.pos.compileFail("Expected exactly one or two arguments to function 'export")
            }

            if (nameEx !is VariableEx) {
              nameEx.pos.compileFail("Expected first argument to 'export' to be a variable name")
            }

            if (nameEx.name in reservedWords) {
              nameEx.pos.compileFail("Reserved name cannot be used as variable name '${nameEx.name}'")
            }

            val nameLiteral = StringLiteralEx(nameEx.name, false, nameEx.pos)

            init += LoadIr(funcName, head.pos)
            internalCompile(nameLiteral, init)
            internalCompile(valueEx, init)

            init += CallIr(2, head.pos)
          }
          "module" -> {
            val name = "module:${head.pos.src}:${head.pos.line}:${head.pos.col}"

            val moduleBody = compileFunction(name, tail.unify(), ArrayList())

            init += LoadFuncIr(moduleBody, head.pos)
            init += BuildModuleIr(head.pos)
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
