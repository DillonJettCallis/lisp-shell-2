package lisp.ir

import kotlin.math.min

interface IrVisitorAccess {
  fun replace(ir: Ir, numToReplace: Int = 1)
  fun replace(all: List<Ir>, numToReplace: Int = 1)
  fun peekAt(numToPeek: Int): List<Ir>
}

interface IrVisitor {

  val reversed: Boolean
    get() = false

  fun visit(ir: StoreIr, access: IrVisitorAccess) {}
  fun visit(ir: PopIr, access: IrVisitorAccess) {}
  fun visit(ir: DupIr, access: IrVisitorAccess) {}
  fun visit(ir: SwapIr, access: IrVisitorAccess) {}
  fun visit(ir: IncrementIr, access: IrVisitorAccess) {}
  fun visit(ir: DecrementIr, access: IrVisitorAccess) {}
  fun visit(ir: DefineIr, access: IrVisitorAccess) {}
  fun visit(ir: LoadIr, access: IrVisitorAccess) {}
  fun visit(ir: LoadConstIr, access: IrVisitorAccess) {}
  fun visit(ir: LoadRecurseIr, access: IrVisitorAccess) {}
  fun visit(ir: LoadArgArrayIr, access: IrVisitorAccess) {}
  fun visit(ir: FreeIr, access: IrVisitorAccess) {}
  fun visit(ir: CallIr, access: IrVisitorAccess) {}
  fun visit(ir: CallDynamicIr, access: IrVisitorAccess) {}
  fun visit(ir: ReturnIr, access: IrVisitorAccess) {}
  fun visit(ir: ReturnVoidIr, access: IrVisitorAccess) {}
  fun visit(ir: BuildShellIr, access: IrVisitorAccess) {}
  fun visit(ir: BuildClosureIr, access: IrVisitorAccess) {}
  fun visit(ir: BuildModuleIr, access: IrVisitorAccess) {}
  fun visit(ir: LoadFuncIr, access: IrVisitorAccess) {}
  fun visit(ir: BranchIr, access: IrVisitorAccess) {
    visitAll(ir.thenEx)
    visitAll(ir.elseEx)
  }

  fun visit(ir: LoopIr, access: IrVisitorAccess) {
    visitAll(ir.condition)
    visitAll(ir.body)
  }

  fun visitAll(block: MutableList<Ir>) {
    if (reversed) {
      block.reverse()
    }

    val iter = block.listIterator()

    val access = object: IrVisitorAccess {
      override fun replace(ir: Ir, numToReplace: Int) {
        (0 until (numToReplace - 1)).forEach {
          iter.remove()
          iter.next()
        }

        iter.remove()

        iter.add(ir)
      }

      override fun replace(all: List<Ir>, numToReplace: Int) {
        (0 until (numToReplace - 1)).forEach {
          iter.remove()
          iter.next()
        }

        iter.remove()

        all.forEach {
          iter.add(it)
        }
      }

      override fun peekAt(numToPeek: Int): List<Ir> {
        val index = iter.nextIndex()

        return block.subList(index, min(index + numToPeek, block.size))
      }
    }

    while(iter.hasNext()) {
      val item = iter.next()

      visit(item, access)
    }

    if (reversed) {
      block.reverse()
    }
  }

  fun visit(ir: Ir, access: IrVisitorAccess) {
    when(ir) {
      is StoreIr -> visit(ir, access)
      is PopIr -> visit(ir, access)
      is DupIr -> visit(ir, access)
      is SwapIr -> visit(ir, access)
      is IncrementIr -> visit(ir, access)
      is DecrementIr -> visit(ir, access)
      is DefineIr -> visit(ir, access)
      is LoadIr -> visit(ir, access)
      is LoadConstIr -> visit(ir, access)
      is LoadFuncIr -> visit(ir, access)
      is LoadRecurseIr -> visit(ir, access)
      is LoadArgArrayIr -> visit(ir, access)
      is FreeIr -> visit(ir, access)
      is CallIr -> visit(ir, access)
      is CallDynamicIr -> visit(ir, access)
      is ReturnIr -> visit(ir, access)
      is ReturnVoidIr -> visit(ir, access)
      is BuildShellIr -> visit(ir, access)
      is BuildClosureIr -> visit(ir, access)
      is BuildModuleIr -> visit(ir, access)
      is BranchIr -> visit(ir, access)
      is LoopIr -> visit(ir, access)
      else -> throw IllegalStateException("We forgot an Ir type!")
    }
  }

}
