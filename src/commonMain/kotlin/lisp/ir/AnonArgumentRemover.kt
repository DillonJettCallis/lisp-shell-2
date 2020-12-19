package lisp.ir

import lisp.ParamMeta

class AnonArgumentRemover private constructor(val params: MutableList<ParamMeta>): IrVisitor {

  companion object {
    fun resolve(body: MutableList<Ir>, params: MutableList<ParamMeta>) {
      val resolver = AnonArgumentRemover(params)
      resolver.visitAll(body)
    }
  }

  override fun visit(ir: LoadFuncIr, access: IrVisitorAccess) {
    resolve(ir.func.body, ir.func.params)
  }

  override fun visit(ir: LoadIr, access: IrVisitorAccess) {
    if (ir.name == "recurse") {
      access.replace(LoadRecurseIr(ir.pos))
      return
    }

    if (ir.name == "0") {
      access.replace(LoadArgArrayIr(ir.pos))
      return
    }

    if (ir.name.matches(digitRegex)) {
      val index = ir.name.toInt() - 1

      if (index >= params.size) {
        // create params up until this one
        (params.size .. index).forEach {
          params.add(ParamMeta("${it + 1}"))
        }
      }
    }
  }

}
