/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

import kotlin.internal.InlineOnly

/**
 * Represents hexadecimal format options.
 *
 * To create a new [HexFormat] use `HexFormat` function.
 */
public class HexFormat internal constructor(
    /**
     * Specifies whether upper case hexadecimal digits `0-9`, `A-F` should be used.
     * If `false`, lower case hexadecimal digits `0-9`, `a-f` will be used.
     *
     * Affects both `ByteArray` and primitive value formatting.
     * */
    val upperCase: Boolean,
    /**
     * Specifies hexadecimal format used for formatting and parsing `ByteArray`.
     */
    val bytes: BytesHexFormat,
    /**
     * Specifies hexadecimal format used for formatting and parsing a value of primitive type.
     */
    val number: NumberHexFormat
) {

    /**
     * Represents hexadecimal format options for formatting and parsing `ByteArray`.
     *
     * You can assume that bytes are firstly separated using line feed character (`'\n'`) into lines
     * with [bytesPerLine] bytes in each line, except the last line, which may have fewer bytes.
     * Then each line is separated into groups using [groupSeparator] with [bytesPerGroup] bytes in each group,
     * except the last group in the line, which may have fewer bytes.
     *
     * All bytes in a group are separated using [byteSeparator].
     * Each byte is converted to its two-digit hexadecimal representation,
     * immediately preceded by [bytePrefix] and immediately succeeded by [byteSuffix].
     *
     * See [HexFormatBuilder.BytesHexFormat] to find out how the options are configured,
     * and what is the default value of each option.
     */
    public class BytesHexFormat internal constructor(
        /** Maximum number of bytes per line. */
        val bytesPerLine: Int,

        /** Maximum number of bytes per group. */
        val bytesPerGroup: Int,
        /** The string used to separate adjacent groups in a line. */
        val groupSeparator: String,

        /** The string used to separate adjacent bytes in a group. */
        val byteSeparator: String,
        /** The string that immediately precedes two-digit hexadecimal representation of each byte. */
        val bytePrefix: String,
        /** The string that immediately succeeds two-digit hexadecimal representation of each byte. */
        val byteSuffix: String
    )

    /**
     * Represents hexadecimal format options for formatting and parsing a value of primitive type.
     *
     * The formatting result consist of [prefix] string, hexadecimal representation of the value being formatted, and [suffix] string.
     * Hexadecimal representation of a value is calculated by mapping each four-bit chunk
     * of its binary representation to the corresponding hexadecimal digit, starting with the most significant bits.
     * If [removeLeadingZeros] it `true`, leading zeros in hexadecimal representation are dropped.
     *
     * For example, the binary representation of the `Byte` value `58` is the 8-bits long `00111010`,
     * which converts to a hexadecimal representation of `3a` or `3A` depending on [upperCase].
     * Whereas, the binary representation of the `Int` value `58` is the 32-bits long `00000000000000000000000000111010`,
     * which converts to a hexadecimal representation of `0000003a` or `0000003A` depending on [upperCase].
     * If [removeLeadingZeros] it `true`, leading zeros in the `0000003a` hexadecimal representation are dropped,
     * resulting `3a`.
     *
     * To convert a value to hexadecimal string of a particular length,
     * first convert the value to a type with the corresponding bit size.
     * For example, to convert an `Int` value to 4-digit hexadecimal string,
     * convert the value `toShort()` before hexadecimal formatting.
     * To convert it to hexadecimal string of at most 4 digits
     * without leading zeros, set [removeLeadingZeros] to `true`.
     *
     * See [HexFormatBuilder.NumberHexFormat] to find out how the options are configured,
     * and what is the default value of each option.
     */
    public class NumberHexFormat internal constructor(
        /** The string that immediately precedes hexadecimal representation of a primitive value. */
        val prefix: String,
        /** The string that immediately succeeds hexadecimal representation of a primitive value. */
        val suffix: String,
        /** Specifies whether to drop leading zeros in the hexadecimal representation of a primitive value. */
        val removeLeadingZeros: Boolean
    )

    companion object {
        /**
         * The default hexadecimal format options.
         *
         * Uses lower case hexadecimal digits `0-9`, `a-f` when formatting
         * both `ByteArray` and primitive values. That is [upperCase] is `false`.
         *
         * No line separator, group separator, byte separator, byte prefix or byte suffix is used
         * when formatting or parsing `ByteArray`. That is:
         *   * [BytesHexFormat.bytesPerLine] is `-1`.
         *   * [BytesHexFormat.bytesPerGroup] is `-1`.
         *   * [BytesHexFormat.byteSeparator], [BytesHexFormat.bytePrefix] and [BytesHexFormat.byteSuffix] are empty strings.
         *
         * No prefix or suffix is used, and no leading zeros in hexadecimal representation are dropped
         * when formatting or parsing a primitive value. That is:
         *   * [NumberHexFormat.prefix] and [NumberHexFormat.suffix] are empty strings.
         *   * [NumberHexFormat.removeLeadingZeros] is `false`.
         */
        public val Default: HexFormat = HexFormatBuilder().build()

        /**
         * Uses upper case hexadecimal digits `0-9`, `A-F` when formatting
         * both `ByteArray` and primitive values. That is [upperCase] is `true`.
         *
         * The same as [Default] format in other aspects.
         */
        public val UpperCase: HexFormat = HexFormat { upperCase = true }
    }
}

