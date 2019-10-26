package org.duangsuse.dokuss.intf

import org.duangsuse.dokuss.bytes.ByteOrder.Swapper
import org.duangsuse.dokuss.bytes.*

/** An object instance of [Swapper], with the capacity of byte-swap all (Int8-Rat64) numeric instances */
interface PrimSwapper {
  /** Rotates byte sequence of integral/real, for all type input `T`, swap always returns `T` */
  fun swap(n: Number): Number
  /** Rotates a `char` by simply casting from/to `short` */
  fun swap(c: Char16): Char16

  fun swap(i: Int8): Int8   fun swap(i: Int16): Int16
  fun swap(i: Int32): Int32 fun swap(i: Int64): Int64
  fun swap(r: Rat32): Rat32 fun swap(r: Rat64): Rat64
}
