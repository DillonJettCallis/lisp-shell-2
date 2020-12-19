package lisp.transform

import lisp.CallEx
import lisp.Expression

class AutoWrapTransformer : Transformer {
  override fun transform(src: List<Expression>): List<Expression> {
    if (src.isEmpty()) {
      return emptyList()
    }

    val head = src.first()

    return if (src.size == 1 && head is CallEx) {
      // everything is already wrapped in one call expression
      src
    } else {
      // auto wrap multiple things into one call - used for shell
      listOf(CallEx(src, head.pos))
    }
  }
}
