package org.duangsuse.dokuss.struct

import org.duangsuse.dokuss.bytes.ByteOrder
import org.duangsuse.dokuss.intf.Reader
import org.duangsuse.dokuss.intf.Writer

// TODO: Why not use `Compose<T>(r: Reader.() -> Unit, w: Writer.() -> Unit)`?
object Endian {
  class SerLE<T>(val ser: Struct<T>): Struct<T> by ser {
    override fun read(r: Reader): T { r.byteOrder = ByteOrder.LittleEndian; return ser.read(r) }
    override fun write(w: Writer, data: T) { w.byteOrder = ByteOrder.LittleEndian; return ser.write(w, data) }
  }
  class SerBE<T>(val ser: Struct<T>): Struct<T> by ser {
    override fun read(r: Reader): T { r.byteOrder = ByteOrder.BigEndian; return ser.read(r) }
    override fun write(w: Writer, data: T) { w.byteOrder = ByteOrder.BigEndian; return ser.write(w, data) }
  }
}
