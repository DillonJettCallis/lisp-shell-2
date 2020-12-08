package lisp
inline fun <In, Out> Iterable<In>.transform(mapper: In.((Out) -> Unit) -> Unit): List<Out> {
  val result = ArrayList<Out>()

  this.forEach { item ->
    mapper (item) { result.add(it) }
  }

  return result
}

