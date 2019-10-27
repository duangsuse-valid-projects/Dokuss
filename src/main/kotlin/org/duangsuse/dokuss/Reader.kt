package org.duangsuse.dokuss

import org.duangsuse.dokuss.bytes.*
import org.duangsuse.dokuss.intf.Reader
import java.io.*
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/** @see Reader */
sealed class Reader(protected val s: InputStream): FilterInputStream(s), Reader {
  private val ds = object: AuxDataInput(this as FilterInputStream) {
    override fun onByteRead() { position += 1 }
  }
  override var byteOrder: ByteOrder = ByteOrder.system

  override val estimate: ZCnt get() = this.available()
  override var position: Idx = 0
    protected set

  override fun read(b: ByteArray?) = read(b, 0, b!!.size)
  override fun read(b: ByteArray?, off: Int, len: Int) = ds.mayReadFully(b!!, len, off).also { position += len }

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
  /** Call [resetToBegin] explicitly to reset without changing marker state
   *
   * This library can define a `markedTimes` to set `position = 0` when reset 'stack' is empty,
   *  but this is unnecessary since resetToBegin()/reset() is usually determined by programmer before program runs */
  override fun reset() = s.reset().also { marking = false; position = oldPos }
  override fun resetToBegin() = s.reset()
  override val isMarking: Boolean get() = marking

  inline val hasRemaining: Boolean get() = position <= estimate
  /** This value could be negative, and `position = position+remaining always @EOF` */
  inline val remaining: Cnt get() = estimate - position

  open val newInstance: (InputStream) -> org.duangsuse.dokuss.Reader = ::Instance
  override fun restream(re: (InputStream) -> InputStream): Reader {
    val mapped = newInstance(re(this.s))
    mapped.byteOrder = this.byteOrder
    mapped.position = this.position
    mapped.marking = this.marking
    mapped.oldPos = this.oldPos
    return mapped
  }

  /** Instance for sealed type [Reader] */
  class Instance(s: InputStream): org.duangsuse.dokuss.Reader(s) {
    override fun toString(): String = "Reader.Instance($s)"
  }
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

    private var oldLongPos: Long = 0
    override fun mark(rl: Cnt) = mark()
    fun mark() { marking = true; oldLongPos = longPosition }
    fun <R> positional(op: () -> R): R {
      mark(); try { return op() } finally { reset() }
    }
    override fun reset() { marking = false; longPosition = oldLongPos }
    override fun close() = raf.close()

    override fun toString(): String = "Reader.File(#${raf.fd}:${raf.channel}@${raf.filePointer}, L${raf.length()})"
  }
  /** @see [java.net.URL.openConnection]
   *  @see [java.net.URLConnection.getInputStream]
   *  @since JDK 1.0 */
  data class URL(val conn: java.net.URLConnection): org.duangsuse.dokuss.Reader(conn.getInputStream()) {
    constructor(url: java.net.URL): this(url.openConnection())
    constructor(url_str: String): this(java.net.URL(url_str))
    override fun toString(): String = "Reader.URL(${conn.url})"
  }
  /** NOTE: [ByteArrayInputStream] uses estimate related to [position], [estimate] == [remaining], [size] == old [estimate] */
  class InMemory(val buffer: Buffer): org.duangsuse.dokuss.Reader(buffer.inputStream()) {
    constructor(str: String, cs: Charset = StandardCharsets.UTF_16): this(str.toByteArray(cs))
    constructor(size: Cnt, initial: Byte = 0x00): this(ByteArray(size) {initial})
    inline val size: Cnt get() = buffer.size
    override fun toString(): String = "Reader.InMemory(${buffer.size}$buffer)"
  }
}
