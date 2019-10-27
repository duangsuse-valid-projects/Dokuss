package org.duangsuse.dokuss.struct

import org.duangsuse.dokuss.bytes.Cnt
import org.duangsuse.dokuss.bytes.Idx
import org.duangsuse.dokuss.intf.Reader
import org.duangsuse.dokuss.intf.Writer
import kotlin.reflect.KProperty

/** Read/write binary struct */
interface Struct<T> {
  fun read(r: Reader): T
  fun write(w: Writer, data: T)
  val size: Cnt?
}

/** Use it for `enum class` declaration */
interface Numbered { val index: Idx }

/** Type constraint is not working on this abstraction... since [Array] requires reified type parameters */
abstract class ArrayLike(val dims: Cnt) {
  private val xs = Array<Any>(dims) {Unit}

  operator fun get(idx: Idx) = xs[idx]
  operator fun set(idx: Idx, value: Any) { xs[idx] = value }

  data class index<T: Any>(val idx: Idx) {
    @Suppress("UNCHECKED_CAST")
    operator fun getValue(xs: ArrayLike, _p: KProperty<*>) = xs[idx] as T
    operator fun setValue(xs: ArrayLike, _p: KProperty<*>, v: T) { xs[idx] = v }
  }

  operator fun component1() = xs[0]
  operator fun component2() = xs[1]
  operator fun component3() = xs[2]
  operator fun component4() = xs[3]
  operator fun component5() = xs[4]
  operator fun component6() = xs[5]
  override fun toString(): String = toString(7)
  fun toString(viewport: Cnt): String = "Array+(${xs.joinToString("|", limit = viewport)})"
}
