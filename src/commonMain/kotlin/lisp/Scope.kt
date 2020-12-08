package lisp
enum class ScopeKind {
  global,
  shell,
  module,
  local
}

class Scope(private val kind: ScopeKind, private val parent: Scope?) {

  private val content = HashMap<String, Any?>()

  operator fun get(key: String): Any? {
    return when {
      content.containsKey(key) -> content[key]
      parent != null -> parent[key]
      else -> throw InterpreterException("No such variable $key in scope")
    }
  }

  fun clear() {
    content.clear()
  }

  operator fun set(key: String, value: Any?) {
    content[key] = value
  }

  fun delete(key: String) {
    content.remove(key)
  }

  fun getGlobal(key: String): Any? {
    return if (kind == ScopeKind.global) {
      this[key]
    } else {
      parent?.getGlobal(key)
    }
  }

  fun setGlobal(key: String, value: Any?) {
    if (kind == ScopeKind.global) {
      this[key] = value
    } else {
      parent?.setGlobal(key, value)
    }
  }

  fun define(key: String, value: Any?) {
    if (kind == ScopeKind.module) {
      this[key] = value
    } else {
      parent?.define(key, value)
    }
  }

  fun child(kind: ScopeKind = ScopeKind.local): Scope = Scope(kind, this)

}
