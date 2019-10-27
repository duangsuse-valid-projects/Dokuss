package org.duangsuse.dokuss

import org.duangsuse.dokuss.bytes.*
import org.duangsuse.dokuss.intf.Reader
import sun.misc.SharedSecrets
import java.io.*
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * + Rewrite [position]/[estimate]/[resetToBegin] for random-access child-classes pls.
 * + Rewrite [mark]/[reset] for every child-class pls.
 *
 * @see Reader */
sealed class Reader(protected val s: InputStream): FilterInputStream(s), Reader {
  private val ds = object: AuxDataInput(s) {
    override fun onByteRead() { countRead += 1 }
    override fun onBulkRead(n: Cnt) { countRead += n }
    override fun onBulkSkip(n: Cnt) { countRead += n }
  }
  override var byteOrder: ByteOrder = ByteOrder.system

  //// Position | sequence length
  protected var countRead: ZCnt = 0
  override val estimate: ZCnt get() = this.available()
  override val position: Idx get() = countRead

  /** This value could be negative, and `position = position+remaining always @EOF` */
  inline val remaining: Cnt get() = estimate - position
  inline val hasRemaining: Boolean get() = position <= estimate

  //// Control
  /** NOTE: This function uses [AuxDataInput.safelyRead] from [ds] to update [position] by `1` */
  override fun read(): Int = ds.safelyRead()
  override fun read(b: ByteArray?) = this.read(b, 0, b!!.size)
  /** NOTE: This function uses [AuxDataInput.mayReadFullyRec] from [ds] to update [position]; result int: __actual read__ */
  override fun read(b: ByteArray?, off: Int, len: Int): Cnt = ds.mayReadFully(b!!, len, off)
    .let { if (it == 0) (-1) else it }

  override fun skip(n: LongCnt): Long = skipRec(n).let { n - it }
  private tailrec fun skipRec(rest: LongCnt): LongCnt {
    if (rest == 0L) return rest //EOS
    val skipped = s.skip(rest)
    if (skipped == 0L) return rest
    ds.onBulkSkip(skipped.toInt()) //LOSS!
    return skipRec(rest-skipped)
  }

  override fun readAllTo(dst: Buffer) { read(dst) }
  override fun readTo(dst: Buffer, cnt: Cnt, idx: Idx) { read(dst, idx, cnt) }
  /** NOTE: This function updates [position] by actually skipped byte count */
  override fun seek(n: LongCnt) { skip(n) }

  //// Read scalar data types
  private inline fun swept(crossinline read: DataInput.() -> Number) = if (shouldSwap)
    ByteOrder.PrimSwapper.swap(ds.read()) else ds.read()
  override fun readInt8(): Int8 = swept(DataInput::readByte) as Int8
  override fun readInt16(): Int16 = swept(DataInput::readShort) as Int16
  override fun readChar16(): Char16 = swept { readChar().toShort() }.toChar()
  override fun readInt32(): Int32 = swept(DataInput::readInt) as Int32
  override fun readInt64(): Int64 = swept(DataInput::readLong) as Int64
  override fun readRat32(): Rat32 = swept(DataInput::readFloat) as Rat32
  override fun readRat64(): Rat64 = swept(DataInput::readDouble) as Rat64

  //// Read extension scalar data types
  /** This function is correctly written (even [ByteOrder.Swapper] uses [shr] not [Int.ushr]),
   *  since Java uses _signed 32-bit_ to encode _unsigned_ types */
  override fun readNat8(): Nat8 = swept(DataInput::readUnsignedByte) as Nat8
  /** Read a 16-bit integral, convert it to unsigned format
   * @see readNat8 */
  override fun readNat16(): Nat16 = swept(DataInput::readUnsignedShort) as Nat16
  override fun readStringUTF(): String = ds.readUTF()

  //// Mark | reset
  override val isMarking: Boolean get() = marking
  protected var marking: Boolean = false
  open override fun mark(rl: Cnt) { marking = true }
  open override fun reset() { marking = false }
  override fun resetToBegin() = reset()

