/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

private const val LOWER_CASE_HEX_DIGITS = "0123456789abcdef"
private const val UPPER_CASE_HEX_DIGITS = "0123456789ABCDEF"

// case-insensitive parsing
private val HEX_DIGITS_TO_DECIMAL = IntArray(128) { -1 }.apply {
    LOWER_CASE_HEX_DIGITS.forEachIndexed { index, char -> this[char.code] = index }
    UPPER_CASE_HEX_DIGITS.forEachIndexed { index, char -> this[char.code] = index }
}

private const val BYTES_LINE_SEPARATOR: String = "\n"

// -------------------------- format and parse ByteArray --------------------------

/**
 * Formats bytes in this array using the specified [format].
 *
 * Note that only [HexFormat.upperCase] and [HexFormat.Bytes] affect formatting.
 *
 * @param format the [HexFormat] to use for formatting, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public fun ByteArray.toHexString(format: HexFormat = HexFormat.Default): String = toHexString(0, size, format)

/**
 * Formats bytes in this array using the specified [HexFormat].
 *
 * Note that only [HexFormat.upperCase] and [HexFormat.Bytes] affect formatting.
 *
 * @param startIndex the beginning (inclusive) of the subrange to format, 0 by default.
 * @param endIndex the end (exclusive) of the subrange to format, size of this array by default.
 * @param format the [HexFormat] to use for formatting, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public fun ByteArray.toHexString(
    startIndex: Int = 0,
    endIndex: Int = size,
    format: HexFormat = HexFormat.Default
): String {
    AbstractList.checkBoundsIndexes(startIndex, endIndex, size)

    val digits = if (format.upperCase) UPPER_CASE_HEX_DIGITS else LOWER_CASE_HEX_DIGITS
    val bytesFormat = format.bytes
    val perLine = bytesFormat.bytesPerLine.let { if (it <= 0) Int.MAX_VALUE else it }
    val perGroup = bytesFormat.bytesPerGroup.let { if (it <= 0) perLine else it }

    var indexInLine = 0
    var indexInGroup = 0

    return buildString {
        for (i in startIndex until endIndex) {
            val byte = this@toHexString[i].toInt() and 0xFF

            if (indexInLine == perLine) {
                append(BYTES_LINE_SEPARATOR)
                indexInLine = 0
                indexInGroup = 0
            } else if (indexInGroup == perGroup) {
                append(bytesFormat.groupSeparator)
                indexInGroup = 0
            }
            if (indexInGroup != 0) {
                append(bytesFormat.byteSeparator)
            }

            append(bytesFormat.bytePrefix)
            append(digits[byte shr 4])
            append(digits[byte and 0xF])
            append(bytesFormat.byteSuffix)

            indexInGroup += 1
            indexInLine += 1
        }
    }
}

/**
 * Parses bytes from this string using the specified [HexFormat].
 *
 * Note that only [HexFormat.Bytes] affects parsing.
 *
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public fun String.hexToByteArray(format: HexFormat = HexFormat.Default): ByteArray = hexToByteArray(0, length, format)

/**
 * Parses bytes from this string using the specified [HexFormat].
 *
 * Note that only [HexFormat.Bytes] affects parsing.
 *
 * @param startIndex the beginning (inclusive) of the substring to parse, 0 by default.
 * @param endIndex the end (exclusive) of the substring to parse, length of this string by default.
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public fun String.hexToByteArray(
    startIndex: Int = 0,
    endIndex: Int = length,
    format: HexFormat = HexFormat.Default
): ByteArray {
    AbstractList.checkBoundsIndexes(startIndex, endIndex, length)

    if (startIndex == endIndex) {
        return byteArrayOf()
    }

    val bytesFormat = format.bytes
    val bytesPerLine = bytesFormat.bytesPerLine.let { if (it <= 0) Int.MAX_VALUE else it }
    val bytesPerGroup = bytesFormat.bytesPerGroup.let { if (it <= 0) bytesPerLine else it }
    val bytePrefix = bytesFormat.bytePrefix
    val byteSuffix = bytesFormat.byteSuffix
    val byteSeparator = bytesFormat.byteSeparator
    val groupSeparator = bytesFormat.groupSeparator

    val byteArraySize = calculateParsedByteArraySize(
        stringLength = endIndex - startIndex,
        bytesPerLine,
        bytesPerGroup,
        groupSeparator.length,
        byteSeparator.length,
        bytePrefix.length,
        byteSuffix.length
    )
    val result = ByteArray(byteArraySize)

    var i = startIndex
    var byteIndex = 0
    var indexInLine = 0
    var indexInGroup = 0

    while (i < endIndex) {
        if (indexInLine == bytesPerLine) {
            if (this[i] != '\n') {
                throw NumberFormatException("Expected line separator '\\n' at index $i, but was ${this[i]}")
            }
            i += 1
            indexInLine = 0
            indexInGroup = 0
        } else if (indexInGroup == bytesPerGroup) {
            checkContainsAt(groupSeparator, i, endIndex, "group separator")
            i += groupSeparator.length
            indexInGroup = 0
        } else if (indexInGroup != 0) {
            checkContainsAt(byteSeparator, i, endIndex, "byte separator")
            i += byteSeparator.length
        }

        checkContainsAt(bytePrefix, i, endIndex, "byte prefix")
        i += bytePrefix.length

        checkHexLength(i, (i + 2).coerceAtMost(endIndex), maxDigits = 2, dropLeadingZeros = false)

        result[byteIndex++] = ((decimalFromHexDigitAt(i) shl 4) or decimalFromHexDigitAt(i + 1)).toByte()
        i += 2

        checkContainsAt(byteSuffix, i, endIndex, "byte suffix")
        i += byteSuffix.length
    }

    check(byteIndex == result.size)

    return result
}

private fun calculateParsedByteArraySize(
    stringLength: Int,
    bytesPerLine: Int,
    bytesPerGroup: Int,
    groupSeparatorLength: Int,
    byteSeparatorLength: Int,
    bytePrefixLength: Int,
    byteSuffixLength: Int
): Int {
    val charsPerByte = bytePrefixLength + 2L + byteSuffixLength
    val charsPerGroup = charsPerSet(charsPerByte, bytesPerGroup, byteSeparatorLength)

    val bytesPerLastGroupInLine = bytesPerLine % bytesPerGroup
    val charsPerLastGroupInLine = charsPerSet(charsPerByte, bytesPerLastGroupInLine, byteSeparatorLength)

    val groupsPerLine = bytesPerLine / bytesPerGroup
    val charsPerLine = charsPerSet(charsPerGroup, groupsPerLine, groupSeparatorLength) +
            if (bytesPerLastGroupInLine == 0) 0 else groupSeparatorLength + charsPerLastGroupInLine

    var numberOfChars = stringLength.toLong()
    val lines = elementsPerSet(numberOfChars, charsPerLine, BYTES_LINE_SEPARATOR.length)
    numberOfChars -= lines * charsPerLine
    val groupsInLastLine = elementsPerSet(numberOfChars, charsPerGroup, groupSeparatorLength)
    numberOfChars -= groupsInLastLine * charsPerGroup
    val bytesInLastGroup = elementsPerSet(numberOfChars, charsPerByte, byteSeparatorLength)
    numberOfChars -= bytesInLastGroup * charsPerByte

    // If numberOfChars is not zero here, have a spare capacity to let parsing continue.
    // It will throw later on with a correct message.
    val spare = if (numberOfChars != 0L) 1 else 0

    return ((lines * bytesPerLine) + (groupsInLastLine * bytesPerGroup) + bytesInLastGroup + spare).toInt()
}

private fun charsPerSet(charsPerElement: Long, elementsPerSet: Int, elementSeparatorLength: Int): Long {
    return (charsPerElement * elementsPerSet) + (elementSeparatorLength * (elementsPerSet - 1L))
}

private fun elementsPerSet(charsPerSet: Long, charsPerElement: Long, elementSeparatorLength: Int): Long {
    return (charsPerSet + elementSeparatorLength) / (charsPerElement + elementSeparatorLength)
}

// -------------------------- format and parse Byte --------------------------

/**
 * Formats this `Byte` value using the specified [format].
 *
 * Note that only [HexFormat.upperCase] and [HexFormat.Numbers] affect formatting.
 *
 * @param format the [HexFormat] to use for formatting, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public fun Byte.toHexString(format: HexFormat = HexFormat.Default): String = toLong().toHexStringImpl(format, bits = 8)

/**
 * Parses a `Byte` value from this string using the specified [format].
 *
 * Note that only [HexFormat.Numbers] affects parsing.
 *
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public fun String.hexToByte(format: HexFormat = HexFormat.Default): Byte = hexToByte(0, length, format)

/**
 * Parses a `Byte` value from this string using the specified [format].
 *
 * Note that only [HexFormat.Numbers] affects parsing.
 *
 * @param startIndex the beginning (inclusive) of the substring to parse, 0 by default.
 * @param endIndex the end (exclusive) of the substring to parse, length of this string by default.
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public fun String.hexToByte(startIndex: Int = 0, endIndex: Int = length, format: HexFormat = HexFormat.Default): Byte =
    hexToLongImpl(startIndex, endIndex, format, maxDigits = 2).toByte()

// -------------------------- format and parse Short --------------------------

/**
 * Formats this `Short` value using the specified [format].
 *
 * Note that only [HexFormat.upperCase] and [HexFormat.Numbers] affect formatting.
 *
 * @param format the [HexFormat] to use for formatting, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public fun Short.toHexString(format: HexFormat = HexFormat.Default): String = toLong().toHexStringImpl(format, bits = 16)

/**
 * Parses a `Short` value from this string using the specified [format].
 *
 * Note that only [HexFormat.Numbers] affects parsing.
 *
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public fun String.hexToShort(format: HexFormat = HexFormat.Default): Short = hexToShort(0, length, format)

/**
 * Parses a `Short` value from this string using the specified [format].
 *
 * Note that only [HexFormat.Numbers] affects parsing.
 *
 * @param startIndex the beginning (inclusive) of the substring to parse, 0 by default.
 * @param endIndex the end (exclusive) of the substring to parse, length of this string by default.
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public fun String.hexToShort(startIndex: Int = 0, endIndex: Int = length, format: HexFormat = HexFormat.Default): Short =
    hexToLongImpl(startIndex, endIndex, format, maxDigits = 4).toShort()

// -------------------------- format and parse Int --------------------------

/**
 * Formats this `Int` value using the specified [format].
 *
 * Note that only [HexFormat.upperCase] and [HexFormat.Numbers] affect formatting.
 *
 * @param format the [HexFormat] to use for formatting, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public fun Int.toHexString(format: HexFormat = HexFormat.Default): String = toLong().toHexStringImpl(format, bits = 32)

/**
 * Parses an `Int` value from this string using the specified [format].
 *
 * Note that only [HexFormat.Numbers] affects parsing.
 *
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public fun String.hexToInt(format: HexFormat = HexFormat.Default): Int = hexToInt(0, length, format)

/**
 * Parses an `Int` value from this string using the specified [format].
 *
 * Note that only [HexFormat.Numbers] affects parsing.
 *
 * @param startIndex the beginning (inclusive) of the substring to parse, 0 by default.
 * @param endIndex the end (exclusive) of the substring to parse, length of this string by default.
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public fun String.hexToInt(startIndex: Int = 0, endIndex: Int = length, format: HexFormat = HexFormat.Default): Int =
    hexToLongImpl(startIndex, endIndex, format, maxDigits = 8).toInt()

// -------------------------- format and parse Long --------------------------

/**
 * Formats this `Long` value using the specified [format].
 *
 * Note that only [HexFormat.upperCase] and [HexFormat.Numbers] affect formatting.
 *
 * @param format the [HexFormat] to use for formatting, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public fun Long.toHexString(format: HexFormat = HexFormat.Default): String = toHexStringImpl(format, bits = 64)

/**
 * Parses a `Long` value from this string using the specified [format].
 *
 * Note that only [HexFormat.Numbers] affects parsing.
 *
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public fun String.hexToLong(format: HexFormat = HexFormat.Default): Long = hexToLong(0, length, format)

/**
 * Parses a `Long` value from this string using the specified [format].
 *
 * Note that only [HexFormat.Numbers] affects parsing.
 *
 * @param startIndex the beginning (inclusive) of the substring to parse, 0 by default.
 * @param endIndex the end (exclusive) of the substring to parse, length of this string by default.
 * @param format the [HexFormat] to use for parsing, [HexFormat.Default] by default.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public fun String.hexToLong(startIndex: Int = 0, endIndex: Int = length, format: HexFormat = HexFormat.Default): Long =
    hexToLongImpl(startIndex, endIndex, format, maxDigits = 16)

// -------------------------- private format and parse functions --------------------------

private fun Long.toHexStringImpl(format: HexFormat, bits: Int): String {
    require(bits and 0x3 == 0)

    val digits = if (format.upperCase) UPPER_CASE_HEX_DIGITS else LOWER_CASE_HEX_DIGITS
    val value = this

    val prefix = format.numbers.prefix
    val suffix = format.numbers.suffix
    val formatLength = prefix.length + (bits shr 2) + suffix.length
    var dropZeros = format.numbers.dropLeadingZeros

    return buildString(formatLength) {
        append(prefix)

        var shift = bits
        while (shift > 0) {
            shift -= 4
            val decimal = ((value shr shift) and 0xF).toInt()
            dropZeros = dropZeros && decimal == 0 && shift > 0
            if (!dropZeros) {
                append(digits[decimal])
            }
        }

        append(suffix)
    }
}

private fun String.hexToLongImpl(startIndex: Int = 0, endIndex: Int = length, format: HexFormat, maxDigits: Int): Long {
    AbstractList.checkBoundsIndexes(startIndex, endIndex, length)

    val prefix = format.numbers.prefix
    val suffix = format.numbers.suffix

    if (prefix.length + suffix.length >= endIndex - startIndex) {
        throw NumberFormatException(
            "Expected a hexadecimal number with prefix \"$prefix\" and suffix \"$suffix\", but was ${substring(startIndex, endIndex)}"
        )
    }

    checkContainsAt(prefix, startIndex, endIndex, "prefix")
    checkContainsAt(suffix, endIndex - suffix.length, endIndex, "suffix")

    val digitsStartIndex = startIndex + prefix.length
    val digitsEndIndex = endIndex - suffix.length

    checkHexLength(digitsStartIndex, digitsEndIndex, maxDigits, format.numbers.dropLeadingZeros)

    var result = 0L
    for (i in digitsStartIndex until digitsEndIndex) {
        result = (result shl 4) or decimalFromHexDigitAt(i).toLong()
    }
    return result
}

private fun String.checkContainsAt(prefix: String, index: Int, endIndex: Int, prefixName: String, checkEndsWith: Boolean = false) {
    val expectedEndIndex = index + prefix.length
    val isCorrectSize = if (checkEndsWith) expectedEndIndex == endIndex else expectedEndIndex <= endIndex
    if (!isCorrectSize || !regionMatches(index, prefix, 0, prefix.length)) {
        throw NumberFormatException(
            "Expected $prefixName \"$prefix\" at index $index, but was ${this.substring(index, expectedEndIndex.coerceAtMost(endIndex))}"
        )
    }
}

private fun String.checkHexLength(startIndex: Int, endIndex: Int, maxDigits: Int, dropLeadingZeros: Boolean) {
    val actualDigits = endIndex - startIndex
    val isCorrectLength = if (dropLeadingZeros) actualDigits <= maxDigits else actualDigits == maxDigits
    if (!isCorrectLength) {
        val specifier = if (dropLeadingZeros) "at most" else "exactly"
        val substring = substring(startIndex, endIndex)
        throw NumberFormatException(
            "Expected $specifier $maxDigits hexadecimal digits at index $startIndex, but was $substring of length $actualDigits"
        )
    }
}

private fun String.decimalFromHexDigitAt(index: Int): Int {
    val code = this[index].code
    if (code > 127 || HEX_DIGITS_TO_DECIMAL[code] < 0) {
        throw NumberFormatException("Expected a hexadecimal digit at index $index, but was ${this[index]}")
    }
    return HEX_DIGITS_TO_DECIMAL[code]
}
