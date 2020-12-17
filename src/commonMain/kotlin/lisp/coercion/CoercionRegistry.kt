package lisp.coercion

import lisp.*
import kotlin.reflect.KClass

object CoercionRegistry {

  private val coercers = HashMap<CoercionKey<*, *>, (Any) -> Any>()
  private val nameToType = HashMap<String, KClass<*>>()
  private val typeToName = HashMap<KClass<*>, String>()

  private fun <K: Any, V: Any> getMapper(src: KClass<K>, dest: KClass<V>): ((K) -> V)? {
    return coercers[CoercionKey(src, dest)] as ((K) -> V)?
  }

  private fun <K: Any, V: Any> addCoercer(src: KClass<K>, dest: KClass<V>, mapper: ((K) -> V)) {
    require(typeToName.containsKey(src)) { "Class '$src' not found in registry" }
    require(typeToName.containsKey(dest)) { "Class '$dest' not found in registry" }

    coercers[CoercionKey(src, dest)] = mapper as (Any) -> Any
  }

  private fun addType(clazz: KClass<*>, name: String) {
    nameToType[name] = clazz
    typeToName[clazz] = name
  }

  fun tryCoerce(value: Any, target: String): Any? {
    val targetClass = nameToType[target] ?: return null

    return tryCoerce(value, targetClass)
  }

  fun <In: Any, Out: Any> tryCoerce(value: In, target: KClass<Out>): Out? {
    if (target.isInstance(value)) {
      return value as Out
    }

    val mapper = getMapper(value::class as KClass<In>, target) ?: return null

    return try {
      mapper(value)
    } catch (e: Exception) {
      null
    }
  }

  fun checkType(value: Any, target: String): Boolean {
    val targetClass = nameToType[target] ?: throw IllegalArgumentException("Invalid checkType")

    return targetClass.isInstance(value)
  }

  fun nameOfType(type: KClass<*>): String? = typeToName[type]

  fun knownType(target: String): Boolean {
    return nameToType.containsKey(target)
  }

  init {
    addType(Any::class, "Any")
    addType(Boolean::class, "Boolean")
    addType(String::class, "String")
    addType(Double::class, "Float")
    addType(Int::class, "Integer")
    addType(KClass::class::class, "Type")

    addType(Path::class, "Path")
    addType(File::class, "File")

    addType(List::class, "Array")
    addType(Map::class, "Map")
    addType(FunctionValue::class, "Function")

    // type to string and back
    coercers[CoercionKey(String::class, KClass::class::class)] = { nameToType[it]!! }
    addCoercer(KClass::class::class, String::class) { typeToName[it]!! }

    // path, file, string
    addCoercer(String::class, Path::class, Path::from)
    addCoercer(String::class, File::class, File::from)
    addCoercer(Path::class, String::class, Path::toString)
    addCoercer(Path::class, File::class, File::from)
    addCoercer(File::class, Path::class, File::toPath)
    addCoercer(File::class, String::class, File::toString)

    // numbers back and forth
    addCoercer(Int::class, Double::class, Int::toDouble)
    addCoercer(Double::class, Int::class, Double::toInt)

    // numbers to string and back
    addCoercer(Int::class, String::class, Int::toString)
    addCoercer(Double::class, String::class, Double::toString)
    addCoercer(String::class, Int::class, String::toInt)
    addCoercer(String::class, Double::class, String::toDouble)
  }
}

data class CoercionKey<K: Any, V: Any>(val src: KClass<K>, val dest: KClass<V>)

inline fun <Target: Any> Any.coerceTo(target: KClass<Target>): Target? = CoercionRegistry.tryCoerce(this, target)

fun List<Any?>.coerceAll(target: List<ParamMeta>, pos: Position): List<Any?> {
  return mapIndexed { index, value ->
    val meta = target.getOrNull(index)

    if (meta == null || value == null) {
      value
    } else {
      val (name, type, desc) = meta

      CoercionRegistry.tryCoerce(value, type) ?: pos.interpretFail("Expected type ${type.coerceTo(String::class)} for field '$name'. Description: '$desc'")
    }
  }
}