/**
 * DSL for building a [HexFormat]. Provides API for configuring format options.
 */
public class HexFormatBuilder @PublishedApi internal constructor() {
    /** Defines [HexFormat.upperCase] of the format being built, `false` by default. */
    var upperCase: Boolean = false

    /**
     * Defines [HexFormat.BytesHexFormat] of the format being built.
     * See [HexFormatBuilder.BytesHexFormat] for default values of the options.
     */
    val bytes: BytesHexFormat = BytesHexFormat()

    /**
     * Defines [HexFormat.NumberHexFormat] of the format being built.
     * See [HexFormatBuilder.NumberHexFormat] for default values of the options.
     */
    val number: NumberHexFormat = NumberHexFormat()

    /** Provides a scope for configuring the [bytes] format options. */
    fun bytes(builderAction: BytesHexFormat.() -> Unit) {
        bytes.builderAction()
    }

    /** Provides a scope for configuring the [number] format options. */
    fun number(builderAction: NumberHexFormat.() -> Unit) {
        number.builderAction()
    }

    @PublishedApi
    internal fun build(): HexFormat {
        return HexFormat(upperCase, bytes.build(), number.build())
    }

    /**
     * DSL for building a [HexFormat.BytesHexFormat]. Provides API for configuring format options.
     */
    class BytesHexFormat internal constructor() {
        /** Defines [HexFormat.BytesHexFormat.bytesPerLine] of the format being built, [Int.MAX_VALUE] by default. */
        var bytesPerLine: Int = Int.MAX_VALUE
            set(value) {
                if (value <= 0)
                    throw IllegalArgumentException("Non-positive values are prohibited for bytesPerLine, but was $value")
                field = value
            }

        /** Defines [HexFormat.BytesHexFormat.bytesPerGroup] of the format being built, [Int.MAX_VALUE] by default. */
        var bytesPerGroup: Int = Int.MAX_VALUE
            set(value) {
                if (value <= 0)
                    throw IllegalArgumentException("Non-positive values are prohibited for bytesPerGroup, but was $value")
                field = value
            }
        /** Defines [HexFormat.BytesHexFormat.groupSeparator] of the format being built, two whitespaces (`"  "`) by default. */
        var groupSeparator: String = "  "

        /** Defines [HexFormat.BytesHexFormat.byteSeparator] of the format being built, empty string by default. */
        var byteSeparator: String = ""
        /** Defines [HexFormat.BytesHexFormat.bytePrefix] of the format being built, empty string by default. */
        var bytePrefix: String = ""
        /** Defines [HexFormat.BytesHexFormat.byteSuffix] of the format being built, empty string by default. */
        var byteSuffix: String = ""

        internal fun build(): HexFormat.BytesHexFormat {
            return HexFormat.BytesHexFormat(bytesPerLine, bytesPerGroup, groupSeparator, byteSeparator, bytePrefix, byteSuffix)
        }
    }

    /**
     * DSL for building a [HexFormat.NumberHexFormat]. Provides API for configuring format options.
     */
    class NumberHexFormat internal constructor() {
        /** Defines [HexFormat.NumberHexFormat.prefix] of the format being built, empty string by default. */
        var prefix: String = ""
        /** Defines [HexFormat.NumberHexFormat.suffix] of the format being built, empty string by default. */
        var suffix: String = ""
        /** Defines [HexFormat.NumberHexFormat.removeLeadingZeros] of the format being built, empty string by default. */
        var removeLeadingZeros: Boolean = false

        internal fun build(): HexFormat.NumberHexFormat {
            return HexFormat.NumberHexFormat(prefix, suffix, removeLeadingZeros)
        }
    }
}

/**
 * Builds a new [HexFormat] by configuring its format options using the specified [builderAction],
 * and returns the resulting format.
 */
@InlineOnly
public inline fun HexFormat(builderAction: HexFormatBuilder.() -> Unit): HexFormat {
    return HexFormatBuilder().apply(builderAction).build()
}