  open val newInstance: (InputStream) -> org.duangsuse.dokuss.Reader = ::Instance
  override fun restream(re: (InputStream) -> InputStream): Reader {
    val mapped = newInstance(re(this.s))
    mapped.byteOrder = this.byteOrder
    mapped.countRead = this.countRead
    mapped.marking = this.marking
    return mapped
  }

  /** Instance for sealed type [Reader]
   *
   * Call [resetToBegin] __explicitly__ to reset without changing marker state [oldReadCount], [marking].
   *
   * This library _can_ define a `var markedTimes: Cnt` to set `position = 0` when reset 'stack' is empty `reset: (markedTimes == 0), --it`
   *
   * but this is _unnecessary_ since [resetToBegin]/[reset] is usually __determined by programmer__ before program runs
   */
  open class Instance(s: InputStream): org.duangsuse.dokuss.Reader(s) {
    private var oldReadCount: Idx = 0
    override fun mark(rl: Cnt) = super.mark(rl).also { s.mark(rl); oldReadCount = countRead }
    override fun reset() = super.reset().also { s.reset(); countRead = oldReadCount }
    override fun resetToBegin() = s.reset()

    override fun restream(re: (InputStream) -> InputStream): Reader
      = super.restream(re).let { (it as Instance).oldReadCount = this.oldReadCount; it }

    override fun toString(): String = "Reader.Instance($s)"
  }

  /** [Reader] Wrapper for [RandomAccessFile], note large files (with [Long] file ptr) are not supported, so sad. */
  class File(private val raf: RandomAccessFile, private val name: String): org.duangsuse.dokuss.Reader(FileInputStream(raf.fd)) {
    constructor(file: java.io.File, name: String): this(RandomAccessFile(file, "r"), name)
    constructor(path: String): this(java.io.File(path), path)

    val longEstimate: LongZCnt get() = raf.length()
    override val estimate: ZCnt get() = longEstimate.toInt()
    var longPosition: LongIdx
      get() = raf.filePointer
      set(pos) = raf.seek(pos)
    override var position: Idx
      get() = longPosition.toInt()
      set(pos) { longPosition = pos.toLong() }
    override fun seek(n: LongZCnt) = raf.seek(longPosition+n)

    private var oldLongPos: Long = 0
    override fun mark(rl: Cnt) = mark()
    fun <R> positional(op: () -> R): R {
      mark(); try { return op() } finally { reset() } }

    fun mark() { marking = true; oldLongPos = longPosition }
    override fun reset() { marking = false; longPosition = oldLongPos }
    override fun resetToBegin() { longPosition = 0L }

    override fun close() = raf.close()
    override fun toString(): String = "Reader.File(#${fd.toHexString()}[$name]@${raf.filePointer}:${raf.length()})"
    val fd: Int get() = SharedSecrets.getJavaIOFileDescriptorAccess().get(raf.fd)
    val file: RandomAccessFile get() = this.raf
  }
  /** @see [java.net.URL.openConnection]
   *  @see [java.net.URLConnection.getInputStream]
   *  @since JDK 1.0 */
  data class URL(val conn: java.net.URLConnection): org.duangsuse.dokuss.Reader.Instance(conn.getInputStream()) {
    constructor(url: java.net.URL): this(url.openConnection())
    constructor(url_str: String): this(java.net.URL(url_str))

    override fun toString(): String = "Reader.URL(${conn.url}, ${describe()})"
    fun describe() = "${conn.contentEncoding ?: "?coding"}/${conn.contentType}:${conn.contentLengthLong}"
  }
  /** NOTE: [ByteArrayInputStream] uses estimate related to [position], [estimate] == [remaining], [size] == old [estimate] */
  class InMemory(val buffer: Buffer): org.duangsuse.dokuss.Reader.Instance(buffer.inputStream()) {
    constructor(str: String, cs: Charset = StandardCharsets.UTF_16): this(str.toByteArray(cs))
    constructor(size: Cnt, initial: Byte = 0x00): this(ByteArray(size) {initial})

    inline val size: Cnt get() = buffer.size
    fun toString(viewport: ZCnt, sep: String = "|"): String
      = "Reader.InMemory(${buffer.size}$buffer]${buffer.slice(0 until viewport).joinToString(sep)})"
    override fun toString(): String = toString(9)
  }
}
