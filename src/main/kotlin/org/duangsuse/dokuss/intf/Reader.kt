package org.duangsuse.dokuss.intf

import org.duangsuse.dokuss.bytes.*
import java.io.Closeable
import java.io.DataInputStream
import java.io.InputStream

/** A wrapper class for [InputStream] and [DataInputStream], with byte-order extension. mark/reset is not reentrant */
interface Reader: ByteOrder.ed, MarkReset<Cnt>, Closeable {
  /** Estimate input data byte length */
  val estimate: ZCnt get
  /** Position (array next read actual, inclusive [estimate]) */
  val position: Idx get

  fun readAllTo(dst: Buffer)
  fun readTo(dst: Buffer, cnt: Cnt, idx: Idx)
  fun seek(n: LongCnt)

  fun readInt8(): Int8
  fun readInt16(): Int16
  fun readChar16(): Char16
  fun readInt32(): Int32
  fun readInt64(): Int64
  fun readRat32(): Rat32
  fun readRat64(): Rat64

  /** @see java.io.DataInput.readUnsignedByte */
  fun readNat8(): Nat8
  /** @see java.io.DataInput.readUnsignedShort */
  fun readNat16(): Nat16

  /** @see java.io.DataInput.readUTF */
  fun readStringUTF(): String
}
