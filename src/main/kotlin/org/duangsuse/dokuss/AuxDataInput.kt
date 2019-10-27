package org.duangsuse.dokuss

import org.duangsuse.dokuss.bytes.*
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
  override fun readByte(): Byte = readUnsignedByte().toByte()
  override fun readShort(): Short = readUnsignedShort().toShort()
  override fun readChar(): Char = readN32(Char.SIZE_BYTES).toChar()
  override fun readInt(): Int = readN32(Int.SIZE_BYTES)
  override fun readLong(): Long = readN64(Long.SIZE_BYTES)

  override fun readFloat(): Float = Float.fromBits(readInt())
  override fun readDouble(): Double = Double.fromBits(readLong())

  override fun readUnsignedByte(): Int = safelyRead()
  override fun readUnsignedShort(): Int = readN32(Short.SIZE_BYTES)

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
  /** Can be used from Java, semi-public interface, uh. */
  internal fun readBytes(n: Cnt): IntArray {
    val readbuf = ByteArray(n).also { readFully(it) }
    n.doTimes(::onByteRead)
    val extbuf = readbuf.toTypedArray().mapArray({ Array(it) {0x00} }, Byte::toInt)
    return extbuf.toIntArray()
  }

  private fun readBits32(bi: IntIterator) = read(Int::shl, Int::or, 0)(bi)
  private fun readBits64(bi: IntIterator) = read(Long::shl, { or(it.toLong()) }, 0L)(bi)
  private fun readN32(n: Cnt): Int32 = (n.timesIterator { safelyRead() }).asIntIterator().let(::readBits32)
  private fun readN64(n: Cnt): Int64 = readBits64(readBytes(n).iterator())

  @Deprecated("Use readByte().readBoolean() instead",
    replaceWith = ReplaceWith("readByte().readBoolean()"))
  override fun readBoolean(): Boolean = throw UnsupportedOperationException()

  override fun readFully(b: ByteArray?) = readFully(b, 0, b!!.size)
  override fun readFully(b: ByteArray?, off: Int, len: Int) {
    val cnt = mayReadFully(b!!, len, off)
    if (cnt != len) throw EOFException()
  }

  /** @return count of bytes actually read */
  fun mayReadFully(dst: ByteArray, len: Cnt, pos_0: Idx): Cnt {
    if (len < 0) throw IndexOutOfBoundsException()
    val r = mayReadFullyRec(dst, len, pos_0)
    return if (r == null) len else (len - r)
  }
  /** @return `null` means normally fully read; other: interrupted, length rest */
  private tailrec fun mayReadFullyRec(dst: ByteArray, rest: Cnt, pos_x: Idx): Cnt? {
    if (rest == 0) return null
    val count = s.read(dst, pos_x, rest)
    if (count == (-1)) return rest
    return mayReadFullyRec(dst, rest-count, pos_x +count)
  }

  /** @see skipBytesRec
   *  @return bytes actually skipped */
  override fun skipBytes(n: Int): Int  = skipBytesRec(n).let { n - it }
  /**
   * `total < n` (`n-total>0`, `n != 0`): this function uses back-counting
   * since skipped is positive integer `(<= n)`, `(== 0)` is used
   *
   * `cur = in.skip(n-total)`, `if(cur <= 0) break`, `total += cur`: back-counting
   * since `skip(_)` only return zero or positive integer, `(== 0)` is used
   *
   * @see DataInputStream.skipBytes
   */
  private tailrec fun skipBytesRec(n: Cnt): Cnt {
    if (n == 0) return n
    val skipped: Cnt = s.skip(n.toLong()).toInt()
    if (skipped == 0) return n
    return skipBytesRec(n-skipped) // (- skipped) is accumulative
  }
}

fun <T> ZCnt.timesIterator(op: (Int) -> T) = object: Iterator<T> {
  var count = this@timesIterator
  override fun hasNext(): Boolean = count != 0
  override fun next(): T { --count; return op(count) }
}
fun <T> Iterator<T>.asIterable() = object: Iterable<T> {
  override fun iterator(): Iterator<T> = this@asIterable
}
fun Iterator<Int>.asIntIterator() = object: IntIterator() {
  override fun hasNext(): Boolean = this@asIntIterator.hasNext()
  override fun nextInt(): Int = this@asIntIterator.next()
}
fun ZCnt.doTimes(op: () -> Unit) {
  for (_t in 1..this) op()
}

fun Byte.readBoolean() = this != 0.toByte()
fun <E> Array<E>.arrayMove(dst: Array<in E>, n: Cnt = this.size) = System.arraycopy(this, 0, dst, 0, n)

fun <E, R> Array<E>.mapArray(new_ary: (Cnt) -> Array<R>, f: (E) -> R): Array<R> {
  val dest = new_ary(this.size)
  for (x in this.withIndex()) {
    dest[x.index] = f(x.value)
  }
  return dest
}
