/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NumberHexFormatTest {

    private fun testDefaultFormatAndParse(value: Int, expected: String) {
        assertEquals(expected, value.toHexString())
        assertEquals(value, expected.hexToInt())
    }

    private fun testFormatAndParse(value: Int, expected: String, format: HexFormat) {
        assertEquals(expected, value.toHexString(format))
        assertEquals(value, expected.hexToInt(format))
    }

    @Test
    fun ignoreBytesFormat() {
        val format = HexFormat {
            bytes {
                bytesPerLine = 10
                bytesPerGroup = 4
                groupSeparator = "---"
                byteSeparator = " "
                bytePrefix = "#"
                byteSuffix = ";"
            }
        }

        testFormatAndParse(58, "0000003a", format)
    }

    @Test
    fun upperCase() {
        testFormatAndParse(58, "0000003A", HexFormat { upperCase = true })
        testFormatAndParse(58, "0000003A", HexFormat.UpperCase)
    }

    @Test
    fun lowerCase() {
        testFormatAndParse(58, "0000003a", HexFormat { upperCase = false })
    }

    @Test
    fun defaultCase() {
        testDefaultFormatAndParse(58, "0000003a")
        testFormatAndParse(58, "0000003a", HexFormat.Default)
    }

    @Test
    fun removeLeadingZeros() {
        val format = HexFormat { number.removeLeadingZeros = true }
        testFormatAndParse(58, "3a", format)
        testFormatAndParse(0, "0", format)
    }

    @Test
    fun parseLongFromSubstring() {
        val url = "https://magnuschatt.medium.com/why-you-should-totally-switch-to-kotlin-c7bbde9e10d5"
        val idStartIndex = url.lastIndexOf('-') + 1
        val articleId = url.substring(idStartIndex).hexToLong()
        assertEquals(0xc7bbde9e10d5, articleId)
    }

    // Number parsing strictness

    @Test
    fun parseIgnoresCase() {
        assertEquals(58, "0000003a".hexToInt())
        assertEquals(58, "3a".hexToInt())
        assertEquals(58, "0000003A".hexToInt())
        assertEquals(58, "3A".hexToInt())

        val format = HexFormat {
            upperCase = true
            number {
                prefix = "0X"
                suffix = "h"
            }
        }
        assertEquals(58, "0X0000003AH".hexToInt(format))
        assertEquals(58, "0x3Ah".hexToInt(format))
        assertEquals(58, "0X0000003aH".hexToInt(format))
        assertEquals(58, "0x3ah".hexToInt(format))
    }

    @Test
    fun parseIgnoresRemoveLeadingZeros() {
        assertEquals(58, "3a".hexToInt())
        assertEquals(58, "3a".hexToInt(HexFormat { number.removeLeadingZeros = false }))
        assertEquals(58, "0000003a".hexToInt(HexFormat { number.removeLeadingZeros = true }))
    }

    @Test
    fun parseLimitsHexLength() {
        assertEquals(0, "0".hexToByte())  // length = 1
        assertEquals(0, "00".hexToByte())  // length = 2
        assertEquals(58, "3a".hexToByte())  // length = 2
        assertFailsWith<NumberFormatException> { "03a".hexToByte() } // length = 3
        assertEquals(58, "03a".hexToShort()) // length = 3
        assertEquals(58, "003a".hexToShort()) // length = 4
        assertFailsWith<NumberFormatException> { "0003a".hexToShort() } // length = 5
        assertEquals(58, "0003a".hexToInt()) // length = 5
        assertEquals(58, "0000003a".hexToInt()) // length = 8
        assertFailsWith<NumberFormatException> { "00000003a".hexToInt() } // length = 9
        assertEquals(58, "00000003a".hexToLong()) // length = 9
        assertEquals(58, "000000000000003a".hexToLong()) // length = 16
        assertFailsWith<NumberFormatException> { "00000000000000003a".hexToLong() } // length = 17
    }
}