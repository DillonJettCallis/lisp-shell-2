package lisp.compiler

import lisp.Position
import lisp.bytecode.Bytecode
import lisp.bytecode.BytecodeFunction
import lisp.ir.*

class Compiler {

  fun compile(func: IrFunction): BytecodeFunction {
    val posList = ArrayList<Position>(20)
    val constantMap = HashMap<Any, Int>()
    val stringConstantMap = HashMap<String, Int>()


    fun compileBlock(block: List<Ir>): IntArray {
      val buffer = IntArrayBuffer()

      block.forEach { ir ->
        posList += ir.pos

        when (ir) {
          is PopIr -> buffer += Bytecode.Pop
          is DupIr -> buffer += Bytecode.Dup
          is SwapIr -> buffer += Bytecode.Swap
          is IncrementIr -> buffer += Bytecode.Increment
          is DecrementIr -> buffer += Bytecode.Decrement
          is DefineIr -> {
            buffer += Bytecode.Define
            buffer.push(stringConstantMap.indexedAdd(ir.name))

            posList += ir.pos
          }
          is StoreIr -> {
            buffer += Bytecode.Store
            buffer.push(stringConstantMap.indexedAdd(ir.name))

            posList += ir.pos
          }
          is LoadIr -> {
            buffer += Bytecode.Load
            buffer.push(stringConstantMap.indexedAdd(ir.name))

            posList += ir.pos
          }
          is LoadConstIr -> {
            when (ir.value) {
              null -> buffer += Bytecode.LoadNull
              true -> buffer += Bytecode.LoadTrue
              false -> buffer += Bytecode.LoadFalse
              is Int -> {
                buffer += Bytecode.LoadInt
                buffer.push(ir.value)

                posList += ir.pos
              }
              is Double -> {
                buffer += Bytecode.LoadDouble
                val longValue = ir.value.toBits()
                val highBits = (longValue.shr(32)).toInt()
                val lowBits = longValue.toInt()

                buffer.push(highBits)
                buffer.push(lowBits)

                posList += ir.pos
                posList += ir.pos
              }
              else -> {
                buffer += Bytecode.LoadConst
                buffer.push(constantMap.indexedAdd(ir.value))

                posList += ir.pos
              }
            }
          }
          is CallIr -> {
            buffer += Bytecode.Call
            buffer.push(ir.paramCount)

            posList += ir.pos
          }
          is CallDynamicIr -> buffer += Bytecode.CallDynamic
          is ReturnIr -> buffer += Bytecode.Return
          is BuildShellIr -> {
            buffer += Bytecode.BuildShell
            buffer.push(stringConstantMap.indexedAdd(ir.path))

            posList += ir.pos
          }
          is BuildClosureIr -> {
            val builtFunction = compile(ir.func)

            buffer += Bytecode.BuildClosure
            buffer.push(constantMap.indexedAdd(builtFunction))

            posList += ir.pos
          }
          is BranchIr -> {
            buffer += Bytecode.Branch

            // branch
            // index if FALSE
            // then block
            // jump to end
            // else block
            // end if

            val indexOfElseJump = buffer.size
            buffer.push(0) // this is a place holder. We'll replace the value later
            posList += ir.pos

            buffer.pushAll(compileBlock(ir.thenEx))

            if (ir.elseEx.isEmpty()) {
              // there is no else block. In that case it's simpler. Just jump here
              buffer[indexOfElseJump] = buffer.size - indexOfElseJump

              // branch
              // index if FALSE
              // then block
              // end if
            } else {
              // then block needs to jump over the else block
              buffer += Bytecode.Jump
              posList += ir.pos

              val indexOfThenJump = buffer.size
              buffer.push(0) // placeholder for the then jump
              posList += ir.pos

              // else block beginning, so jump here
              buffer[indexOfElseJump] = buffer.size - indexOfElseJump

              buffer.pushAll(compileBlock(ir.elseEx))

              // patch the then block with a jump to the end
              buffer[indexOfThenJump] = buffer.size - indexOfThenJump
            }
          }
          is LoopIr -> {
            // this is the only case where the first instruction is NOT based on what we just looked at
            posList.removeLast()

            // condition start
            // branch
            // jump to end of loop if FALSE
            // body
            // jump to condition start
            // end of loop

            val indexOfConditionStart = buffer.size

            buffer.pushAll(compileBlock(ir.condition))

            // branch after condition
            buffer += Bytecode.Branch
            posList += ir.pos

            val indexOfConditionJump = buffer.size
            buffer.push(0) // placeholder for the condition jump
            posList += ir.pos

            buffer.pushAll(compileBlock(ir.body))

            buffer += Bytecode.Jump
            posList += ir.pos
            buffer.push(indexOfConditionStart - buffer.size) // should be a negative number to jump backwards
            posList += ir.pos

            buffer[indexOfConditionJump] = buffer.size - indexOfConditionJump // jump to here when condition is false
          }
        }
      }

      return buffer.build()
    }

    val code = compileBlock(func.body)


    return BytecodeFunction(
      params = func.params,
      code = code,
      posArray = posList.toTypedArray(),
      constants = constantMap.indexedValue(),
      stringConstants = stringConstantMap.indexedValue()
    )
  }
}

operator fun IntArrayBuffer.plusAssign(code: Bytecode) {
  push(code.ordinal)
}

fun <Key> MutableMap<Key, Int>.indexedAdd(value: Key): Int {
  return this[value] ?: run {
    val index = size
    this[value] = index
    index
  }
}

inline fun <reified Key> Map<Key, Int>.indexedValue(): Array<Key> {
  val inversed = entries.associate { (key, value) -> value to key }

  return Array(size) { inversed.getValue(it) }
}
