package lisp.ir

class UnusedLocalRemove : IrVisitor {

  companion object {
    fun removeUnusedLocals(body: MutableList<Ir>) {
      UnusedLocalRemove().visitAll(body)
    }
  }

  override fun visit(ir: StoreIr, access: IrVisitorAccess) {
    val following = access.peekAt(2)

    if (following.isNotEmpty()) {
      val maybeLoad = following[0]

      if (maybeLoad is LoadIr && maybeLoad.name == ir.name) {
        if (following.size == 2) {
          val maybeFree = following[1]

          if (maybeFree is FreeIr) {
            // if we free right after load and store, that means we never use the value, so just leave the value on stack
            access.replace(listOf(), numToReplace = 3)
            return
          }
        }

        access.replace(listOf(
          // replace [store, load] with [dup, store]
          DupIr(ir.pos),
          ir
        ), numToReplace = 2)
      }
    }
  }

  override fun visit(ir: LoadFuncIr, access: IrVisitorAccess) {
    removeUnusedLocals(ir.func.body)
  }

}
