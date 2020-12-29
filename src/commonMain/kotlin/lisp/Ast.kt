package lisp
data class Position(val line: Int, val col: Int, val src: String) {
  private fun fail(kind: (String, Throwable?) -> Throwable, message: String, cause: Throwable? = null): Nothing {
    throw kind("$message at $line:$col $src", cause)
  }

  fun lexFail(message: String, cause: Throwable? = null): Nothing = fail(::LexException, message, cause)

  fun parseFail(message: String, cause: Throwable? = null): Nothing = fail(::ParseException, message, cause)

  fun compileFail(message: String, cause: Throwable? = null): Nothing = fail(::CompileException, message, cause)

  fun interpretFail(message: String, cause: Throwable? = null): Nothing = fail(::InterpreterException, message, cause)

  fun coerceFail(message: String, cause: Throwable? = null): Nothing = fail(::CoerceException, message, cause)
}

class ParseException(message: String, cause: Throwable? = null): RuntimeException(message, cause)
class LexException(message: String, cause: Throwable? = null): RuntimeException(message, cause)
class CompileException(message: String, cause: Throwable? = null): RuntimeException(message, cause)
class InterpreterException(message: String, cause: Throwable? = null): RuntimeException(message, cause)
class CoerceException(message: String, cause: Throwable? = null): RuntimeException(message, cause)

sealed class Expression {
  abstract val pos: Position
}

sealed class LiteralEx: Expression() {
  abstract val value: Any?
}

data class StringLiteralEx(override val value: String, val quoted: Boolean, override val pos: Position): LiteralEx()
data class IntLiteralEx(override val value: Int, override val pos: Position): LiteralEx()
data class DoubleLiteralEx(override val value: Double, override val pos: Position): LiteralEx()
data class BooleanLiteralEx(override val value: Boolean, override val pos: Position): LiteralEx()
data class NullLiteralEx(override val pos: Position): LiteralEx() {
  override val value: Any? = null
}

data class CommandEx(val value: String, override val pos: Position): Expression()
data class VariableEx(val name: String, override val pos: Position): Expression()
data class OperatorEx(val op: String, override val pos: Position): Expression()

data class CallEx(val body: List<Expression>, override val pos: Position): Expression()
data class ArrayEx(val body: List<Expression>, override val pos: Position): Expression()
data class MapEx(val body: List<Pair<Expression, Expression>>, override val pos: Position): Expression()

