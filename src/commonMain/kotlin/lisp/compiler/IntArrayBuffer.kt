package lisp.compiler

class IntArrayBuffer(initCapacity: Int = 20) {

  private var content: IntArray = IntArray(initCapacity)
  private var index = 0

  val size: Int
    get() = index

  fun push(value: Int) {
    checkCapacity(1)
    content[index++] = value
  }

  fun pop(): Int {
    if (index == 0) {
      throw IndexOutOfBoundsException("Cannot pop empty array")
    }

    return content[index--]
  }

  fun pushAll(block: IntArray) {
    checkCapacity(block.size)

    block.copyInto(content, index)
    index += block.size
  }

  fun build(): IntArray {
    return content.copyOf(index)
  }

  operator fun get(index: Int): Int = content[index]
  operator fun set(index: Int, value: Int) {
    content[index] = value
  }

  private fun checkCapacity(additional: Int) {
    if (index + additional >= content.size) {
      // resize the array by doubling the full size after this additional is added
      content = content.copyOf((index + additional) * 2)
    }
  }
}
