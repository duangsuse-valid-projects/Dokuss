package org.duangsuse.dokuss.bytes

/** Nonzero counter */
typealias Cnt = Int
typealias LongCnt = Long
typealias LongZCnt = Long
/** Zero-able counter [Cnt] */
typealias ZCnt = Int
/** Array-like index */
typealias Idx = Int
typealias LongIdx = Long
/** Read-write buffer type */
typealias Buffer = Int8Array

/** Natural numbers 8/16 (UnsignedByte/UnsignedShort)
 * is not currently supported, so typealias is used */
typealias Nat8 = Int
/** [Nat8] */
typealias Nat16 = Int

typealias Int8 = Byte
typealias Int16 = Short
typealias Int32 = Int
typealias Int64 = Long
typealias Int8Array = ByteArray
typealias Int16Array = ShortArray
typealias Int32Array = IntArray
typealias Int64Array = LongArray

/** Rational numbers (IEE754 floating point) */
typealias Rat32 = Float
/** [Rat32] */
typealias Rat64 = Double
typealias Rat32Array = FloatArray
typealias Rat64Array = DoubleArray

typealias Char16 = Char
typealias Char16Array = CharArray
