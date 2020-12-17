package lisp.lib

import lisp.FunctionValue
import lisp.ParamMeta
import lisp.Position
import lisp.Scope
import lisp.coercion.coerceTo

object ParseLibrary: Library {
  override fun addLib(global: Scope) {
    global["parse/lines"] = object: FunctionValue {
      override val name: String = "parse/lines"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("raw", String::class, "string to be split into lines")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 1) {
          pos.interpretFail("Expected only one argument to 'parse/lines'")
        }

        val raw = args.first()?.coerceTo(String::class) ?: pos.interpretFail("Expected string argument to 'parse/lines'")

        return raw.splitToSequence("\n")
          .map { it.trim() }
          .filter { it.isNotEmpty() }
          .toList()
      }
    }

    global["parse/json"] = object: FunctionValue {
      override val name: String = "parse/json"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("raw", String::class, "string containing raw json")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 1) {
          pos.interpretFail("Expected only one argument to 'parse/json'")
        }

        val raw = args.first()?.coerceTo(String::class) ?: pos.interpretFail("Expected string argument to 'parse/json'")

        return JsonParser(raw).parseWhole()
      }
    }
  }
}

private class JsonParser(private val raw: String) {

  private var index = 0
  private var line = 1
  private var col = 1
  private val maxIndex = raw.length

  companion object {
    private val whitespace = " \t\r\n".toSet()
    private val startDigit = "0123456789".toSet()
    private val digits = startDigit + '.'
    private val keywordChars = "truefalsenull".toSet()
    private val escapeMap = mapOf(
      'b' to '\b',
      'f' to '\u000C',
      'n' to '\n',
      'r' to '\r',
      't' to '\t',
      '"' to '"',
      '\\' to '\\'
    )
  }

  fun parseWhole(): Any? {
    val result = parseValue()

    skipWhitespace()

    if (maxIndex != raw.length) {
      fail("start was valid but followed by invalid")
    }

    return result
  }

  private fun parseValue(): Any? {
    skipWhitespace()

    return when (val next = raw[index]) {
      in startDigit -> parseNumber()
      in keywordChars -> parseKeyword()
      '"' -> parseString()
      '[' -> parseArray()
      '{' -> parseObject()
      else -> fail("Unknown char '$next'")
    }
  }

  private fun parseNumber(): Any? {
    val buff = StringBuilder()

    while (index < maxIndex) {
      val next = raw[index]

      if (next in digits) {
        buff.append(next)
        advance()
      }
    }

    return buff.toString().toDoubleOrNull() ?: fail("Number not valid")
  }

  private fun parseKeyword(): Any? {
    val buff = StringBuilder()

    while (index < maxIndex) {
      val next = raw[index]

      if (next in keywordChars) {
        buff.append(next)
        advance()
      }
    }

    return when(buff.toString()) {
      "null" -> null
      "true" -> true
      "false" -> false
      else -> fail("Invalid keyword")
    }
  }

  private fun parseString(): String {
    advance() // skip the init quote
    val buff = StringBuilder()

    while (index < maxIndex) {
      val next = raw[index]

      if (next == '\\') {
        advance()

        if (index < maxIndex) {
          fail("Unclosed string")
        }

        val tail = raw[index]

        buff.append(escapeMap[tail] ?: fail("Invalid escape char '$tail'"))
      }

      if (next == '"') {
        advance()
        return buff.toString()
      }
    }

    fail("Unclosed string")
  }

  private fun parseArray(): List<Any?> {
    advance() // skip the init [
    val array = ArrayList<Any?>()

    while (index < maxIndex) {
      array += parseValue()

      skipWhitespace()

      when (raw[index]) {
        ',' -> advance()
        ']' -> {
          advance()
          return array
        }
        else -> fail("Array invalid")
      }
    }

    fail("Unclosed array")
  }

  private fun parseObject(): Map<String, Any?> {
    advance() // skip the init {
    val map = HashMap<String, Any?>()

    while (index < maxIndex) {
      skipWhitespace()

      if (raw[index] != '"') {
        fail("Invalid key to map")
      }

      val key = parseString()

      skipWhitespace()

      if (raw[index] != ':') {
        fail("Invalid map, expected ':'")
      }

      advance()

      val value = parseValue()

      map[key] = value

      skipWhitespace()

      when (raw[index]) {
        ',' -> advance()
        '}' -> {
          advance()
          return map
        }
        else -> fail("Invalid map")
      }
    }

    fail("Unclosed map")
  }

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

  private fun fail(cause: String): Nothing {
    throw IllegalArgumentException("parse/json found invalid json at $line:$col - $cause")
  }

}




