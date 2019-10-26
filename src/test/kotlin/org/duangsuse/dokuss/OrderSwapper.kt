package org.duangsuse.dokuss

import org.duangsuse.dokuss.check.QuickCheck
import org.duangsuse.dokuss.bytes.ByteOrder.PrimSwapper
import org.duangsuse.dokuss.sized.*
import org.junit.Test

/** Test for class [PrimSwapper] ([org.duangsuse.dokuss.intf.PrimSwapper]) */
fun <N> revRevEq(swap: (N) -> N) = object: QuickCheck.Property<N>
  { override fun validate(input: N): Boolean = swap(swap(input)) == input }

class ShortRevRev: QuickCheck.Verify<Short>
  (revRevEq(PrimSwapper::swap), (Short.MIN_VALUE..Short.MAX_VALUE).map(Int::toShort), 0.2) {@Test fun r()=check()}

class CharRevRev: QuickCheck.Verify<Char>
  (revRevEq(PrimSwapper::swap), (Char.MIN_VALUE..Char.MAX_VALUE), 0.2) {@Test fun r()=check()}

abstract class BoundRevRev(xs: Iterable<Number>, ratio: Double, private val skip: Double):
  QuickCheck.Verify<Number>(revRevEq(PrimSwapper::swap), xs, ratio) {
  override fun pick(inputs: SizedIterable<Number>, ratio: Ratio): Iterable<Number> = inputs.ratioSample(ratio, skip)
}

// 精度耗时跪了，多态多事，草泥马
/**
 * const val base = 0.1e-17
 * class IntRevRev: BoundRevRev(Int32.range(::IntRange), base*2e10, 9e4) {@Test fun r()=check()}
 * class LongRevRev: BoundRevRev(Int64.range(::LongRange), base*4, 9e40)
 * class FloatRevRev: BoundRevRev(Rat32.range { a, b -> a.toInt()..b.toInt() }.map(Number::toFloat), base*base, 9e4)
 * class DoubleRevRev: BoundRevRev(Rat64.range { a, b -> a.toLong()..b.toLong() }.map(Number::toDouble), base*base*0.1e-270, 9e40)
 */
// 你们它娘的都是历史了，沃日

class NumRevRev {
  private inline fun <N> revRevId(crossinline swp: (N) -> N) where N: Number
    = { x: N -> swp(swp(x)) == x }
  val xs = arrayOf(1, 2, (-1), 0, 9, -1002, -1001)
  @Test fun int8() = QuickCheck.prop(xs.map(Int::toByte), f=revRevId(PrimSwapper::swap)).check()
  @Test fun int16() = QuickCheck.prop(xs.map(Int::toShort), f=revRevId(PrimSwapper::swap)).check()
  @Test fun char16() = QuickCheck.prop(listOf('a', 'b', '箪', '_')) { PrimSwapper.swap(PrimSwapper.swap(it)) == it }.check()
  @Test fun int32() = QuickCheck.prop(xs.toList(), f=revRevId(PrimSwapper::swap)).check()
  @Test fun int64() = QuickCheck.prop(xs.map(Int::toLong), f=revRevId(PrimSwapper::swap)).check()
  @Test fun rat32() = QuickCheck.prop(xs.map(Int::toFloat), listOf(0.3F, 0.9F, 0.1e-17F), f=revRevId(PrimSwapper::swap)).check()
  @Test fun rat64() = QuickCheck.prop(xs.map(Int::toDouble), listOf(0.3, 0.9, 0.1e-17), f=revRevId(PrimSwapper::swap)).check()
}
