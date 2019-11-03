package org.duangsuse.dokuss

import org.duangsuse.dokuss.bytes.*
import org.duangsuse.dokuss.intf.Writer
import java.io.*

/** A stream writer class with byte-order extension */
open class Writer(private val s: OutputStream): Writer, Flushable by s, Closeable by s {
  private val dd: DataOutput = DataOutputStream(s)
  override var byteOrder: ByteOrder = ByteOrder.jvm

  override fun writeAllFrom(src: Buffer) = s.write(src)
  override fun writeFrom(src: Buffer, cnt: Cnt, pos: Idx) = s.write(src, pos, cnt)

  private inline fun <reified T: Number> swept(crossinline write_f: DataOutput.(T) -> Unit, n: T) = if (isJvmOrder)
    dd.write_f(n) else dd.write_f(ByteOrder.PrimSwapper.swap(n) as T)
  override fun writeInt8(i: Int8) = swept(DataOutput::writeByte, i.toInt())
  override fun writeInt16(i: Int16) = swept(DataOutput::writeShort, i.toInt())
  override fun writeChar16(c: Char16) = swept(DataOutput::writeChar, c.toInt())
  override fun writeInt32(i: Int32) = swept(DataOutput::writeInt, i)
  override fun writeInt64(i: Int64) = swept(DataOutput::writeLong, i)
  override fun writeRat32(r: Rat32) = swept(DataOutput::writeFloat, r)
  override fun writeRat64(r: Rat64) = swept(DataOutput::writeDouble, r)

  override fun writeString(str: String, kind: Writer.StringReprFmt)
    = when (kind) {
        Writer.StringReprFmt.Bytes -> dd::writeBytes
        Writer.StringReprFmt.Chars -> dd::writeChars
        Writer.StringReprFmt.UTF -> dd::writeUTF
    } (str)

  override fun toString(): String = "Writer($s)"

  class File(val file: java.io.File): org.duangsuse.dokuss.Writer
  (file.also { check(it.canWrite()) {"File $file can't be read"} }.outputStream()) {
    constructor(path: String): this(java.io.File(path))
    override fun toString(): String = "Writer.File($file)"
  }
  class ofBuffer(val buffer: ByteArrayOutputStream): org.duangsuse.dokuss.Writer(buffer) {
    constructor(size: Cnt): this(ByteArrayOutputStream(size))
    override fun toString(): String = "Writer.Buffer($buffer)"
  }
}
