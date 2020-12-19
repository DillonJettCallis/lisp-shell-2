package lisp.ir

class LoadFinder private constructor(): IrVisitor{

  companion object {
    fun findLoads(all: MutableList<Ir>): Set<String> {
      val finder = LoadFinder()
      finder.visitAll(all)
      return finder.loads
    }
  }

  val loads = HashSet<String>()

  override fun visit(ir: LoadIr, access: IrVisitorAccess) {
    loads += ir.name
  }
}
