package org.duangsuse.dokuss.sized

/** WTF thing that [Number] has bound */
interface Bounded<T: Number>: Sized<T> {
  val min: T
  val max: T
  override val size: /*succ*/ T
  fun <R> range(to: (T, T) -> R) = to(min, max)
}

object Int32: Bounded<Int> {
  override val min = Int.MIN_VALUE
  override val max = Int.MAX_VALUE
  override val size = max // (max.toLong() - min.toLong()).toInt()
}
object Int64: Bounded<Long> {
  override val min = Long.MIN_VALUE
  override val max = Long.MAX_VALUE
  override val size = max // (max.toFloat() - min.toFloat()).toLong()
}

object Rat32: Bounded<Float> {
  override val min = Float.MIN_VALUE
  override val max = Float.MAX_VALUE
  override val size = max // (max.toDouble() - min.toDouble()).toFloat()
}
object Rat64: Bounded<Double> {
  override val min = Double.MIN_VALUE
  override val max = Double.MAX_VALUE
  override val size = max // max - min
}
