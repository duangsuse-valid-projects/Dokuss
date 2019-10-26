# <sub>Doku--</sub> [![platform]](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.io/index.html) [![version]](build.gradle) [![build]](https://travis-ci.org/duangsuse/Dokuss)
[platform]: https://img.shields.io/badge/kotlin-jvm--1.3-orange?style=flat-square&logo=kotlin
[version]: https://img.shields.io/badge/version-1.0-informational?style=flat-square
[build]: https://img.shields.io/travis/duangsuse-valid-projects/Dokuss?style=flat-square

> _Combinator style binary structure read/write library for Kotlin_

`org.duangsuse.dokuss:dokuss`
__Dokuss__, _ss_ means "Simple Stupid" __(WIP)__

## Basic interface

### ByteOrder.Swapper

```kotlin
// package org.duangsuse.dokuss.intf
// see ByteOrder.PrimSwapper
interface PrimSwapper {
  fun swap(n: Number): Number
  fun swap(c: Char16): Char16

  fun swap(i: Int8): Int8   fun swap(i: Int16): Int16
  fun swap(i: Int32): Int32 fun swap(i: Int64): Int64
  fun swap(r: Rat32): Rat32 fun swap(r: Rat64): Rat64
}
```

### Readers

```kotlin
// package org.duangsuse.dokuss
interface Reader: ByteOrder.ed, MarkReset<Cnt>, Closeable {
  val estimate: ZCnt get
  val position: Idx get

  fun readAllTo(dst: Buffer)
  fun readTo(dst: Buffer, cnt: Cnt, idx: Idx)
  fun seek(n: LongCnt)

  fun readInt8(): Int8
  // ...16, char16, 32, ...
  fun readRat64(): Rat64

  // readUnsignedByte
  fun readNat8(): Nat8
  fun readNat16(): Nat16

  // readUTF
  fun readStringUTF(): String
}
// val Reader.hasRemaining, Reader.remaining
```

### Writers

```kotlin
interface Writer: ByteOrder.ed, Flushable, Closeable {
  fun writeAllFrom(src: Buffer)
  fun writeFrom(src: Buffer, cnt: Cnt, pos: Idx)

  fun writeInt8(i: Int8)
  // ...16, char16, 32, ...
  fun writeRat64(r: Rat64)

  enum class StringReprFmt { Bytes, Chars, UTF }
  fun writeString(str: String, kind: StringReprFmt = StringReprFmt.UTF)
}
```

### Byte-Order of I/O stream

```kotlin
enum class ByteOrder {
  BigEndian, LittleEndian;

  companion object Detect {
    val system: ByteOrder = ByteOrder.fromJava(nativeOrder())
    val jvm: ByteOrder = BigEndian
  } }
```

```kotlin
// enum class org.duangsuse.dokuss.bytes.ByteOrder
interface ed {
  var byteOrder: ByteOrder
  val shouldSwap get() = byteOrder != ByteOrder.system }
```

### MarkReset

```kotlin
// <D> in Reader is Cnt(aka. Int)
interface MarkReset<D> {
  val isMarking: Boolean
  fun mark(rl: D) fun reset()

  fun <R> positional(rl: D, op: () -> R): R
  fun positionalTask(rl: D): Closeable // for Java try-with-resource
}
```

## Implementation detail

### Swapping byte-order

```kotlin
// abstract class org.duangsuse.dokuss.bytes.ByteOrder.Swapper
// This function is functional-inlined, low abstraction-overhead
protected inline fun <I> rotatePrimOrd(
  crossinline shl: I.(Cnt) -> I, crossinline or: I.(I) -> I,  // construct I
  crossinline shr: I.(Cnt) -> I, crossinline and: I.(I) -> I, // destruct I
  byte_select: I, byte_width: Cnt): (I) -> I
val swap: (Int) -> Int = rotatePrimOrd(
  Int::shl, Int::or,
  Int::ushr, Int::and,
  0xFF, byte_width = Int.SIZE_BYTES)
```

Example 0: given an integral `0xFF00FF00`
+ copy: `res`(empty by-value instance) = `it`
+ repeat `4` times:
  + set: `byte` = `it & 0xFF`, also `it >>= 8`
    + `(1) 0xFF00FF [00]`, `0xFF00FF[00]>>`
    + `(2) 0x??FF00 [FF]`, `0xFF00[FF]>>aa` *
    + `(3) 0x????FF [00]`, `0xFF[00]>>bbaa` |
    + `(4) 0x?????? [FF]`, `0x[FF]>>ccbbaa`
  + set: `res` = `res << 8 | byte`
    + `(1) 0x???????? << 8 | aa=0x00`
    + `(2) 0x??????00 << 8 | bb=0xFF` *
    + `(3) 0x????00FF << 8 | cc=0x00` |
    + ...
    
### `position` for `Reader` instances

```kotlin
override val estimate: ZCnt get() = this.available()
override var position: Idx = 0
  protected set

// right-inclusive ([1,>2,3], pos=nextIdx=3); ([1,2,>3], pos=nextIdx=4, >size)
inline val hasRemaining: Boolean get() = position <= estimate
inline val remaining: Cnt get() = estimate - position
```

### `seek` (skipBytes)

```kotlin
override tailrec fun seek(n: LongCnt) {
  if (n == 0L) return //EOS
  val skipped = this.skip(n)
  if (skipped != 0L) seek(n - skipped)
}
```

## Code review

### Project-management

+ [ReadMe](README.md), [License](LICENSE)
+ [.gitigore](.gitignore) [.editorconfig](.editorconfig)


+ [gradle/](gradle), [gradlew](gradlew), [gradle-version=4.4](gradle/wrapper/gradle-wrapper.properties)
+ [build](build.gradle), [settings](settings.gradle)
+ [mirror-ali.gradle](gradle/mirror-ali.gradle)

### Source

+ [main-module](src/main/kotlin), [interfaces](src/main/kotlin/org/duangsuse/dokuss/intf)
+ [tests](src/test/kotlin/org/duangsuse/dokuss)

## 📓 [License](LICENSE)

## Related links

+ [sdklite/SED (Java)](https://github.com/sdklite/sed/blob/master/src/main/java/com/sdklite/sed/)
+ [Binary Reader/Writer extension (C#)](https://www.cnblogs.com/conmajia/p/a-more-powerful-binary-reader-writer.html)
