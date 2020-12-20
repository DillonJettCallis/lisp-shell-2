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

    fun compileBlock(block: List<Ir>, scope: CompilerScope): IntArray {
      val buffer = IntArrayBuffer()

      block.forEach { ir ->
        posList += ir.pos

        when (ir) {
          is PopIr -> {
            buffer += Bytecode.Pop
            scope.pop()
          }
          is DupIr -> {
            buffer += Bytecode.Dup
            scope.push()
          }
          is SwapIr -> buffer += Bytecode.Swap
          is IncrementIr -> buffer += Bytecode.Increment
          is DecrementIr -> buffer += Bytecode.Decrement
          is DefineIr -> {
            buffer += Bytecode.Define
            buffer.push(stringConstantMap.indexedAdd(ir.name))
            scope.pop()

            posList += ir.pos
          }
          is StoreIr -> {
            buffer += Bytecode.Store
            buffer.push(scope.store(ir.name))
            scope.pop()

            posList += ir.pos
          }
          is LoadIr -> {
            if (ir.name in func.closureContext.globals) {
              buffer += Bytecode.LoadScope
              buffer.push(stringConstantMap.indexedAdd(ir.name))
            } else {
              buffer += Bytecode.LoadLocal
              buffer.push(scope.load(ir.name))
            }

            scope.push()
            posList += ir.pos
          }
          is LoadRecurseIr -> {
            buffer += Bytecode.LoadRecurse
            scope.push()
          }
          is LoadArgArrayIr -> {
            buffer += Bytecode.LoadArgArray
            scope.push()
          }
          is LoadConstIr -> {
            scope.push()

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
                scope.push()
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
          is LoadFuncIr -> {
            val builtFunction = compile(ir.func)

            buffer += Bytecode.LoadConst
            buffer.push(constantMap.indexedAdd(builtFunction))
            scope.push()
            posList += ir.pos
          }
          is FreeIr -> {
            scope.free(ir.name)
            posList.removeLast()
          }
          is CallIr -> {
            buffer += Bytecode.Call
            buffer.push(ir.paramCount)
            scope.call(ir.paramCount)

            posList += ir.pos
          }
          is CallDynamicIr -> {
            buffer += Bytecode.CallDynamic
            scope.call(1)
          }
          is ReturnIr -> buffer += Bytecode.Return
          is BuildShellIr -> {
            buffer += Bytecode.BuildShell
            buffer.push(stringConstantMap.indexedAdd(ir.path))
            scope.pop()

            posList += ir.pos
          }
          is BuildClosureIr -> {
            buffer += Bytecode.BuildClosure
            buffer.push(ir.paramCount)
            scope.call(ir.paramCount)
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

            scope.child {
              buffer.pushAll(compileBlock(ir.thenEx, it))
            }

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

              scope.child {
                buffer.pushAll(compileBlock(ir.elseEx, it))
              }

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

            scope.child {
              buffer.pushAll(compileBlock(ir.condition, it))
            }

            // branch after condition
            buffer += Bytecode.Branch
            posList += ir.pos

            val indexOfConditionJump = buffer.size
            buffer.push(0) // placeholder for the condition jump
            posList += ir.pos

            scope.child {
              buffer.pushAll(compileBlock(ir.body, it))
            }

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

    val scope = CompilerScope.init(func)

    val code = compileBlock(func.body, scope)


    return BytecodeFunction(
      params = func.params,
      code = code,
      maxLocals = scope.maxLocals,
      maxStack = scope.maxStack,
      ir = func,
      posArray = posList.toTypedArray(),
      constants = constantMap.indexedValue(),
      stringConstants = stringConstantMap.indexedValue()
    )
  }
}

class CompilerScope private constructor(
  private val freeLocals: MutableSet<Int>,
  private val locals: MutableMap<String, Int>,
  private var localCount: Int,
  private var maxStackSize: Int,
  private var stackSize: Int
){

  companion object {
    fun init(func: IrFunction): CompilerScope {
      val scope = CompilerScope(HashSet(), HashMap(), 0, 0, 0)

      func.params.forEach {
        scope.store(it.name)
      }

      func.closureContext.closures.forEach {
        scope.store(it)
      }

      return scope
    }
  }

  val maxLocals: Int
    get() = localCount

  val maxStack: Int
    get() = maxStackSize

  fun store(name: String): Int {
    // if this var is known, use the same slot
    if (name in locals) {
      return locals[name]!!
    }

    // are there any free slots?
    val freeSlot = freeLocals.minOrNull()

    return if (freeSlot != null) {
      // if there is a free slot, take it up and use it
      freeLocals.remove(freeSlot)
      locals[name] = freeSlot
      freeSlot
    } else {
      // if there is no free slot, make a new slot and use that
      localCount++
      locals.indexedAdd(name)
    }
  }

  fun load(name: String): Int {
    return locals[name]!!
  }

  fun free(name: String) {
    freeLocals += locals.remove(name)!!
  }

  fun push() {
    stackSize++

    if (stackSize > maxStackSize) {
      maxStackSize = stackSize
    }
  }

  fun pop() {
    stackSize--
  }

  fun call(argCount: Int) {
    stackSize -= argCount
  }

  fun contains(name: String): Boolean {
    return name in locals
  }

  fun child(with: (CompilerScope) -> Unit) {
    val child = CompilerScope(
      freeLocals = HashSet(freeLocals),
      locals = HashMap(locals),
      localCount = localCount,
      maxStackSize = maxStackSize,
      stackSize = stackSize
    )

    with(child)

    localCount = child.localCount
    maxStackSize = child.maxStackSize
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
