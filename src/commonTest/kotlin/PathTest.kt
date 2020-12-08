import lisp.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class PathTest {

  @Test()
  fun resolveTest() {
    fun doTest(start: String, end: String, expected: String) {
      val actual = Path.from("$start/$end").resolve().toString()

      assertEquals(expected, actual)
    }

    doTest("top/this", "./test", "top/this/test")
    doTest("top/this", "../test", "top/test")
    doTest("top/this",  ".", "top/this")
    doTest("top/this",  "..", "top")

    doTest("./this", "./test", "./this/test")
    doTest("./this", "../test", "./test")
    doTest("./this", ".", "./this")
    doTest("./this", "..", ".")

    doTest("../this", "./test", "../this/test")
    doTest("../this", "../test", "../test")
    doTest("../this", ".", "../this")
    doTest("../this", "..", "..")

    doTest(".", "./test", "./test")
    doTest(".", "../test", "../test")
    doTest(".", ".", ".")
    doTest(".", "..", "..")

    doTest("..", "./test", "../test")
    doTest("..", "../test", "../../test")
    doTest("..", ".", "..")
    doTest("..", "..", "../..")
  }

}
