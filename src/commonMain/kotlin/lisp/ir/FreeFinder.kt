package lisp.ir

import lisp.ParamMeta

class FreeFinder private constructor(private val freeable: HashSet<String>): IrVisitor {

  companion object {
    fun addFrees(all: MutableList<Ir>, params: List<ParamMeta>) {
      // go backwards, the first time you find a load,
      // it's actually the last time it's loaded.
      // So we know it's never needed again
      val freeable = HashSet(all.stores())
      freeable += params.map { it.name }
      FreeFinder(freeable).visitAll(all)
    }

    private fun List<Ir>.stores(): HashSet<String> {
      return filterIsInstance<StoreIr>().mapTo(HashSet()) { it.name }
    }
  }

  override val reversed: Boolean
    get() = true

  override fun visit(ir: LoadFuncIr, access: IrVisitorAccess) {
    addFrees(ir.func.body, ir.func.params)
  }

  override fun visit(ir: LoopIr, access: IrVisitorAccess) {
    // first free anything declared inside of these blocks (stored and not already declared)
    FreeFinder(ir.condition.stores().also { it -= freeable }).visitAll(ir.condition)
    FreeFinder(ir.body.stores().also { it -= freeable }).visitAll(ir.body)

    // nothing can be freed inside a loop, they're used more than once, but they can be freed after the loop
    val loads = (LoadFinder.findLoads(ir.condition) + LoadFinder.findLoads(ir.body)).intersect(freeable)

    if (loads.isNotEmpty()) {
      freeable -= loads
      val frees = loads.mapTo(ArrayList<Ir>()) { FreeIr(it, ir.pos) }
      // remember that we're reversed, so put the loop below the frees
      frees.add(ir)

      access.replace(frees)
    }
  }

  override fun visit(ir: BranchIr, access: IrVisitorAccess) {
    val thenFreeable = HashSet(freeable)
    val elseFreeable = HashSet(freeable)

    // first free anything declared inside of these blocks
    FreeFinder(ir.thenEx.stores().also { it -= freeable }).visitAll(ir.thenEx)
    FreeFinder(ir.elseEx.stores().also { it -= freeable }).visitAll(ir.elseEx)

    // now free everything declared before
    FreeFinder(thenFreeable).visitAll(ir.thenEx)
    FreeFinder(elseFreeable).visitAll(ir.elseEx)

    val freedByThen = freeable - thenFreeable
    val freedByElse = freeable - elseFreeable

    freedByThen.forEach {
      if (it !in freedByElse) {
        // if a var was freed in the then block but not in else, then free it in else also

        ir.elseEx.add(0, FreeIr(it, ir.pos))
      }
    }

    freedByElse.forEach {
      if (it !in freedByThen) {
        // if a var was freed in the else block but not in then, then free it in then also

        ir.thenEx.add(0, FreeIr(it, ir.pos))
      }
    }

    freeable -= freedByThen
    freeable -= freedByElse
  }

  override fun visit(ir: LoadIr, access: IrVisitorAccess) {
    // if this var can be freed
    if (ir.name in freeable) {
      // free it (remember we are reversed so it's backwards)
      access.replace(listOf(FreeIr(ir.name, ir.pos), ir))

      // remove so it can't be freed again
      freeable -= ir.name
    }
  }

}
