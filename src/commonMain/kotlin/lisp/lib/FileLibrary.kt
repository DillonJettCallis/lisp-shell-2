package lisp.lib

import lisp.*
import lisp.runtime.Type

object FileLibrary: Library {
  override fun addLib(global: Scope) {
    global["new"] = object: FunctionValue {
      override val name: String = "new"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("source", Type.StringType, "file path")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val str = args[0] as String

        return File.from(str)
      }
    }

    global["file/isFile"] = object: FunctionValue {
      override val name: String = "file/isFile"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("file", Type.FileType, "file to check")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val path = args[0] as File

        return path.isFile()
      }
    }

    global["file/isDir"] = object: FunctionValue {
      override val name: String = "file/isDir"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("file", Type.FileType, "file to check")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val path = args[0] as File

        return path.isDir()
      }
    }

    global["file/exists"] = object: FunctionValue {
      override val name: String = "file/exists"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("file", Type.FileType, "file to check")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val path = args[0] as File

        return path.exists()
      }
    }

    global["file/list"] = object: FunctionValue {
      override val name: String = "file/list"
      override val params: List<ParamMeta> = listOf(
        ParamMeta("file", Type.FileType, "directory to list contents")
      )

      override fun call(args: List<Any?>, pos: Position): Any? {
        val path = args[0] as File

        return path.list()
      }
    }
  }
}
