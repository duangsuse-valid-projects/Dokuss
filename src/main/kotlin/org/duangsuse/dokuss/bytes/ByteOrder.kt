package org.duangsuse.dokuss.bytes

import java.nio.ByteOrder.*

/**
 * An enumeration for [BigEndian] BE(0x00FF), [LittleEndian] LE(0xFF00) machine-word byte representation order
 *
 * + [Detect]: Value [Detect.system] and [Detect.jvm] (always BigEndian, `0xFF.shr(8) == 0`), initialized on-load
 * + [ByteOrder.ed]: An interface of types that has byte-order [ed.isJvmOrder]
 * + [Swapper]: Helper class for byte-sequence rotating
 * + [PrimSwapper]: Instance of [Swapper] but can rotate [Float], [Double], [Char] using coercion methods
 */
enum class ByteOrder {
  BigEndian, LittleEndian;

  companion object Detect {
    /**
     * Calculates system byte-order by shift overflowing:
     *
     * ```kotlin
     * private fun bitFFOverflow() = 0xFF.shr(8).shl(8)
     * val system: ByteOrder = if (bitFFOverflow() == 0xFF) LittleEndian else BigEndian
     * ```
     *
     * Byte-order __is about memory layout__, not operation shr/shl, or/and implementation,
     *  and JVM hides platform dependent details like those operation.
     *
     * ```c
     * typedef enum {BE, LE} ByteOrd;
     * ByteOrd nativeEndian() {
     *   union {int i; char bs[sizeof(int)];} repr = {0xFF};
     *   return repr.bs[0] == (char)0xFF? LE : BE;
     * }
     * ```
     *
     * Native byte-order for Java __and even JVM__ is _strictly_ [BigEndian], but for interact it can be determined by native using [nativeOrder].
     */
    val system: ByteOrder = ByteOrder.fromJava(nativeOrder())
    /** The byte-order for whole JVM primitive reader */
    val jvm: ByteOrder = BigEndian
    @JvmStatic fun fromJava(ord: java.nio.ByteOrder) = when (ord)
      { LITTLE_ENDIAN -> LittleEndian; BIG_ENDIAN -> BigEndian; else -> throw Error() }
  }

  interface ed {
    /** Byte order used for this input stream */
    var byteOrder: ByteOrder
    /** The child-class should swap output byte-order when exporting non-native-endian data */
    val isJvmOrder get() = byteOrder == ByteOrder.jvm
    val isNativeOrder get() = byteOrder == ByteOrder.system }

  /** Byte-order rotate helper class [rotatePrimOrd] */
  abstract class Swapper {
    /**
     * Helper function constructs a function `(I) -> I` rotating [LittleEndian] to [BigEndian] (and back)
     *
     * Function is defined `crossinline` to reduce the abstraction cost.
     *  [shr], [shl] must be accumulative `∀i a b. (i op a) op b = i op (a + b)`
     *  ensuring `a.shl(8).or(i0).shl(8).or(i1)` equivalent to `a.shl(8+8).or(i0).or(a.shl(8).or(i1))`.
     *
     * + [shr], [and]: right-populates a byte by `byte = i.and(0xFF)` `i.shr(8)`
     * + [shl], [or]: right-appends a byte by `i.shl(8).or(byte)`
     * + [byte_width]: count of the bytes in [I] instance, satisfies `(x as I).shr(byte_width * 8) == 0x0`
     *  (LE, truncate), if lessThan `T.SIZE_BYTES`, the only [byte_width] bytes(right) of this integral is swapped
     */
    protected inline fun <I> rotatePrimOrd(
      crossinline shl: I.(Cnt) -> I, crossinline or: I.(I) -> I,  // construct I
      crossinline shr: I.(Cnt) -> I, crossinline and: I.(I) -> I, // destruct I
      byte_select: I, byte_width: Cnt): (I) -> I = rotate@{
      var stack = it   // original
      var swapped = it // result
      for (_t in 1..byte_width) {
        val shifted = stack.and(byte_select)
        stack = stack.shr(WByte) // |00 [FF]<< (00) FF|
        swapped = swapped.shl(WByte).or(shifted) // |00 00 FF (00)<<|
      }
      return@rotate swapped
    }
    companion object Constants {
      const val WByte = Byte.SIZE_BITS
    }
  }

  /**
   * Numeric swappers satisfies: `∀i. swap(swap(i)) = i` __if no overflow occurs__
   * @see org.duangsuse.dokuss.intf.PrimSwapper */
  object PrimSwapper : Swapper(), org.duangsuse.dokuss.intf.PrimSwapper {
    private fun rotateIntegral32Ord(byte_count: Cnt)
      = rotatePrimOrd(Int::shl, Int::or, Int::ushr, Int::and, 0xFF, byte_count)
    private fun rotateIntegral64Ord(byte_count: Cnt)
      = rotatePrimOrd(Long::shl, Long::or, Long::ushr, Long::and, 0xFFL, byte_count)
    /** Identity (input-as-output) to fit `swap(Number)` */
    override fun swap(i: Int8) = i
    /**
     * This swapper uses [Short.toInt] coercion, but int swap can lead to result like:
     *
     * + 0xAAFF (0x0000_AAFF) => 0xFFAA_0000, toShort() = 0x0000, truth = 0xFFAA
     * + 0xFFAA_0000 (not valid [Short]) => 0x0000_AAFF, toShort() = 0xAAFF, truth = 0xAAFF
     *
     * It can be resolved using conditional branch:
     *
     * ```kotlin
     * val irev = swap(i.toInt())
     * val atright = irev.and(0xFFFF) != 0
     * return if (!atright) irev.shr(Int.SIZE_BITS/2).toShort() else irev.toShort()
     * ```
     *
     * But swaps the __only 2 byte__ from [Int] type is more efficient.
     *
     * Using [Int.ushr] is suitable for this operation ([Swapper] copies the sign bit of integral)
     */
    override fun swap(i: Int16): Short = rotateIntegral32Ord(Short.SIZE_BYTES)(i.toInt()).toShort()
    override fun swap(c: Char16) = swap(c.toShort()).toChar()
    override fun swap(i: Int32) = rotateIntegral32Ord(Int.SIZE_BYTES)(i)
    override fun swap(i: Int64) = rotateIntegral64Ord(Long.SIZE_BYTES)(i)
    override fun swap(r: Rat32) = Float.fromBits(swap(r.toBits()))
    override fun swap(r: Rat64) = Double.fromBits(swap(r.toBits()))
    /** Polymorphic helper for [swap] functions */
    override fun swap(n: Number): Number = when (n) {
      is Int8 -> swap(n)
      is Int16 -> swap(n)
      is Int32 -> swap(n)
      is Int64 -> swap(n)
      is Rat32 -> swap(n)
      is Rat64 -> swap(n)
      else -> throw Error()
    }
  }
}
