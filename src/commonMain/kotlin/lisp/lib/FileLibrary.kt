package lisp.lib

import lisp.File
import lisp.FunctionValue
import lisp.Position
import lisp.Scope
import lisp.coercion.coerceTo

object FileLibrary: Library {
  override fun addLib(global: Scope) {
    global["file"] = object: FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 1) {
          pos.interpretFail("Expected one argument to 'file'")
        }

        val (strRaw) = args

        val str = strRaw?.coerceTo(String::class) ?: pos.interpretFail("Expected first argument to 'file' to be a string")

        return File.from(str)
      }
    }


    global["file/isFile"] = object: FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 1) {
          pos.interpretFail("Expected one argument to 'file/isFile'")
        }

        val (pathRaw) = args

        val path = pathRaw?.coerceTo(File::class) ?: pos.interpretFail("Expected first argument to 'file/isFile' to be a path")

        return path.isFile()
      }
    }

    global["file/isDir"] = object: FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 1) {
          pos.interpretFail("Expected one argument to 'path/isDir'")
        }

        val (pathRaw) = args

        val path = pathRaw?.coerceTo(File::class) ?: pos.interpretFail("Expected first argument to 'file/isDir' to be a path")

        return path.isDir()
      }
    }

    global["file/exists"] = object: FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 1) {
          pos.interpretFail("Expected one argument to 'file/exists'")
        }

        val (pathRaw) = args

        val path = pathRaw?.coerceTo(File::class) ?: pos.interpretFail("Expected first argument to 'file/exists' to be a path")

        return path.exists()
      }
    }

    global["file/list"] = object: FunctionValue {
      override fun call(args: List<Any?>, pos: Position): Any? {
        if (args.size != 1) {
          pos.interpretFail("Expected one argument to 'file/list'")
        }

        val (pathRaw) = args

        val path = pathRaw?.coerceTo(File::class) ?: pos.interpretFail("Expected first argument to 'file/list' to be a path")

        return path.list()
      }
    }
  }
}
