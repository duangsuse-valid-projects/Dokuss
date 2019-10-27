package org.duangsuse.dokuss

import org.duangsuse.dokuss.bytes.*
import org.duangsuse.dokuss.intf.Reader
import java.lang.reflect.Method

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

inline fun <reified EXCEPT, R> Try(exceptional: () -> R,
    err: (EXCEPT) -> R? = {null}): R?
  where EXCEPT: Throwable
  = try { exceptional() }
  catch (e: Throwable) {
    if (e is EXCEPT) err(e)
    else throw e }

fun Class<*>.tryForName(name: String, init: Boolean = true) = Try<ClassNotFoundException,
  Class<*>>({ Class.forName(name, init, this.classLoader) })
sealed class MethodOf(val name: String, val paramTypes: Array<out Class<*>>) {
  class Fn(name: String, vararg ts: Class<*>): MethodOf(name, ts)
  class Unbound(name: String, vararg ts: Class<*>): MethodOf(name, ts)
}
operator fun Class<*>.get(method: MethodOf.Fn): Method? = getDeclaredMethod(method.name, *method.paramTypes)
operator fun Class<*>.get(method: MethodOf.Unbound): Method? = getMethod(method.name, *method.paramTypes)
fun Method.invokeStatic(vararg args: Any?) = invoke(null, *args)
fun Method.access() = this.also { isAccessible = true }
fun <R> Method.bound(receiver: Any?) = @Suppress("UNCHECKED_CAST")
fun (args: Array<Any>): R { return this@bound.invoke(receiver, *args) as R }
