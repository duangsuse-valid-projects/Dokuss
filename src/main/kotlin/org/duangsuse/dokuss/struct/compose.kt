package org.duangsuse.dokuss.struct

import org.duangsuse.dokuss.bytes.Action
import org.duangsuse.dokuss.bytes.ByteOrder
import org.duangsuse.dokuss.bytes.Cnt
import org.duangsuse.dokuss.intf.Reader
import org.duangsuse.dokuss.intf.Writer

abstract class Compose<T>(val st: Struct<T>): Struct<T> {
  override fun read(r: Reader): T {
    beforeRead(r)
    return st.read(r)
  }

  override fun write(w: Writer, data: T) {
    beforeWrite(w)
    st.write(w, data)
  }
  override val size: Cnt? get() = st.size
  abstract fun beforeRead(r: Reader)
  abstract fun beforeWrite(w: Writer)

  inner class Fn(val on_r: Action<Reader>, val on_w: Action<Writer>, st: Struct<T>): Compose<T>(st) {
    override fun beforeRead(r: Reader) = r.on_r()
    override fun beforeWrite(w: Writer) = w.on_w()
  }
}

object Endian {
  class SerLE<T>(val ser: Struct<T>): Compose<T>(ser) {
    override fun beforeRead(r: Reader) { r.byteOrder = ByteOrder.LittleEndian }
    override fun beforeWrite(w: Writer) { w.byteOrder = ByteOrder.LittleEndian }
  }
  class SerBE<T>(val ser: Struct<T>): Compose<T>(ser) {
    override fun beforeRead(r: Reader) { r.byteOrder = ByteOrder.BigEndian }
    override fun beforeWrite(w: Writer) { w.byteOrder = ByteOrder.BigEndian }
  }
}
