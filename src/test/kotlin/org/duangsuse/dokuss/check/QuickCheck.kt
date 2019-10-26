package org.duangsuse.dokuss.check

import org.duangsuse.dokuss.sized.*
import org.junit.Test

/** Simple test abstraction like what defined in Haskell, should be rewritten since [Iterable] is used badly */
abstract class QuickCheck<T: Any> protected constructor(protected val prop: Property<T>) {
  @FunctionalInterface interface Property<in T> { fun validate(input: T): Boolean }
  /** Try annotate it with [Test] in subclass, since JUnit 5 searches for constructor in classes' base class */
  abstract fun check()
  open fun pick(inputs: SizedIterable<T>, ratio: Ratio) = inputs.ratioSlice(ratio)

  /** Inputs can be `Sized`, but Kotlin does not have this type built-in, btw. */
  operator fun invoke(inputs: Iterable<T>) = invoke(SizedIterable.Instance(inputs))
  operator fun invoke(inputs: SizedIterable<T>, sample: Ratio = 1.0)
    = pick(inputs, sample).map(prop::validate).zip(inputs)

  /** Sadly, looks like Kotlin won't find inner classes for inheritance */
  open /*inner*/ class Verify<T: Any> protected constructor(prop: Property<T>,
      private val inputs: Iterable<T>, private val ratio: Ratio): QuickCheck<T>(prop) {
    operator fun invoke(sample: Double) = super.invoke(SizedIterable.Instance(inputs), sample)
    operator fun invoke() = invoke(ratio)
    override fun check() {
      val reports = invoke()
      for (report in reports.withIndex()) {
        val (ok, x) = report.value
        if (!ok) throw AssertionError("Failed at test $prop($x) after ${report.index} tests ($ratio).")
      }
      println("${reports.size} (rate $ratio) tests passed.")
    }
  }

  companion object Helper {
    var sampler: (SizedIterable<*>, Ratio) -> Iterable<*> = { s, r -> s.ratioSample(r, 94.0.percent) }
    /** prop *inputs, \[sample\], \[sampler\], f */
    @Suppress("UNCHECKED_CAST")
    fun <T: Any> prop(vararg inputs_s: Collection<T>, sample: Ratio = 100.0.percent, f: (T) -> Boolean)
      = object: QuickCheck<T>(object: Property<T> { override fun validate(input: T): Boolean = f(input) }) {
      override fun check() = inputs_s.forEach { inputs -> invoke(SizedIterable.Instance(inputs.toList()), sample).forEachIndexed { i, record ->
        val (ok, input) = record
        if (!ok) throw AssertionError("Failed after $i tests: $prop($input)")
      } }
      override fun pick(inputs: SizedIterable<T>, ratio: Ratio): Iterable<T> = sampler(inputs, ratio) as Iterable<T>
    }
  }
}
