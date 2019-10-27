package org.duangsuse.dokuss.struct

import org.duangsuse.dokuss.bytes.Cnt
import org.duangsuse.dokuss.intf.Reader
import org.duangsuse.dokuss.intf.Writer

/**
 * Takes a function returns [ArrayLike] object allocation,
 *  parse child struct, store them.
 */
class Seq<T: ArrayLike, E>(private val st: () -> T, private val children: Iterable<Struct<E>>): Struct<T> {
  constructor(st: () -> T, vararg children: Struct<E>): this(st, children.asIterable())

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
      writers[xi](w, data[xi] as E)
    }
  }

  override val size: Cnt? = children.takeIf { it.all { x -> x.size is Any } }?.sumBy { it.size!! }
}
