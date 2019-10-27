package org.duangsuse.dokuss

import org.duangsuse.dokuss.bytes.*
import org.duangsuse.dokuss.intf.Reader

fun Reader.readManyByte(n: Cnt): Buffer = ByteArray(n).also { readTo(it, n, 0) }
fun Reader.readAll(): Buffer = readManyByte(estimate)

inline fun <reified T> Reader.readMany(initial: T, crossinline itemReader: Reader.() -> T):
  (Cnt) -> Array<T> = readxx@{ size ->
  val resbuf = Array(size) {initial}
  0.until(size).forEach { resbuf[it] = this.itemReader() }
  return@readxx resbuf
}
fun Reader.readInt8s(n: Cnt)   = readMany(0,  Reader::readInt8)(n)
fun Reader.readInt16s(n: Cnt)  = readMany(0,  Reader::readInt16)(n)
fun Reader.readChar16s(n: Cnt) = readMany('?', Reader::readChar16)(n)
fun Reader.readInt32s(n: Cnt)  = readMany(0,  Reader::readInt32)(n)
fun Reader.readInt64s(n: Cnt)  = readMany(0L, Reader::readInt64)(n)

fun Reader.readRat32s(n: Cnt) = readMany(0.0f, Reader::readRat32)(n)
fun Reader.readRat64s(n: Cnt) = readMany(0.0, Reader::readRat64)(n)

fun Reader.readNat8s(n: Cnt)  = readMany(0, Reader::readNat8)(n)
fun Reader.readNat16s(n: Cnt) = readMany(0, Reader::readNat16)(n)

fun Reader.alignToNext(alignment: LongCnt)
  { seek(alignment - (position%alignment)) }

/** 哪个大佬给写 Coroutine async/await 的版本 */
fun Reader.waitForEstimate(n: ZCnt, op: Runnable = Runnable{}) {
  while (estimate != n) { op.run() }
  return
}

fun Reader.nativeEndian() { byteOrder = ByteOrder.system }
fun Reader.jvmEndian() { byteOrder = ByteOrder.jvm }

fun Int.toHexString() = toString(16)
