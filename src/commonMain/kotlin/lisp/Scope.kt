package lisp

import lisp.runtime.Type

enum class ScopeKind {
  global,
  shell,
  export,
  module,
  local
}

class Scope(private val kind: ScopeKind, private val parent: Scope?) {

  private val content = HashMap<String, Any?>()

  operator fun get(key: String): Any? {
    return when {
      key == "(scope)" -> this
      content.containsKey(key) -> content[key]
      parent != null -> parent[key]
      else -> throw RuntimeException("No such variable $key in scope")
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

  fun all(): Set<Map.Entry<String, Any?>> = content.entries

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

  fun export(key: String, value: Any?) {
    if (kind == ScopeKind.export) {
      this[key] = value
    } else {
      parent?.export(key, value)
    }
  }

  fun export(): Map<String, Any?> {
    return HashMap(content)
  }

  fun global(): Scope = if (this.kind == ScopeKind.global) this else parent!!.global()

  fun child(kind: ScopeKind = ScopeKind.local): Scope = Scope(kind, this)

  fun include(values: Map<String, Any?>, prefix: String?) {
    if (prefix == null) {
      values.forEach { (key, value) -> define(key, value) }
    } else {
      values.forEach { (key, value) -> define("$prefix/$key", value) }
    }
  }

  fun constructModuleScope(): Pair<Scope, Scope> {
    val exportScope = child(ScopeKind.export)
    val moduleScope = exportScope.child(ScopeKind.module)

    moduleScope["export"] = object: FunctionValue {
      override val name: String = "export"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("name", Type.StringType, "variable name to export"),
        ParamMeta("value", Type.AnyType, "value to export")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val (rawName, value) = args

        exportScope[rawName as String] = value
        return value
      }
    }

    return exportScope to moduleScope
  }

}
