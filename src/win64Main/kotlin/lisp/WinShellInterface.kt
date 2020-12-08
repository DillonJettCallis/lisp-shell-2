package lisp

import kotlinx.coroutines.runBlocking
import pw.binom.Console
import pw.binom.asyncInput
import pw.binom.io.readln
import pw.binom.io.utf8Reader

class WinShellInterface: ShellInterface {
  override fun read(): String {
    return runBlocking {
      Console
        .inChannel
        .asyncInput()
        .utf8Reader()
        .readln()
    } ?: ""
  }

  override fun write(content: String) {
    Console.err.append(content)
  }
}
