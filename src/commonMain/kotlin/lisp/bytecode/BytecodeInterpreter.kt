package lisp.bytecode

import lisp.*
import lisp.coercion.coerceTo
import lisp.coercion.coerceAll

class BytecodeInterpreter(private val shell: Command) {

  private val codes = Bytecode.values()

  fun interpret(parentScope: Scope, func: BytecodeFunction, args: List<Any?>): Any? {
    val body = func.code
    val size = body.size
    val stack = ArrayList<Any?>()
    val locals = parentScope.child()

    args.forEachIndexed { index, value ->
      locals["$index"] = value

      if (func.params.size > index) {
        locals[func.params[index].name] = value
      }
    }

    var index = 0

    while (index < size) {
      val next = body[index].let { if (it < 0 || it >= codes.size) func.invalidBytecode(it) else codes[it] }

      when (next) {
        Bytecode.NoOp -> {} // do nothing
        Bytecode.Pop -> stack.pop(func, index)
        Bytecode.Dup -> stack.push(stack.peek(func, index))
        Bytecode.Swap -> {
          val first = stack.pop(func, index)
          val second = stack.pop(func, index)
          stack.push(first)
          stack.push(second)
        }
        Bytecode.Define -> {
          val value = stack.pop(func, index)
          val name = func.getString(index, body[++index])

          parentScope.define(name, value)
        }
        Bytecode.Store -> {
          val value = stack.pop(func, index)
          val name = func.getString(index, body[++index])

          locals[name] = value
        }
        Bytecode.Load -> {
          val name = func.getString(index, body[++index])

          stack.push(locals[name])
        }
        Bytecode.LoadNull -> stack.push(null)
        Bytecode.LoadTrue -> stack.push(true)
        Bytecode.LoadFalse -> stack.push(false)
        Bytecode.LoadInt -> stack.push(body[++index])
        Bytecode.LoadDouble -> {
          val top = body[++index]
          val bottom = body[++index]
          val longValue = top.toLong().shl(32) or bottom.toLong()

          stack.push(Double.fromBits(longValue))
        }
        Bytecode.LoadConst -> stack.push(func.constants[body[++index]])
        Bytecode.Call -> {
          val pos = func.posArray[index]
          val initIndex = index
          val argCount = body[++index]

          val params = (0 until argCount).map { stack.pop(func, initIndex) }.reversed()
          val callFunc = stack.pop(func, initIndex)

          stack.push(callFunction(callFunc, params, locals, func, pos, initIndex))
        }
        Bytecode.CallDynamic -> {
          val pos = func.posArray[index]

          val params = stack.pop(func, index)?.coerceTo(List::class) ?: func.fail(index, "Expected array of args passed to 'call' function")
          val callFunc = stack.pop(func, index)

          stack.push(callFunction(callFunc, params, locals, func, pos, index))
        }
        Bytecode.Return -> return stack.pop(func, index)
        Bytecode.BuildShell -> {
          val path = func.getString(index, body[++index])

          stack.push(ShellFunction(path))
        }
        Bytecode.BuildClosure -> {
          val funcCall = func.constants[body[++index]] as BytecodeFunction

          stack.push(ClosureFunction(locals, funcCall))
        }
        Bytecode.Jump -> {
          val initIndex = index
          val offset = body[++index]

          index += offset

          if (index < 0 || index >= size) {
            func.invalidBytecode(initIndex)
          }

          continue // don't allow the when to exit as that would ++ index which we don't want
        }
        Bytecode.Branch -> {
          val initIndex = index
          val offset = body[++index]
          val condition = stack.pop(func, initIndex)

          // branch is backwards. We jump if false, fall if true
          if (condition == null || condition == false) {
            index += offset

            if (index < 0 || index >= size) {
              func.invalidBytecode(initIndex)
            }

            continue // don't allow the when to exit as that would ++ index which we don't want
          }
        }
      }

      index++
    }

    func.invalidBytecode(body.size - 1)
  }

  private fun callFunction(callFunc: Any?, params: List<Any?>, scope: Scope, func: BytecodeFunction, pos: Position, index: Int): Any? {
    return when (callFunc) {
      is ClosureFunction -> interpret(callFunc.scope, callFunc.code, params.coerceAll(callFunc.code.params, pos))
      is NativeFunction -> callFunc.call(params.coerceAll(callFunc.params, pos), pos)
      is ShellFunction -> {
        val cwd = scope["cwd"]?.coerceTo(File::class) ?: func.fail(index, "Expected 'cwd' to be a file")

        shell.execute(cwd, callFunc.name, params.map { it.stringify(func, index) })
      }
      else -> {
        func.fail(index, "Value is not function")
      }
    }
  }

  private fun <Item> MutableList<Item>.pop(func: BytecodeFunction, index: Int): Item? {
    if (isEmpty()) {
      func.invalidBytecode(index)
    } else {
      return removeLast()
    }
  }

  private fun <Item> MutableList<Item>.peek(func: BytecodeFunction, index: Int): Item? {
    if (isEmpty()) {
      func.invalidBytecode(index)
    } else {
      return last()
    }
  }

  private fun <Item> MutableList<Item>.push(item: Item) = add(item)

  private fun Any?.stringify(func: BytecodeFunction, index: Int): String {
    return when(this) {
      null -> "null"
      is ShellFunction -> name
      is BytecodeFunction, is NativeFunction -> func.fail(index, "Cannot render function to string")
      else -> toString()
    }
  }
}


