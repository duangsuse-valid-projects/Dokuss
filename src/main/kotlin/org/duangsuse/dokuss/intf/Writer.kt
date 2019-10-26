package org.duangsuse.dokuss.intf

import org.duangsuse.dokuss.bytes.*
import java.io.Closeable
import java.io.Flushable

interface Writer: ByteOrder.ed, Flushable, Closeable {
  fun writeAllFrom(src: Buffer)
  fun writeFrom(src: Buffer, cnt: Cnt, pos: Idx)

  fun writeInt8(i: Int8)
  fun writeInt16(i: Int16)
  fun writeChar16(c: Char16)
  fun writeInt32(i: Int32)
  fun writeInt64(i: Int64)
  fun writeRat32(r: Rat32)
  fun writeRat64(r: Rat64)

  enum class StringReprFmt { Bytes, Chars, UTF }
  fun writeString(str: String, kind: StringReprFmt = StringReprFmt.UTF)
}
