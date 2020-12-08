package lisp

import java.util.*

class JvmShellInterface: ShellInterface {

  private val scanner = Scanner(System.`in`)

  override fun read(): String {
    return scanner.nextLine()
  }

  override fun write(content: String) {
    print(content)
  }
}
