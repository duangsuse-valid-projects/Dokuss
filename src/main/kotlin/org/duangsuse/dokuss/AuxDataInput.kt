package org.duangsuse.dokuss

import org.duangsuse.dokuss.bytes.Cnt
import java.io.InputStream
import java.io.DataInput
import java.io.DataInputStream
import java.io.EOFException

/**
 * Auxiliary class for parsing binary data input
 * @see DataInputStream
 * @see InputStream.read
 */
open class AuxDataInput(private val s: InputStream): DataInput by DataInputStream(s) {
  override fun readUnsignedByte(): Int = safelyRead()
  override fun readByte(): Byte = readUnsignedByte().toByte()
  override fun readUnsignedShort(): Int = readN32(Short.SIZE_BYTES)
  override fun readShort(): Short = readUnsignedShort().toShort()
  override fun readChar(): Char = readN32(Char.SIZE_BYTES).toChar()
  override fun readInt(): Int = readN32(Int.SIZE_BYTES)
  override fun readLong(): Long = readN64(Long.SIZE_BYTES)

  override fun readFloat(): Float = Float.fromBits(readInt())
  override fun readDouble(): Double = Double.fromBits(readLong())

  /** Abstraction `((ch1 << 8) + (ch2 << 0))`, and operation [shl] must be accumulative */
  private inline fun <N> read(crossinline shl: N.(Cnt) -> N,
      crossinline or: N.(Int) -> N, zero: N): (IntIterator) -> N = reader@{ bytes ->
    var parsed = zero // initial
    for (part in bytes)
      { parsed = parsed.shl(Byte.SIZE_BITS).or(part) }
    return@reader parsed
  }
  open fun onByteRead() {}
  private fun safelyRead() = s.read().also { if(it == -1) throw EOFException(); onByteRead(); }
  private fun readBits32(bi: IntIterator) = read(Int::shl, Int::or, 0)(bi)
  private fun readBits64(bi: IntIterator) = read(Long::shl, { or(it.toLong()) }, 0L)(bi)
  private fun readN32(n: Cnt) = (n.timesIterator { safelyRead() }).asIntIterator().let(::readBits32)
  private fun readN64(n: Cnt): Long = readBits64(readBytes(n).iterator())
  /** Can be used from Java, semi-public interface, uh. */
  internal fun readBytes(n: Cnt): IntArray {
    val readbuf = ByteArray(n).also { readFully(it) }
    n.doTimes(::onByteRead)
    val extbuf = IntArray(n)
    readbuf.toTypedArray().arraymove(extbuf)
    return extbuf
  }

  @Deprecated("Use readByte().readBoolean() instead",
    replaceWith = ReplaceWith("readByte().readBoolean()"))
  override fun readBoolean(): Boolean = throw UnsupportedOperationException()

  override fun readFully(b: ByteArray?) = readFully(b, 0, b!!.size)
  override fun readFully(b: ByteArray?, off: Int, len: Int) {
    val cnt = mayReadFully(b, off, len)
    if (cnt != len) throw EOFException()
  }
  fun mayReadFully(b: ByteArray?, off: Int, len: Int): Int {
    if (len < 0) throw IndexOutOfBoundsException()
    val r = readFullyRec(b, off, len)
    return if (r == (-1)) len else (len-r)
  }
  /** @return -1 means normally fully read; other: interrupted, length read */
  private tailrec fun readFullyRec(b: ByteArray?, off: Int, len: Int): Int {
    if (len == 0) return (-1)
    val count = s.read(b, off, len)
    if (count < 0) return len
    return readFullyRec(b, off+count, len-count)
  }

  override fun skipBytes(n: Int): Int {
    val rest = skipBytesRec(n)
    return n - rest
  }
  /**
   * `total < n` (`n-total>0`, `n != 0`): this function uses back-counting
   * since skipped is positive integer `(<= n)`, `(== 0)` is used
   *
   * `cur = in.skip(n-total)`, `if(cur <= 0) break`, `total += cur`: back-counting
   * since `skip(_)` only return zero or positive integer, `(== 0)` is used
   *
   * @see DataInputStream.skipBytes
   */
  private tailrec fun skipBytesRec(n: Int): Int {
    if (n == 0) return n
    val skipped: Cnt = s.skip(n.toLong()).toInt()
    if (skipped == 0) return n
    return skipBytesRec(n-skipped) // (- skipped) is accumulative
  }
}

fun <T> Int.timesIterator(op: (Int) -> T) = object: Iterator<T> {
  var count = this@timesIterator
  override fun hasNext(): Boolean = count != 0
  override fun next(): T { val r = op(count); --count; return r }
}
fun Iterator<Int>.asIntIterator() = object: IntIterator() {
  override fun hasNext(): Boolean = this@asIntIterator.hasNext()
  override fun nextInt(): Int = this@asIntIterator.next()
}
fun Byte.readBoolean() = this != 0.toByte()
fun Array<*>.arraymove(dst: Any, n: Cnt = this.size) = System.arraycopy(this, 0, dst, 0, n)

fun Int.doTimes(op: () -> Unit) {
  for (_t in 1..this) op()
}
