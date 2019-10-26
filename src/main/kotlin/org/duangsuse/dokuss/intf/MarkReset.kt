package org.duangsuse.dokuss.intf

import java.io.Closeable

/** A class with the capacity of restoring its state, specialized for sequence input */
interface MarkReset<D> {
  val isMarking: Boolean
  fun mark(rl: D) fun reset()

  /** Run the operation [op] with current context, and reset state after its called */
  fun <R> positional(rl: D, op: () -> R): R {
    mark(rl); try { return op() } finally { reset() }
  }
  /** Java try-with-resource version of [positional] */
  fun positionalTask(rl: D) = Closeable { this@MarkReset.reset() }.also { mark(rl) }
}
