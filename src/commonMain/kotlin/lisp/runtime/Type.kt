package lisp.runtime

import lisp.File
import lisp.ParamMeta
import lisp.Path
import lisp.Position
import lisp.bytecode.ClosureFunction
import lisp.bytecode.NativeFunction

enum class Type {
  AnyType,
  StringType,
  BooleanType,
  IntegerType,
  FloatType,
  FileType,
  PathType,
  TypeType,
  ArrayType,
  MapType,
  FunctionType;

  companion object {
    private val coercions = HashMap<Pair<Type, Type>, (Any) -> Any>()

    private fun <K, V> registerCoercion(from: Type, to: Type, transform: (K) -> V) {
      coercions[from to to] = transform as (Any) -> Any
    }

    fun coerceAll(values: List<Any?>, target: List<ParamMeta>, pos: Position): List<Any?> {
      return values.mapIndexed { index, value ->
        val meta = target.getOrNull(index)

        if (meta == null || value == null) {
          value
        } else {
          val (name, type, desc) = meta

          coerce(type, value) ?: pos.interpretFail("Expected type ${type.name.substringBeforeLast("Type")} for field '$name'. Description: '$desc'")
        }
      }
    }

    fun coerce(to: Type, value: Any?): Any? {
      if (to == AnyType) {
        return value
      }

      if (value == null) {
        return null
      }

      val from = typeOf(value)

      return if (to == from) {
        value
      } else {
        val coercion = coercions[from to to] ?: return null

        coercion(value)
      }
    }

    init {

      // type to string and back
      registerCoercion<String, Type>(StringType, TypeType) { valueOf(it + "Type") }
      registerCoercion<Type, String>(TypeType, StringType) { it.name.substringBeforeLast("Type") }

      // path, file, string
      registerCoercion(StringType, PathType, Path::from)
      registerCoercion<String, File>(StringType, FileType, File::from)
      registerCoercion(PathType, StringType, Path::toString)
      registerCoercion<Path, File>(PathType, FileType, File::from)
      registerCoercion(FileType, PathType, File::toPath)
      registerCoercion(FileType, StringType, File::toString)

      // numbers back and forth
      registerCoercion(IntegerType, FloatType, Int::toDouble)
      registerCoercion(FloatType, IntegerType, Double::toInt)

      // numbers to string and back
      registerCoercion(IntegerType, StringType, Int::toString)
      registerCoercion(FloatType, StringType, Double::toString)
      registerCoercion(StringType, IntegerType, String::toInt)
      registerCoercion(StringType, FloatType, String::toDouble)

      // boolean to string and back
      registerCoercion(BooleanType, StringType, Boolean::toString)
      registerCoercion<String, Boolean>(StringType, BooleanType) {
        when (it.toLowerCase().trim()) {
          "true" -> true
          "false" -> false
          else -> throw IllegalArgumentException("Invalid boolean value '${it}'")
        }
      }
    }

    fun typeOf(obj: Any?): Type {
      return when (obj) {
        null -> AnyType
        is String -> StringType
        is Boolean -> BooleanType
        is Int -> IntegerType
        is Double -> FloatType
        is File -> FileType
        is Path -> PathType
        is Type -> TypeType
        is ArrayList<*> -> ArrayType
        is HashMap<*, *> -> MapType
        is ClosureFunction, is NativeFunction -> FunctionType
        else -> AnyType
      }
    }
  }
}
