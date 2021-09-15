package lisp

enum class TokenKind {
  quotedString,
  rawString,
  bracket,
  command,
  number,
  literal,
  variable
}

data class Token(val value: String, val kind: TokenKind, val pos: Position) {
  fun fail(message: String): Nothing = pos.parseFail("$message '$value'")
}

class Lexer(private val raw: String, private val src: String) {

  companion object {
    private val whitespace = " ,\t\r\n".toSet()
    private val quoteTypes = "'\"`".toSet()
    private val numberRegex = "-?\\d\\.?\\d*".toRegex()
    private val brackets = "()[]{}".toSet()
    private val literals = setOf("null", "true", "false")
    private val stringEscapeMap = mapOf(
      '\'' to '\'',
      '"' to '"',
      '`' to '`',
      't' to "\t",
      'n' to '\n',
      'r' to '\r',
      '\\' to '\\',
      '$' to '$'
    )
    private val nonWords = whitespace + quoteTypes + brackets

    fun lex(raw: String, src: String): List<Token> {
      return Lexer(raw, src).doLex()
    }
  }

  private var line = 1
  private var col = 1
  private var index = 0
  private val maxIndex = raw.length
  private val tokens = ArrayList<Token>()

  private fun doLex(): List<Token> {
    skipWhitespace()

    while(index < maxIndex) {
      tokens += lexNext(pos())

      skipWhitespace()
    }

    return tokens
  }

  private fun lexNext(pos: Position): List<Token> {
    return when (val next = raw[index]) {
      in brackets -> {
        advance()
        listOf(Token(value = next.toString(), kind = TokenKind.bracket, pos = pos))
      }
      in quoteTypes -> {
        parseString(next, pos)
      }
      '$' -> {
        advance()
        listOf(parseWord(pos).copy(kind = TokenKind.variable))
      }
      '@' -> {
        advance()

        if (index < maxIndex) {
          val path = if (raw[index] in quoteTypes) {
            parseString(raw[index], pos)
          } else {
            listOf(parseWord(pos))
          }

          // transform "@test" into (\ exec $0 test)
          // transform "@`an interpolated $path`" into "(\ exec $0 (& `an interpolated` $path))"

          return listOf(
            listOf(
              Token(value = "(", kind =  TokenKind.bracket, pos = pos),
              Token(value = "\\", kind = TokenKind.variable, pos = pos),
              Token(value = "exec", kind = TokenKind.variable, pos = pos),
            ),
            path,
            listOf(
              Token(value = "0", kind = TokenKind.variable, pos = pos),
              Token(value = ")", kind = TokenKind.bracket, pos = pos),
            ),
          ).flatten()
        } else {
          pos.lexFail("Dangling @")
        }
      }
      else -> listOf(parseWord(pos))
    }
  }

  private fun String.kind(): TokenKind {
    return when {
      this in literals -> TokenKind.literal
      matches(numberRegex) -> TokenKind.number
      else -> TokenKind.rawString
    }
  }

  private fun parseWord(pos: Position, insideString: Boolean = false): Token {
    val buff = StringBuilder()

    while (index < maxIndex) {
      when (val next = raw[index]) {
        in whitespace, in brackets -> break
        in quoteTypes -> if (insideString) break else pos().lexFail("Unexpected string begin")
        else -> {
          buff.append(next)
          advance()
        }
      }
    }

    val value = buff.toString()
    return Token(value, value.kind(), pos)
  }

  private fun parseString(quoteType: Char, pos: Position): List<Token> {
    val tokens = ArrayList<Token>()
    val buff = StringBuilder()
    var hasInterpolation = false
    var workingPos = pos
    advance()

    loop@while (index < maxIndex) {
      when (val next = raw[index]) {
        '\\' -> {
          advance()

          if (index < maxIndex) {
            val escaped = raw[index]

            if (stringEscapeMap.containsKey(escaped)) {
              buff.append(stringEscapeMap[escaped])
              advance()
            } else {
              pos().lexFail("Invalid escape char")
            }
          } else {
            pos.lexFail("Unclosed string starting")
          }
        }
        quoteType -> {
          advance()
          break@loop
        }
        '$' -> {
          if (!hasInterpolation) {
            hasInterpolation = true
            tokens += Token(value = "(", kind = TokenKind.bracket, pos = pos)
            tokens += Token(value = ".", kind = TokenKind.variable, pos = pos)
          }

          if (buff.isNotEmpty()) {
            tokens += Token(value = buff.toString(), kind = TokenKind.quotedString, pos = workingPos)
            buff.clear()
          }

          workingPos = pos()
          advance()

          if (index >= maxIndex) {
            pos.lexFail("Unclosed string starting")
          }

          when (raw[index]) {
            !in nonWords -> tokens += parseWord(workingPos, true).copy(kind = TokenKind.variable)
            '(' -> {
              var parenCount = 0

              while(index < maxIndex) {
                val startCount = tokens.size
                val nextTokens = lexNext(pos())
                tokens += nextTokens

                // TODO: this is an ugly little work around.
                // If you have "$(thenInside "$(this will cause the outer to blowup without this check)" )"
                // This is because "a $(long) test" becomes (& "a " (long) "test") and the close paren
                // will be returned in lexNext(). But if we check the size of token before and after and
                // if token size increased by more than one we know that could not have happened and so
                // can't check for the end. It works but is very ugly and should be replaced with a better idea
                if (tokens.size - startCount == 1) {
                  when (nextTokens.last().value) {
                    "(" -> parenCount++
                    ")" -> {
                      parenCount--
                      if (parenCount == 0) {
                        continue@loop
                      }
                    }
                  }
                }

                skipWhitespace()
              }

              workingPos.lexFail("Unclosed string interpolation starting")
            }
            else -> workingPos.lexFail("Unescaped \$ is not followed by identifier or interpolation")
          }
        }
        else -> {
          buff.append(next)
          advance()
        }
      }
    }

    tokens +=  Token(value = buff.toString(), kind = TokenKind.quotedString, pos = workingPos)

    if (hasInterpolation) {
      tokens += Token(value = ")", kind = TokenKind.bracket, pos = pos())
    }

    return tokens
  }

  private fun pos(): Position = Position(line = line, col = col, src = src)

  private fun advance() {
    val curr = raw[index]
    index++
    if (curr == '\n') {
      line++
      col = 1
    } else {
      col++
    }
  }

  private fun skipWhitespace() {
    while (index < maxIndex) {
      val next = raw[index]
      if (next in whitespace) {
        advance()
      } else {
        return
      }
    }
  }
}
