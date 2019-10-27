package org.duangsuse.dokuss

import org.duangsuse.dokuss.bytes.Cnt
import org.duangsuse.dokuss.intf.MarkReset
import org.junit.Assert.*
import org.junit.Test

/** [MarkReset] */
class MarkResetAbstra {
  var x = 0
  val inst = object: MarkReset<Int> {
    override var isMarking: Boolean = false
      private set
    override fun mark(rl: Cnt) { isMarking = true; x += rl; ++x }
    override fun reset() { isMarking = false; --x }
  }

  /**
   * 1. In `op` function beginning, x=(x+1)
   * 2. When `reset()` is called, `x` is like what we updated `x` without `positional ()`
   * 3. rl is passed correctly
   *
   * [MarkReset.positional], [MarkReset.positionalTask]
   */
  @Test fun functional() {
    inst.positional(0) {
      assertEquals(1, x)
      x += 9
    }
    assertEquals(9, x)

    val t = inst.positionalTask(0)
      assertEquals(10, x)
      x += 1
    t.close()
    assertEquals(10, x)

    inst.positional(99) { x -= 99 }
    assertEquals(10, x)
  }
  /** [MarkReset.positional] */
  @Test fun exceptionalSafe() {
    x = 0; try {
      inst.positional<Nothing>(3) { throw Exception() }
    } catch (e: Exception) {}
    assertEquals(3, x)
  }
}
