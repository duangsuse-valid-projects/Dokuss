package org.duangsuse.dokuss.sized

import java.util.*

typealias Ratio = Double

/** Any type that provides a `size` */
interface Sized<out S: Number> { val size: S }

/** Lazily evaluated lists */
interface SizedIterable<out T>: Iterable<T>, Sized<Int> {
  class Instance<T>(private val iter: Iterable<T>, override val size: Int):
    SizedIterable<T>, Iterable<T> by iter {
    constructor(s: Collection<T>): this(s.asIterable(), s.size)
    companion object Factory {
      operator fun <N: Number> invoke(b: Bounded<N>, range: (N, N) -> Iterable<N>)
        = Instance(b.range(range), (b.size as Number).toInt())
      operator fun <T> invoke(xs: Iterable<T>) = Instance(xs.toList())
    }
  }
}

val Ratio.toPercent get() = this * 100.0
val Ratio.percent get() = this / 100.0

/** Solve what [IntRange.step] should be use when expecting result size ratio at [ratio] */
fun <T> SizedIterable<T>.ratioSkip(ratio: Ratio): Int {
  check(ratio <= 1.0) { "Percentage ratio have to lessOrEqual 100% (${ratio.toPercent})" }
  return (size / (size * ratio)).toInt()
}

/** Note: this function is different from [IntRange.random], it's right-exclusive */
fun random(r: IntRange): Int {
  val rand = Random()
  return r.start + rand.nextInt() % (Math.max(1, r.last))
}

/**
 * 类似这种程序就很需要形式化验证，尤其是对数学不好的我
 *
 * + [limit]: Limit of [Iterator.next] calls
 * + [k]: Random skip range, right exclusive
 */
fun <T> SizedIterable<T>.sample(limit: Int, k: IntRange = 0..3) = object: Iterable<T> {
  override fun iterator() = object: Iterator<T> {
    val iterator = this@sample.iterator()
    var rest = limit
    override fun hasNext(): Boolean = rest >0
    override fun next(): T {
      for (_t in 0 until random(k)) {
        if (iterator.hasNext()) iterator.next()
        else throw Error("Bad contract $limit, still have $rest more elements to read")
      }
      return iterator.next().also { --rest }
    }
  }
}.also { size.div(k.last+1).let { align ->
  check (limit <= align) { "Limit too large ($limit/$size, $align)" } } }

/** Slice this iterable with size ratio of [ratio] */
fun <T> SizedIterable<T>.ratioSlice(ratio: Ratio): Iterable<T>
  = ratioSkip(ratio).let { this.filterIndexed { i, _ -> i % it == 0 } }

fun <T> SizedIterable<T>.ratioSample(ratio: Ratio, skipRatio: Ratio = 100.0.percent): Iterable<T>
  = sample(size.times(ratio).toInt(), 0..skipRatio.times(ratioSkip(ratio)).toInt())

