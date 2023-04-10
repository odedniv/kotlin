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
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public class HexFormat internal constructor(
    /**
     * Specifies whether upper case hexadecimal digits `0-9`, `A-F` should be used for formatting.
     * If `false`, lower case hexadecimal digits `0-9`, `a-f` will be used.
     *
     * Affects both `ByteArray` and numeric value formatting.
     * */
    val upperCase: Boolean,
    /**
     * Specifies hexadecimal format used for formatting and parsing `ByteArray`.
     */
    val bytes: BytesHexFormat,
    /**
     * Specifies hexadecimal format used for formatting and parsing a numeric value.
     */
    val number: NumberHexFormat
) {

    /**
     * Represents hexadecimal format options for formatting and parsing `ByteArray`.
     *
     * When formatting one can assume that bytes are firstly separated using LF character (`'\n'`) into lines
     * with [bytesPerLine] bytes in each line. The last line may have fewer bytes, and have no line separator at the end.
     * Then each line is separated into groups using [groupSeparator] with [bytesPerGroup] bytes in each group,
     * except the last group in the line, which may have fewer bytes.
     * All bytes in a group are separated using [byteSeparator].
     * Each byte is converted to its two-digit hexadecimal representation,
     * immediately preceded by [bytePrefix] and immediately succeeded by [byteSuffix].
     *
     * When parsing the input string is required to be in the format described above.
     * However, any of the char sequences CRLF, LF and CR is considered a valid line separator,
     * and parsing is performed in case-insensitive manner.
     *
     * See [BytesHexFormat.Builder] to find out how the options are configured,
     * and what is the default value of each option.
     */
    public class BytesHexFormat internal constructor(
        /** The maximum number of bytes per line. */
        val bytesPerLine: Int,

        /** The maximum number of bytes per group. */
        val bytesPerGroup: Int,
        /** The string used to separate adjacent groups in a line. */
        val groupSeparator: String,

        /** The string used to separate adjacent bytes in a group. */
        val byteSeparator: String,
        /** The string that immediately precedes two-digit hexadecimal representation of each byte. */
        val bytePrefix: String,
        /** The string that immediately succeeds two-digit hexadecimal representation of each byte. */
        val byteSuffix: String
    ) {
        /**
         * DSL for building a [BytesHexFormat]. Provides API for configuring format options.
         */
        class Builder internal constructor() {
            /**
             * Defines [BytesHexFormat.bytesPerLine] of the format being built, [Int.MAX_VALUE] by default.
             *
             * Setting a non-positive value is prohibited.
             */
            var bytesPerLine: Int = Int.MAX_VALUE
                set(value) {
                    if (value <= 0)
                        throw IllegalArgumentException("Non-positive values are prohibited for bytesPerLine, but was $value")
                    field = value
                }

            /**
             * Defines [BytesHexFormat.bytesPerGroup] of the format being built, [Int.MAX_VALUE] by default.
             *
             * Setting a non-positive value is prohibited.
             */
            var bytesPerGroup: Int = Int.MAX_VALUE
                set(value) {
                    if (value <= 0)
                        throw IllegalArgumentException("Non-positive values are prohibited for bytesPerGroup, but was $value")
                    field = value
                }

            /** Defines [BytesHexFormat.groupSeparator] of the format being built, two whitespaces (`"  "`) by default. */
            var groupSeparator: String = "  "

            /** Defines [BytesHexFormat.byteSeparator] of the format being built, empty string by default. */
            var byteSeparator: String = ""

            /** Defines [BytesHexFormat.bytePrefix] of the format being built, empty string by default. */
            var bytePrefix: String = ""

            /** Defines [BytesHexFormat.byteSuffix] of the format being built, empty string by default. */
            var byteSuffix: String = ""

            internal fun build(): BytesHexFormat {
                return BytesHexFormat(bytesPerLine, bytesPerGroup, groupSeparator, byteSeparator, bytePrefix, byteSuffix)
            }
        }
    }

    /**
     * Represents hexadecimal format options for formatting and parsing a numeric value.
     *
     * The formatting result consist of [prefix] string, hexadecimal representation of the value being formatted, and [suffix] string.
     * Hexadecimal representation of a value is calculated by mapping each four-bit chunk
     * of its binary representation to the corresponding hexadecimal digit, starting with the most significant bits.
     * [upperCase] determines whether upper case `0-9`, `A-F` or lower case `0-9`, `a-f` hexadecimal digits are used.
     * If [removeLeadingZeros] it `true`, leading zeros in the hexadecimal representation are removed.
     *
     * For example, the binary representation of the `Byte` value `58` is the 8-bits long `00111010`,
     * which converts to a hexadecimal representation of `3a` or `3A` depending on [upperCase].
     * Whereas, the binary representation of the `Int` value `58` is the 32-bits long `00000000000000000000000000111010`,
     * which converts to a hexadecimal representation of `0000003a` or `0000003A` depending on [upperCase].
     * If [removeLeadingZeros] it `true`, leading zeros in `0000003a` are removed, resulting `3a`.
     *
     * To convert a value to hexadecimal string of a particular length,
     * first convert the value to a type with the corresponding bit size.
     * For example, to convert an `Int` value to 4-digit hexadecimal string,
     * convert the value `toShort()` before hexadecimal formatting.
     * To convert it to hexadecimal string of at most 4 digits
     * without leading zeros, set [removeLeadingZeros] to `true` in addition.
     *
     * Parsing requires [prefix] and [suffix] to be present in the input string,
     * and the amount of hexadecimal digits to be at least one and at most the value bit size divided by four.
     * Parsing is performed in case-insensitive manner, and [removeLeadingZeros] is ignored as well.
     *
     * See [NumberHexFormat.Builder] to find out how the options are configured,
     * and what is the default value of each option.
     */
    public class NumberHexFormat internal constructor(
        /** The string that immediately precedes hexadecimal representation of a numeric value. */
        val prefix: String,
        /** The string that immediately succeeds hexadecimal representation of a numeric value. */
        val suffix: String,
        /** Specifies whether to remove leading zeros in the hexadecimal representation of a numeric value. */
        val removeLeadingZeros: Boolean
    ) {
        /**
         * DSL for building a [NumberHexFormat]. Provides API for configuring format options.
         */
        class Builder internal constructor() {
            /** Defines [NumberHexFormat.prefix] of the format being built, empty string by default. */
            var prefix: String = ""

            /** Defines [NumberHexFormat.suffix] of the format being built, empty string by default. */
            var suffix: String = ""

            /** Defines [NumberHexFormat.removeLeadingZeros] of the format being built, empty string by default. */
            var removeLeadingZeros: Boolean = false

            internal fun build(): NumberHexFormat {
                return NumberHexFormat(prefix, suffix, removeLeadingZeros)
            }
        }
    }


    /**
     * DSL for building a [HexFormat]. Provides API for configuring format options.
     */
    public class Builder @PublishedApi internal constructor() {
        /** Defines [HexFormat.upperCase] of the format being built, `false` by default. */
        var upperCase: Boolean = false

        /**
         * Defines [HexFormat.bytes] of the format being built.
         *
         * See [BytesHexFormat.Builder] for default values of the options.
         */
        val bytes: BytesHexFormat.Builder = BytesHexFormat.Builder()

        /**
         * Defines [HexFormat.number] of the format being built.
         *
         * See [NumberHexFormat.Builder] for default values of the options.
         */
        val number: NumberHexFormat.Builder = NumberHexFormat.Builder()

        /**
         * Provides a scope for configuring the [HexFormat.bytes] format options.
         *
         * See [BytesHexFormat.Builder] for default values of the options.
         */
        @InlineOnly
        inline fun bytes(builderAction: BytesHexFormat.Builder.() -> Unit) {
            bytes.builderAction()
        }

        /**
         * Provides a scope for configuring the [HexFormat.number] format options.
         *
         * See [NumberHexFormat.Builder] for default values of the options.
         */
        @InlineOnly
        inline fun number(builderAction: NumberHexFormat.Builder.() -> Unit) {
            number.builderAction()
        }

        @PublishedApi
        internal fun build(): HexFormat {
            return HexFormat(upperCase, bytes.build(), number.build())
        }
    }


    companion object {
        /**
         * The default hexadecimal format options.
         *
         * Uses lower case hexadecimal digits `0-9`, `a-f` when formatting
         * both `ByteArray` and numeric values. That is [upperCase] is `false`.
         *
         * No line separator, group separator, byte separator, byte prefix or byte suffix is used
         * when formatting or parsing `ByteArray`. That is:
         *   * [BytesHexFormat.bytesPerLine] is `Int.MAX_VALUE`.
         *   * [BytesHexFormat.bytesPerGroup] is `Int.MAX_VALUE`.
         *   * [BytesHexFormat.byteSeparator], [BytesHexFormat.bytePrefix] and [BytesHexFormat.byteSuffix] are empty strings.
         *
         * No prefix or suffix is used, and no leading zeros in hexadecimal representation are removed
         * when formatting or parsing a numeric value. That is:
         *   * [NumberHexFormat.prefix] and [NumberHexFormat.suffix] are empty strings.
         *   * [NumberHexFormat.removeLeadingZeros] is `false`.
         */
        public val Default: HexFormat = Builder().build()

        /**
         * Uses upper case hexadecimal digits `0-9`, `A-F` when formatting
         * both `ByteArray` and numeric values. That is [upperCase] is `true`.
         *
         * The same as [Default] format in other aspects.
         */
        public val UpperCase: HexFormat = HexFormat { upperCase = true }
    }
}

/**
 * Builds a new [HexFormat] by configuring its format options using the specified [builderAction],
 * and returns the resulting format.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
@InlineOnly
public inline fun HexFormat(builderAction: HexFormat.Builder.() -> Unit): HexFormat {
    return HexFormat.Builder().apply(builderAction).build()
}
