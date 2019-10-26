package org.duangsuse.dokuss

import org.duangsuse.dokuss.bytes.*
import org.duangsuse.dokuss.intf.Writer
import java.io.*

/** A stream writer class with byte-order extension */
class Writer(s: OutputStream): FilterOutputStream(s), Writer {
  private val dd: DataOutput = DataOutputStream(this)
  override var byteOrder: ByteOrder = ByteOrder.jvm

  override fun writeAllFrom(src: Buffer) = write(src)
  override fun writeFrom(src: Buffer, cnt: Cnt, pos: Idx) = write(src, pos, cnt)

  private inline fun <reified T: Number> swept(crossinline write_f: DataOutput.(T) -> Unit, n: T) = if (shouldSwap)
    dd.write_f(ByteOrder.PrimSwapper.swap(n) as T) else dd.write_f(n)
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
}
