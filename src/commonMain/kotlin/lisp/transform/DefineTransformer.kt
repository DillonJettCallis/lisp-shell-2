package lisp.transform

import lisp.*

class DefineTransformer : Transformer {

  override fun transform(src: List<Expression>): List<Expression> {
    return src.map(::transform)
  }

  private fun transform(ex: Expression): Expression {
    return when (ex) {
      is CallEx -> {
        val head = ex.body.firstOrNull()

        val kind = when {
          head is VariableEx -> head.name
          head is StringLiteralEx && !head.quoted -> head.value
          head is OperatorEx -> head.op
          else -> return ex.copy(body = ex.body.map(::transform))
        }

        when (kind) {
          "cd" -> {
            // (cd a/path) => (def $cwd a/path)

            if (ex.body.size != 2) {
              ex.pos.compileFail("Expected exactly one argument to function 'cd'")
            }

            val path = ex.body[1]

            CallEx(
              body = listOf(
                VariableEx("def", head.pos),
                StringLiteralEx("cwd", true, head.pos),
                transform(path)
              ),
              pos = head.pos
            )
          }
          "defn" -> {
            // (defn $name [$args] (body)) => (def $name (fn "name" [$args] (body)))

            if (ex.body.size != 4) {
              ex.pos.compileFail("Invalid args to defn. Expected (defn \$name [\$args] (body))")
            }

            val (_, nameEx, argArray, body) = ex.body

            if (nameEx !is VariableEx) {
              nameEx.pos.compileFail("Expected first argument to 'defn' to be a variable")
            }

            CallEx(
              body = listOf(
                VariableEx("def", head.pos),
                nameEx,
                CallEx(
                  body = listOf(
                    VariableEx("fn", head.pos),
                    StringLiteralEx(nameEx.name, true, nameEx.pos),
                    argArray,
                    transform(body)
                  ),
                  pos = ex.pos
                )
              ),
              pos = ex.pos
            )
          }
          "\\" -> {
            // (\ body) => (fn [] (body))

            CallEx(
              body = listOf(
                VariableEx("fn", head.pos),
                ArrayEx(emptyList(), head.pos),
                CallEx(ex.body.drop(1).map(::transform), head.pos)
              ),
              pos = ex.pos
            )
          }
          else -> ex.copy(body = ex.body.map(::transform))
        }

      }
      is ArrayEx -> ex.copy(body = ex.body.map(::transform))
      is MapEx -> ex.copy(body = ex.body.map { (key, value) -> transform(key) to transform(value) })
      else -> ex

    }
  }

}
