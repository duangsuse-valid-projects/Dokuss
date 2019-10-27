package org.duangsuse.dokuss.struct

import org.duangsuse.dokuss.bytes.Cnt
import org.duangsuse.dokuss.intf.Reader
import org.duangsuse.dokuss.intf.Writer

/** Give me a struct, I can read it multiply times (for a number that can be easily determined runtime) */
class Many<T>(val substruct: Struct<T>, val read_count: Reader.() -> Cnt): Struct<Array<T>> {
  override fun read(r: Reader): Array<T> {
    val count = r.read_count()
    val ary = arrayOfNulls<Any?>(count)
    for (i in 0 until count)
      { ary[i] = substruct.read(r) }
    @Suppress("UNCHECKED_CAST")
    return ary as Array<T>
  }

  override fun write(w: Writer, data: Array<T>) {
    for (item in data)
      { substruct.write(w, item) }
  }

  override val size: Cnt? = null

  /** [Many] using reduce pattern */
  class Fold {}
}
