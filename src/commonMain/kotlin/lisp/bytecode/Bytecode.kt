package lisp.bytecode

import lisp.FunctionValue
import lisp.ParamMeta
import lisp.Position
import lisp.Scope

class BytecodeFunction(
  val params: List<ParamMeta>,
  val code: IntArray,
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
}

typealias NativeFunction = FunctionValue

data class ShellFunction(val name: String)
data class ClosureFunction(val scope: Scope, val code: BytecodeFunction)


enum class Bytecode {
  NoOp,
  Pop, // (any)
  Dup, // (any)
  Swap, // (any, any)
  Increment, // (int)
  Decrement, // (int)
  Define, // (any) name const index
  Store, // (any) index
  Load, // () index
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
  BuildClosure, // () function const index
  Jump, // jump to index
  Branch, // (boolean) jump if FALSE! True will fall through
}


