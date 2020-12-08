package lisp.transform

import lisp.Expression

interface Transformer {
  fun transform(src: List<Expression>): List<Expression>
}
