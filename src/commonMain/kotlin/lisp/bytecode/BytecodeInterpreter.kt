package lisp.bytecode

import lisp.Command
import lisp.File
import lisp.Position
import lisp.Scope
import lisp.runtime.Type

class BytecodeInterpreter(private val shell: Command) {

  private val codes = Bytecode.values()

  fun interpret(scope: Scope, func: BytecodeFunction, args: Array<Any?>, closures: Array<Any?>): Any? {
    val body = func.code
    val size = body.size
    val stack = ArrayStack(func.maxStack)
    val locals = args.copyOf(func.maxLocals)

    // copy closures into locals
    if (closures.isNotEmpty()) {
      closures.copyInto(locals, args.size)
    }

    var index = 0

    while (index < size) {
      val next = body[index].let { if (it < 0 || it >= codes.size) func.invalidBytecode(it) else codes[it] }

      when (next) {
        Bytecode.NoOp -> {} // do nothing
        Bytecode.Pop -> stack.pop(func, index)
        Bytecode.Dup -> stack.push(func, index, stack.peek(func, index))
        Bytecode.Swap -> {
          val first = stack.pop(func, index)
          val second = stack.pop(func, index)
          stack.push(func, index, first)
          stack.push(func, index, second)
        }
        Bytecode.Increment -> {
          val last = stack.pop(func, index) as? Int ?: func.fail(index, "Attempt to increment value that was not an int")
          stack.push(func, index, last + 1)
        }
        Bytecode.Decrement -> {
          val last = stack.pop(func, index) as? Int ?: func.fail(index, "Attempt to decrement value that was not an int")
          stack.push(func, index, last - 1)
        }
        Bytecode.Define -> {
          val value = stack.pop(func, index)
          val name = func.getString(index, body[++index])

          scope.define(name, value)
        }
        Bytecode.Store -> {
          val value = stack.pop(func, index)
          val localIndex = body[++index]

          locals[localIndex] = value
        }
        Bytecode.LoadLocal -> {
          val localIndex = body[++index]

          stack.push(func, index, locals[localIndex])
        }
        Bytecode.LoadScope -> {
          val name = func.getString(index, body[++index])

          stack.push(func, index, scope[name])
        }
        Bytecode.LoadNull -> stack.push(func, index, null)
        Bytecode.LoadTrue -> stack.push(func, index, true)
        Bytecode.LoadFalse -> stack.push(func, index, false)
        Bytecode.LoadInt -> stack.push(func, index, body[++index])
        Bytecode.LoadDouble -> {
          val top = body[++index]
          val bottom = body[++index]
          val longValue = top.toLong().shl(32) or bottom.toLong()

          stack.push(func, index, Double.fromBits(longValue))
        }
        Bytecode.LoadRecurse -> stack.push(func, index, ClosureFunction(scope, closures, func))
        Bytecode.LoadArgArray -> stack.push(func, index, args.toList())
        Bytecode.LoadConst -> stack.push(func, index, func.constants[body[++index]])
        Bytecode.Call -> {
          val pos = func.posArray[index]
          val initIndex = index
          val argCount = body[++index]

          val params = (0 until argCount).map { stack.pop(func, initIndex) }.reversed()
          val callFunc = stack.pop(func, initIndex)

          stack.push(func, initIndex, callFunction(callFunc, params, scope, func, pos, initIndex))
        }
        Bytecode.CallDynamic -> {
          val pos = func.posArray[index]

          val params = stack.pop(func, index) as? List<*> ?: func.fail(index, "Expected array of args passed to 'call' function")
          val callFunc = stack.pop(func, index)

          stack.push(func, index, callFunction(callFunc, params, scope, func, pos, index))
        }
        Bytecode.Return -> return stack.pop(func, index)
        Bytecode.BuildShell -> {
          val path = func.getString(index, body[++index])

          stack.push(func, index, ShellFunction(path))
        }
        Bytecode.BuildClosure -> {
          val startIndex = index
          val closureSize = body[++index]

          val closure = arrayOfNulls<Any?>(closureSize)

          (0 until closureSize).forEach {
            closure[it] = stack.pop(func, startIndex)
          }

          val funcCall = stack.pop(func, startIndex) as BytecodeFunction

          stack.push(func, index, ClosureFunction(scope, closure, funcCall))
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
      is ClosureFunction -> {
        val args = Type.coerceAll(params, callFunc.code.params, pos).toTypedArray()

        interpret(callFunc.scope, callFunc.code, args, callFunc.closure)
      }
      is NativeFunction -> callFunc.call(Type.coerceAll(params, callFunc.params, pos), pos)
      is ShellFunction -> {
        val cwd = Type.coerce(Type.FileType, scope["cwd"]) as? File ?: func.fail(index, "Expected 'cwd' to be a file")

        shell.execute(cwd, callFunc.name, params.map { it.stringify(func, index) })
      }
      else -> {
        func.fail(index, "Value is not function")
      }
    }
  }

  private fun Any?.stringify(func: BytecodeFunction, index: Int): String {
    return when(this) {
      null -> "null"
      is BytecodeFunction, is ClosureFunction, is NativeFunction -> func.fail(index, "Cannot render function to string")
      else -> toString()
    }
  }
}

// dead simple stack that does exactly what we want and nothing more
class ArrayStack(size: Int) {

  private val content = arrayOfNulls<Any?>(size)
  private var index = 0

  fun pop(func: BytecodeFunction, pos: Int): Any? {
    if (index == 0) {
      func.invalidBytecode(pos)
    } else {
      return content[--index]
    }
  }

  fun peek(func: BytecodeFunction, pos: Int): Any? {
    if (index == 0) {
      func.invalidBytecode(pos)
    } else {
      return content[index - 1]
    }
  }

  fun push(func: BytecodeFunction, pos: Int, item: Any?) {
    if (index == content.size) {
      func.invalidBytecode(pos)
    } else {
      content[index++] = item
    }
  }
}
