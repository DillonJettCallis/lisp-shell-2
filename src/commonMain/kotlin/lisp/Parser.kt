package lisp
class ParseException(message: String, cause: Throwable? = null): RuntimeException(message, cause)
class LexException(message: String, cause: Throwable? = null): RuntimeException(message, cause)

class Parser(private val raw: List<Token>) {

  companion object {
    private val brackets = mapOf("(" to ")", "[" to "]", "{" to "}")

    fun parse(tokens: List<Token>): List<Expression> = Parser(tokens).parseBody()
  }

  private var index = 0
  private val maxIndex = raw.size

  private fun parseBody(): List<Expression> {
    val body = ArrayList<Expression>()

    while (index < maxIndex) {
      body += parseExpression()
      index++
    }

    return body
  }

  private fun parseExpression(): Expression {
    val next = raw[index]

    return when(next.kind) {
      TokenKind.bracket -> {
        if (next.value in brackets) {
          parseCall(next)
        } else {
          next.fail("Unexpected close")
        }
      }
      TokenKind.variable -> VariableEx(next.value, next.pos)
      TokenKind.operator -> OperatorEx(next.value, next.pos)
      TokenKind.literal -> {
        when (next.value) {
          "null" -> NullLiteralEx(next.pos)
          "true" -> BooleanLiteralEx(true, next.pos)
          "false" -> BooleanLiteralEx(false, next.pos)
          else -> next.fail("Unexpected literal")
        }
      }
      TokenKind.command -> CommandEx(next.value, next.pos)
      TokenKind.number -> {
        if (next.value.contains('.')) {
          DoubleLiteralEx(next.value.toDoubleOrNull() ?: next.fail("Failed to parse decimal literal"), next.pos)
        } else {
          IntLiteralEx(next.value.toIntOrNull() ?: next.fail("Failed to parse integer literal"), next.pos)
        }
      }
      TokenKind.quotedString -> StringLiteralEx(next.value, true, next.pos)
      TokenKind.rawString -> StringLiteralEx(next.value, false, next.pos)
    }
  }

  private fun parseCall(open: Token): Expression {
    val close = brackets.getValue(open.value)
    val body = ArrayList<Expression>()
    index++

    while (index < maxIndex) {
      val next = raw[index]

      if (next.value == close) {
        return when (close) {
          ")" -> CallEx(body, open.pos)
          "]" -> ArrayEx(body, open.pos)
          "}" -> {
            if (body.size.rem(2) != 0) {
              open.fail("Map literal has an odd number of terms")
            }

            MapEx(body.windowed(2, 2) { (l, r) -> l to r }, open.pos)
          }
          else -> next.fail("Unexpected bracket")
        }
      } else {
        body += parseExpression()
        index++
      }
    }

    open.fail("Unclosed bracket")
  }

}
