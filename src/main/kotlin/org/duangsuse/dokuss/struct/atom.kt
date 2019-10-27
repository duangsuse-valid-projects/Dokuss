package org.duangsuse.dokuss.struct

import org.duangsuse.dokuss.bytes.*
import org.duangsuse.dokuss.intf.Reader
import org.duangsuse.dokuss.intf.Writer

object int8: Struct<Int8> {
  override fun read(r: Reader): Int8 = r.readInt8()
  override fun write(w: Writer, data: Int8) = w.writeInt8(data)
  override val size: Cnt = Byte.SIZE_BYTES
}
object int16: Struct<Int16> {
  override fun read(r: Reader): Int16 = r.readInt16()
  override fun write(w: Writer, data: Int16) = w.writeInt16(data)
  override val size: Cnt = Short.SIZE_BYTES
}
object char16: Struct<Char16> {
  override fun read(r: Reader): Char16 = r.readChar16()
  override fun write(w: Writer, data: Char16) = w.writeChar16(data)
  override val size: Cnt = Char.SIZE_BYTES
}
object int32: Struct<Int32> {
  override fun read(r: Reader): Int32 = r.readInt32()
  override fun write(w: Writer, data: Int32) = w.writeInt32(data)
  override val size: Cnt = Int.SIZE_BYTES
}
object int64: Struct<Int64> {
  override fun read(r: Reader): Int64 = r.readInt64()
  override fun write(w: Writer, data: Int64) = w.writeInt64(data)
  override val size: Cnt = Long.SIZE_BYTES
}

object rat32: Struct<Rat32> {
  override fun read(r: Reader): Rat32 = r.readRat32()
  override fun write(w: Writer, data: Rat32) = w.writeRat32(data)
  override val size: Cnt = Int.SIZE_BYTES
}
object rat64: Struct<Rat64> {
  override fun read(r: Reader): Rat64 = r.readRat64()
  override fun write(w: Writer, data: Rat64) = w.writeRat64(data)
  override val size: Cnt = Long.SIZE_BYTES
}

object nat8: Struct<Nat8> {
  override fun read(r: Reader): Nat8 = r.readNat8()
  override fun write(w: Writer, data: Nat8) = TODO("Unsigned write")
  override val size: Cnt = Byte.SIZE_BYTES
}
object nat16: Struct<Nat16> {
  override fun read(r: Reader): Nat16 = r.readNat16()
  override fun write(w: Writer, data: Nat16) = TODO("Unsigned write")
  override val size: Cnt = Short.SIZE_BYTES
}

object utf: Struct<String> {
  override fun read(r: Reader): String = r.readStringUTF()
  override fun write(w: Writer, data: String) = w.writeString(data)
  override val size: Cnt? = null
}
