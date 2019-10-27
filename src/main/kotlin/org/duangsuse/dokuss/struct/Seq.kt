package org.duangsuse.dokuss.struct

import org.duangsuse.dokuss.bytes.Cnt
import org.duangsuse.dokuss.intf.Reader
import org.duangsuse.dokuss.intf.Writer

/**
 * Takes a function returns [ArrayLike] object allocation,
 *  parse child struct, store them.
 */
class Seq<T: ArrayLike>(private val st: () -> T, private val children: Iterable<Struct<T>>): Struct<T> {
  override fun read(r: Reader): T {
    val struct = st()
    for (child in children.withIndex())
      { struct[child.index] = child.value.read(r) as Any }
    return struct
  }

  override fun write(w: Writer, data: T) {
    val writers = children.map { return@map it::write }
    for (xi in 0 until data.dims) {
      @Suppress("UNCHECKED_CAST")
      writers[xi](w, data[xi] as T)
    }
  }

  override val size: Cnt? = children.takeIf { it.all { x -> x.size is Any } }?.sumBy { it.size!! }
}
