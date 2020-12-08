package lisp

import kotlinx.cinterop.*
import platform.windows.*

class WinCommand: Command {
  override fun execute(cwd: File, command: String, args: List<String>): String {
    val fullCommand = (listOf(command) + args).map { it.replace("\"", "\\\"") }.joinToString(" ") { "\"$it\"" }

    memScoped {
      val startInfo = alloc<STARTUPINFOA>()

      SecureZeroMemory!!(startInfo.ptr, sizeOf<STARTUPINFOA>().toULong())
      startInfo.cb = sizeOf<STARTUPINFOA>().toUInt()

      val processInfo = alloc<PROCESS_INFORMATION>()

      val errorCode = CreateProcessA(
        lpApplicationName = null,
        lpCommandLine = fullCommand.cstr.ptr,
        lpProcessAttributes = null,
        lpThreadAttributes = null,
        bInheritHandles = 0,
        dwCreationFlags = 0u,
        lpEnvironment = null,
        lpCurrentDirectory = cwd.toString(),
        lpStartupInfo = startInfo.ptr, // might need actual value
        lpProcessInformation = processInfo.ptr // might need actual value
      )

      if (errorCode == 0) {
        throw IllegalArgumentException("Failed to run command")
      }

      CloseHandle(processInfo.hProcess)
      CloseHandle(processInfo.hThread)

      return "just a test"
    }
  }
}
