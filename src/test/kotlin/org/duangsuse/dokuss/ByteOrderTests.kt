package org.duangsuse.dokuss

import org.duangsuse.dokuss.bytes.ByteOrder
import org.duangsuse.dokuss.bytes.ByteOrder.*
import org.junit.Test
import org.junit.Assert.*

/** @see ByteOrder */
class ByteOrderTests {
  @Test
  /** [Detect.system], [Detect.fromJava] */
  fun detects() {
    assertEquals(LittleEndian, Detect.system)
  }

  @Test
  /** [ed.shouldSwap] */
  fun swapPredicate() {
    var order = Detect.system
    val readinst = object: ed {
      override var byteOrder: ByteOrder
        get() = order
        set(v) { order = v }
    }
    assertEquals(true, readinst.shouldSwap)
    order = Detect.jvm
    assertEquals(false, readinst.shouldSwap)
  }

  @Test
  /** [Swapper], [PrimSwapper] */
  fun someOrdSwapTests() {
    fun assertHexEquals(a: Long, n: Long) { assertEquals(a.toString(16), n.toString(16)) }
    fun assertHexEquals(a: Int, n: Int) { assertEquals(a.toString(16), n.toString(16)) }
    val r = PrimSwapper
    for (x in setOf(0xFF, 0x00, 0x0F).map(Int::toByte)) assertHexEquals(x.toInt(), r.swap(x).toInt())
    assertHexEquals(0x00FF, r.swap(0xFF00.toShort()).toInt())
    assertHexEquals('\u0061'.toInt(), r.swap('\u6100').toInt())
    assertHexEquals(0x7ABBCC7D, r.swap(0x7DCCBB7A))
    assertHexEquals(0x7ABBCCDDEEFF0011L, r.swap(0x1100FFEEDDCCBB7AL))
    assertHexEquals(0.2f.toBits(), r.swap(-428443584.0f).toBits())
    assertHexEquals(0.122.toBits(), r.swap(6.810929006278219E-267).toBits())
  }
}
