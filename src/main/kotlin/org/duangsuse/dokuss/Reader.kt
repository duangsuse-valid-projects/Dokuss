package org.duangsuse.dokuss

import org.duangsuse.dokuss.bytes.*
import org.duangsuse.dokuss.intf.Reader
import java.io.*
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/** @see Reader */
sealed class Reader(private val s: InputStream): FilterInputStream(s), Reader {
  private val ds: DataInput = DataInputStream(this as FilterInputStream)
  override var byteOrder: ByteOrder = ByteOrder.jvm

  override val estimate: ZCnt get() = this.available()
  override var position: Idx = 0
    protected set

  override fun read() = s.read().also { position += 1 }
  override fun read(b: ByteArray?) = s.read(b).also { position += it }
  override fun read(b: ByteArray?, off: Int, len: Int) = s.read(b, off, len).also { position += it }

  override fun readAllTo(dst: Buffer) { read(dst) }
  override fun readTo(dst: Buffer, cnt: Cnt, idx: Idx) { read(dst, idx, cnt) }
  override tailrec fun seek(n: LongCnt) {
    if (n == 0L) return //EOS
    val skipped = this.skip(n)
    if (skipped != 0L) seek(n - skipped)
  }

  private inline fun swept(crossinline read: DataInput.() -> Number) = if (shouldSwap)
    ByteOrder.PrimSwapper.swap(ds.read()) else ds.read()
  override fun readInt8(): Int8 = swept(DataInput::readByte) as Int8
  override fun readInt16(): Int16 = swept(DataInput::readShort) as Int16
  override fun readChar16(): Char16 = swept { readChar().toShort() }.toChar()
  override fun readInt32(): Int32 = swept(DataInput::readInt) as Int32
  override fun readInt64(): Int64 = swept(DataInput::readLong) as Int64
  override fun readRat32(): Rat32 = swept(DataInput::readFloat) as Rat32
  override fun readRat64(): Rat64 = swept(DataInput::readDouble) as Rat64

  /** This function is correctly written (even [ByteOrder.Swapper] uses [shr] not [Int.ushr]),
   *  since Java uses _signed 32-bit_ to encode _unsigned_ types */
  override fun readNat8(): Nat8 = swept(DataInput::readUnsignedByte) as Nat8
  /** Read a 16-bit integral, convert it to unsigned format
   * @see readNat8 */
  override fun readNat16(): Nat16 = swept(DataInput::readUnsignedShort) as Nat16

  override fun readStringUTF(): String = ds.readUTF()

  protected var marking: Boolean = false
  private var oldPos: Idx = 0
  override fun mark(rl: Cnt) = s.mark(rl).also { marking = true; oldPos = position }
  override fun reset() = s.reset().also { marking = false; position = oldPos }
  override val isMarking: Boolean get() = marking

  inline val hasRemaining: Boolean get() = position <= estimate
  inline val remaining: Cnt get() = estimate - position

  /** [Reader] Wrapper for [RandomAccessFile], note large files (with [Long] file ptr) are not supported, so sad. */
  class File(private val raf: RandomAccessFile): org.duangsuse.dokuss.Reader(FileInputStream(raf.fd)) {
    constructor(file: java.io.File): this(RandomAccessFile(file, "r"))
    constructor(path: String): this(java.io.File(path))

    val longEstimate: Long get() = raf.length()
    override val estimate: ZCnt get() = longEstimate.toInt()
    var longPosition: Long
      get() = raf.filePointer
      set(pos) = raf.seek(pos)
    override var position: Idx
      get() = longPosition.toInt()
      set(pos) { longPosition = pos.toLong() }
    override fun seek(n: LongCnt) = raf.seek(longPosition+n)

    private var oldPos: Long = 0
    override fun mark(rl: Cnt) { marking = true; oldPos = longPosition }
    override fun reset() { marking = false; longPosition = oldPos }
    override fun close() = raf.close()

    override fun toString(): String = "Reader.File($raf)"
  }
  data class URL(val conn: java.net.URLConnection): org.duangsuse.dokuss.Reader(conn.getInputStream()) {
    constructor(url: java.net.URL): this(url.openConnection())
    constructor(url_str: String): this(java.net.URL(url_str))
    override fun toString(): String = "Reader.URL($conn)"
  }
  class InMemory(val buffer: Buffer): org.duangsuse.dokuss.Reader(buffer.inputStream()) {
    constructor(str: String, cs: Charset = StandardCharsets.UTF_16): this(str.toByteArray(cs))
    constructor(size: Cnt, initial: Byte = 0x00): this(ByteArray(size) {initial})
    override fun toString(): String = "Reader.InMemory(${buffer.size})"
  }
}
