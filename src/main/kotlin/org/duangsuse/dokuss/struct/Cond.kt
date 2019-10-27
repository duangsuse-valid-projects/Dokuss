package org.duangsuse.dokuss.struct

import org.duangsuse.dokuss.intf.Reader
import org.duangsuse.dokuss.intf.Writer

/** Give me a predicate, I can do conditional read/write */
open class Cond<T>(val substruct: Struct<T>, val pred: Reader.() -> Boolean): Struct<T> by substruct {
  override fun read(r: Reader): T = if (r.pred()) substruct.read(r) else onIgnoreRead()
  override fun write(w: Writer, data: T) =
    if (shouldWrite(data)) substruct.write(w, data)
    else onIgnoreWrite()
  open fun shouldWrite(data: T): Boolean = true
  open fun onIgnoreWrite() {}
  open fun onIgnoreRead(): Nothing = throw Error()
}
