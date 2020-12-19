package lisp.ir

import lisp.ParamMeta


class ClosureChecker private constructor(
  private val context: ClosureContextBuilder
): IrVisitor {

  companion object {
    fun check(body: MutableList<Ir>, params: List<ParamMeta>): ClosureContext {
      val checker = ClosureChecker(ClosureContextBuilder(params))
      checker.visitAll(body)
      return checker.context.build()
    }
  }

  override fun visit(ir: DefineIr, access: IrVisitorAccess) {
    context.define(ir.name)
  }

  override fun visit(ir: StoreIr, access: IrVisitorAccess) {
    context.store(ir.name)
  }

  override fun visit(ir: LoadIr, access: IrVisitorAccess) {
    context.load(ir.name)
  }

  override fun visit(ir: LoadFuncIr, access: IrVisitorAccess) {
    val checkedFunction = run {
      val innerBody = ArrayList(ir.func.body)
      val innerChecker = ClosureChecker(context.child(ir.func.params))
      innerChecker.visitAll(innerBody)
      val innerContext = innerChecker.context.build()

      ir.func.copy(
        body = innerBody,
        closureContext = innerContext
      )
    }

    val closures = checkedFunction.closureContext.closures

    // even with no closed over values, scope is always closed over

    // put the function on the stack
    val init = ir.copy(func = checkedFunction)

    // load up the values we're going to pass down
    val middle = closures.map {
      context.load(it) // make sure to mark that we're closing here
      LoadIr(it, ir.pos)
    }

    // build a closure with x closed over values (may be 0)
    val build = BuildClosureIr(closures.size, ir.pos)

    access.replace(listOf(init) + middle + build)
  }
}

val digitRegex = "[\\d]".toRegex()

data class ClosureContext(
  val locals: Set<String>,
  val globals: Set<String>,
  val closures: Set<String>
) {
  companion object {
    val empty = ClosureContext(emptySet(), emptySet(), emptySet())
  }
}

class ClosureContextBuilder(params: List<ParamMeta>, private val parent: ClosureContextBuilder? = null) {

  private val locals = HashSet<String>()
  private val globals = HashSet<String>()
  private val closures = HashSet<String>()

  init {
    params.forEach { store(it.name) }
  }

  fun define(name: String) {
    // if you define a value it's now at a global level
    // even if you only use it locally we have to treat it like a global
    globals += name
  }

  fun store(name: String) {
    locals += name
  }

  fun load(name: String) {
    // if we can find a local, it's not a closure

    if (name !in locals) {
      // if we can't find it in local, then can we find it in parent

      if (checkClosure(name)) {
        // then it's a closure from above
        // treat it like it's also a local since it will be in local scope
        closures += name
        locals += name
      } else {
        // otherwise it must be a global or module var
        globals += name
      }
    }
  }

  fun build(): ClosureContext = ClosureContext(locals, globals, closures)

  private fun checkClosure(name: String): Boolean {
    // no parent means it must be global
    if (parent == null) {
      return false
    }

    // if the name is known to be global, then don't bother looking further up; we know we won't find it
    if (name in parent.globals) {
      return false
    }

    // if the name is a local, then it's a closure
    if (name in parent.locals) {
      return true
    }

    // have the parent check
    return parent.checkClosure(name)
  }

  fun child(params: List<ParamMeta>) = ClosureContextBuilder(params, this)
}
