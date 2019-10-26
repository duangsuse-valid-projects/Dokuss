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
      crossinline or: N.(Int) -> N, zero: N): (IntIterator) -> N = reader@{ ints ->
    var parsed = zero // initial
    for (part in ints)
      { parsed = parsed.shl(Byte.SIZE_BITS).or(part) }
    return@reader parsed
  }
  open fun onReadByte() {}
  private fun safelyRead() = s.read().also { onReadByte(); if(it == -1) throw EOFException() }
  private fun readBits32(bi: IntIterator) = read(Int::shl, Int::or, 0)(bi)
  private fun readBits64(bi: IntIterator) = read(Long::shl, { or(it.toLong()) }, 0L)(bi)
  private fun readN32(n: Cnt) = (n.timesIterator { safelyRead() }).asIntIterator().let(::readBits32)
  private fun readN64(n: Cnt) = (n.timesIterator { safelyRead() }).asIntIterator().let(::readBits64)

  @Deprecated("Use readByte().readBoolean() instead",
    replaceWith = ReplaceWith("readByte().readBoolean()"))
  override fun readBoolean(): Boolean = throw UnsupportedOperationException()

  override fun readFully(b: ByteArray?) = readFully(b, 0, b!!.size)
  override fun readFully(b: ByteArray?, off: Int, len: Int) { mayReadFully(b, off, len) }
  fun mayReadFully(b: ByteArray?, off: Int, len: Int): Int {
    if (len < 0) throw IndexOutOfBoundsException()
    val r = readFullyRec(b, off, len)
    return if (r == (-1)) len else (len-r)
  }
  /** @return -1 means normal fully readed; other: interrupted, length to read */
  private tailrec fun readFullyRec(b: ByteArray?, off: Int, len: Int): Int {
    if (len == 0) return (-1)
    val count = s.read(b, off, len)
    if (count < 0) return len
    return readFullyRec(b, off+count, len-count)
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
