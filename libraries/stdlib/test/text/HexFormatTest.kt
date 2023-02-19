/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import kotlin.test.Test
import kotlin.test.assertEquals

class HexFormatTest {
    @Test
    fun formatByteArrayIgnoreNumbersFormat() {
        val format = HexFormat {
            bytes {
                bytePrefix = "#"
                byteSuffix = ";"
            }
            // should not affect ByteArray formatting
            numbers {
                prefix = "0x"
                suffix = "h"
                dropLeadingZeros = true
            }
        }

        val byteArray = ByteArray(4) { it.toByte() }
        assertEquals("#00;#01;#02;#03;", byteArray.toHexString(format))
    }

    @Test
    fun formatByteArrayUpperCase() {
        val byteArray = ByteArray(4) { (10 + it).toByte() }
        assertEquals("0A0B0C0D", byteArray.toHexString(HexFormat { upperCase = true }))
    }

    @Test
    fun formatByteArrayLowerCase() {
        val byteArray = ByteArray(4) { (10 + it).toByte() }
        assertEquals("0a0b0c0d", byteArray.toHexString(HexFormat { upperCase = false }))
    }

    @Test
    fun formatByteArrayOnlyLines() {
        val format = HexFormat {
            upperCase = true
            bytes {
                bytesPerLine = 10
                byteSeparator = " "
                bytePrefix = "#"
                byteSuffix = ";"
            }
        }

        val byteArray = ByteArray(27) { it.toByte() }

        val expected = """
            #00; #01; #02; #03; #04; #05; #06; #07; #08; #09;
            #0A; #0B; #0C; #0D; #0E; #0F; #10; #11; #12; #13;
            #14; #15; #16; #17; #18; #19; #1A;
        """.trimIndent()

        assertEquals(expected, byteArray.toHexString(format))
    }

    @Test
    fun formatByteArrayOnlyGroups() {
        val format = HexFormat {
            upperCase = true
            bytes {
                bytesPerGroup = 4
                groupSeparator = "---"
            }
        }

        val byteArray = ByteArray(30) { it.toByte() }

        val expected = "00010203---04050607---08090A0B---0C0D0E0F---10111213---14151617---18191A1B---1C1D"
        assertEquals(expected, byteArray.toHexString(format))
    }

    @Test
    fun formatByteArrayLinesAndGroups() {
        val format = HexFormat {
            upperCase = true
            bytes {
                bytesPerLine = 10
                bytesPerGroup = 4
                groupSeparator = "---"
                byteSeparator = " "
                bytePrefix = "#"
                byteSuffix = ";"
            }
        }

        val byteArray = ByteArray(31) { it.toByte() }

        val expected = """
            #00; #01; #02; #03;---#04; #05; #06; #07;---#08; #09;
            #0A; #0B; #0C; #0D;---#0E; #0F; #10; #11;---#12; #13;
            #14; #15; #16; #17;---#18; #19; #1A; #1B;---#1C; #1D;
            #1E;
        """.trimIndent()

        assertEquals(expected, byteArray.toHexString(format))
    }

    @Test
    fun formatMacAddress() {
        val address = byteArrayOf(0x00, 0x1b, 0x63, 0x84.toByte(), 0x45, 0xe6.toByte())

        assertEquals(
            "00:1b:63:84:45:e6",
            address.toHexString(HexFormat { bytes.byteSeparator = ":" })
        )
        assertEquals(
            "00-1B-63-84-45-E6",
            address.toHexString(HexFormat { upperCase = true; bytes.byteSeparator = "-" })
        )
        assertEquals(
            "001B.6384.45E6",
            address.toHexString(HexFormat { upperCase = true; bytes.bytesPerGroup = 2; bytes.groupSeparator = "." })
        )
    }

    @Test
    fun formatWithLeadingZeros() {
        assertEquals("0000003a", 58.toHexString())
        assertEquals("0000003a", 58.toHexString(HexFormat { numbers.dropLeadingZeros = false }))
        assertEquals("0000003A", 58.toHexString(HexFormat.UpperCase))
    }

    @Test
    fun formatWithNoLeadingZeros() {
        assertEquals("3a", 58.toHexString(HexFormat { numbers.dropLeadingZeros = true }))
        assertEquals("0", 0.toHexString(HexFormat { numbers.dropLeadingZeros = true }))
    }

    @Test
    fun parseLongFromSubstring() {
        val url = "https://magnuschatt.medium.com/why-you-should-totally-switch-to-kotlin-c7bbde9e10d5"
        val idStartIndex = url.lastIndexOf('-') + 1
        assertEquals(12, url.length - idStartIndex)

        val articleId = url.hexToLong(startIndex = idStartIndex, format = HexFormat { numbers.dropLeadingZeros = true })

        assertEquals(0xc7bbde9e10d5, articleId)
    }
}