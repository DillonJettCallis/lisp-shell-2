package lisp.bytecode

import lisp.FunctionValue
import lisp.ParamMeta
import lisp.Position
import lisp.Scope
import lisp.ir.IrFunction

class BytecodeFunction(
  val params: List<ParamMeta>,
  val code: IntArray,
  val maxLocals: Int,
  val maxStack: Int,
  val ir: IrFunction,
  val posArray: Array<Position>,
  val constants: Array<Any>,
  val stringConstants: Array<String>
) {

  fun fail(index: Int, message: String, cause: Throwable? = null): Nothing {
    posArray[index].interpretFail(message, cause)
  }

  fun invalidBytecode(index: Int): Nothing {
    fail(index, "Invalid Bytecode at index: $index")
  }

  fun getString(code: Int, index: Int): String {
    if (index < 0 || index > stringConstants.size) {
      fail(code, "Invalid Bytecode at index: $code. No string constant at index $index")
    } else {
      return stringConstants[index]
    }
  }

  fun decompile(): String {
    val bytecodes = Bytecode.values()
    val buffer = StringBuilder()
    var index = 0

    while (index < code.size) {
      val next = bytecodes[code[index++]]

      when (next) {
        Bytecode.NoOp -> buffer += "NoOp"
        Bytecode.Pop -> buffer += "Pop"
        Bytecode.Dup -> buffer += "Dup"
        Bytecode.Swap -> buffer += "Swap"
        Bytecode.Increment -> buffer += "Increment"
        Bytecode.Decrement -> buffer += "Decrement"
        Bytecode.Define -> {
          val name = getString(index, code[index++])
          buffer += "Define -> $name"
        }
        Bytecode.Store -> {
          val slot = code[index++]
          buffer += "Store -> $slot"
        }
        Bytecode.LoadLocal -> {
          val slot = code[index++]
          buffer += "LoadLocal -> $slot"
        }
        Bytecode.LoadScope -> {
          val name = getString(index, code[index++])
          buffer += "LoadScope -> $name"
        }
        Bytecode.LoadRecurse -> buffer += "LoadRecurse"
        Bytecode.LoadArgArray -> buffer += "LoadArgArray"
        Bytecode.LoadNull -> buffer += "LoadNull"
        Bytecode.LoadTrue -> buffer += "LoadTrue"
        Bytecode.LoadFalse -> buffer += "LoadFalse"
        Bytecode.LoadInt -> {
          val value = code[index++]
          buffer += "LoadInt -> $value"
        }
        Bytecode.LoadDouble -> {
          val top = code[index++]
          val bottom = code[index++]
          val longValue = top.toLong().shl(32) or bottom.toLong()

          buffer += "LoadDouble -> ${Double.fromBits(longValue)}"
        }
        Bytecode.LoadConst -> {
          val constIndex = code[index++]

          buffer += "LoadConst -> $constIndex"
        }
        Bytecode.Call -> {
          val argCount = code[index++]

          buffer += "Call -> $argCount"
        }
        Bytecode.CallDynamic -> buffer += "CallDynamic"
        Bytecode.Return -> buffer += "Return"
        Bytecode.BuildShell -> {
          val constIndex = code[index++]
          buffer += "BuildShell -> $constIndex"
        }
        Bytecode.BuildClosure -> {
          val argCount = code[index++]

          buffer += "BuildClosure -> $argCount"
        }
        Bytecode.Jump -> {
          val offset = code[index++]

          buffer += "Jump -> $offset"
        }
        Bytecode.Branch -> {
          val offset = code[index++]

          buffer += "Branch -> $offset"
        }
      }
    }

    return buffer.toString()
  }
}

inline operator fun StringBuilder.plusAssign(str: String) {
  appendLine(str)
}

typealias NativeFunction = FunctionValue

class ShellFunction(val name: String)
class ClosureFunction(val scope: Scope, val closure: Array<Any?>, val code: BytecodeFunction)


enum class Bytecode {
  NoOp,
  Pop, // (any)
  Dup, // (any)
  Swap, // (any, any)
  Increment, // (int)
  Decrement, // (int)
  Define, // (any) name const index
  Store, // (any) index
  LoadLocal, // () local index
  LoadScope, // () index of string const
  LoadRecurse, //
  LoadArgArray, //
  LoadNull, //
  LoadTrue, //
  LoadFalse, //
  LoadInt, // () value
  LoadDouble, // () value
  LoadConst, // () const index
  Call, // (function, ..any) num of args
  CallDynamic, // (function, array of args)
  Return, // (any)
  BuildShell, // () shell const index
  BuildClosure, // () number of closure params
  Jump, // jump to index
  Branch, // (boolean) jump if FALSE! True will fall through
}


