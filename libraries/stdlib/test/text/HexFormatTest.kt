/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import kotlin.test.*

class HexFormatTest {
    @Test
    fun formatByteArrayIgnoreNumbersFormat() {
        val format = HexFormat {
            bytes {
                bytePrefix = "#"
                byteSuffix = ";"
            }
            // should not affect ByteArray formatting
            number {
                prefix = "0x"
                suffix = "h"
                removeLeadingZeros = true
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

    // test group separator contains LF character

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
        assertEquals("0000003a", 58.toHexString(HexFormat { number.removeLeadingZeros = false }))
        assertEquals("0000003A", 58.toHexString(HexFormat.UpperCase))
    }

    @Test
    fun formatWithNoLeadingZeros() {
        assertEquals("3a", 58.toHexString(HexFormat { number.removeLeadingZeros = true }))
        assertEquals("0", 0.toHexString(HexFormat { number.removeLeadingZeros = true }))
    }

    @Test
    fun parseLongFromSubstring() {
        val url = "https://magnuschatt.medium.com/why-you-should-totally-switch-to-kotlin-c7bbde9e10d5"
        val idStartIndex = url.lastIndexOf('-') + 1
        assertEquals(12, url.length - idStartIndex)

        val articleId = url.hexToLong(startIndex = idStartIndex, format = HexFormat { number.removeLeadingZeros = true })

        assertEquals(0xc7bbde9e10d5, articleId)
    }

    // Number parsing strictness

    @Test
    fun parseNumberIgnoreCase() {
        val hexString = "0x0000003aH"
        val format = HexFormat {
            upperCase = true
            number {
                prefix = "0X"
                suffix = "h"
            }
        }
        assertEquals(58, hexString.hexToInt(format))
    }

    @Test
    fun parseNumberIgnoreRemoveLeadingZeros() {
        assertEquals(58, "3a".hexToInt())
        assertEquals(58, "3a".hexToInt(HexFormat { number.removeLeadingZeros = false }))
        assertEquals(58, "0000003a".hexToInt(HexFormat { number.removeLeadingZeros = true }))
    }

    @Test
    fun parseNumberLimitHexLength() {
        assertEquals(58, "3a".hexToByte())  // length = 2
        assertFailsWith<NumberFormatException> { "03a".hexToByte() } // length = 2
        assertEquals(58, "003a".hexToShort()) // length = 4
        assertFailsWith<NumberFormatException> { "0003a".hexToShort() } // length = 5
        assertEquals(58, "0000003a".hexToInt()) // length = 8
        assertFailsWith<NumberFormatException> { "00000003a".hexToInt() } // length = 9
        assertEquals(58, "000000000000003a".hexToLong()) // length = 16
        assertFailsWith<NumberFormatException> { "00000000000000003a".hexToLong() } // length = 17
    }

    // ByteArray parsing strictness

    @Test
    fun parseByteArrayRequireTwoDigitsPerByte() {
        assertContentEquals(byteArrayOf(58), "3a".hexToByteArray())
        assertFailsWith<NumberFormatException> {
            "a".hexToByteArray()
        }
        assertFailsWith<NumberFormatException> {
            "03a".hexToByteArray()
        }

        val format = HexFormat { bytes.bytePrefix = "#"; bytes.byteSuffix = ";" }
        assertContentEquals(byteArrayOf(58), "#3a;".hexToByteArray(format))
        assertFailsWith<NumberFormatException> {
            "#a;".hexToByteArray(format)
        }
        assertFailsWith<NumberFormatException> {
            "#03a;".hexToByteArray(format)
        }
    }

    @Test
    fun parseByteArrayIgnoreCase() {
        val byteArray = ByteArray(20) { it.toByte() }
        assertContentEquals(
            byteArray,
            "000102030405060708090A0B0C0D0E0F10111213".hexToByteArray()
        )
        val hexString = "0x00H bs 0x01H bS 0x02H Bs 0x03H  gs  " +
                "0x04h BS 0x05h bs 0x06h Bs 0x07h  Gs  " +
                "0X08H bS 0X09H Bs 0X0aH bs 0X0bH  gS  " +
                "0X0Ch bs 0X0Dh BS 0X0Eh Bs 0X0Fh  GS  " +
                "0x10H Bs 0x11H bS 0x12H BS 0x13H"
        val format = HexFormat {
            upperCase = true
            bytes {
                bytesPerGroup = 4
                groupSeparator = "  gs  "
                byteSeparator = " bs "
                bytePrefix = "0x"
                byteSuffix = "h"
            }
        }
        assertContentEquals(
            byteArray,
            hexString.hexToByteArray(format)
        )
    }

    @Test
    fun parseAcceptAllNewLineSequences() {
        assertContentEquals(
            ByteArray(20) { it.toByte() },
            "00010203\n04050607\r\n08090a0b\r0c0d0e0f\r\n10111213".hexToByteArray(HexFormat { bytes.bytesPerLine = 4 })
        )
    }

    @Test
    fun parseMultipleNewLines() {
        assertFailsWith<NumberFormatException> {
            "00010203\n\r04050607\r\n08090a0b\r0c0d0e0f\r\n10111213".hexToByteArray(HexFormat { bytes.bytesPerLine = 4 })
        }
        assertFailsWith<NumberFormatException> {
            "00010203\n\n04050607\r\n08090a0b\r0c0d0e0f\r\n10111213".hexToByteArray(HexFormat { bytes.bytesPerLine = 4 })
        }
        assertFailsWith<NumberFormatException> {
            "00010203\n04050607\r\n\n08090a0b\r0c0d0e0f\r\n10111213".hexToByteArray(HexFormat { bytes.bytesPerLine = 4 })
        }
    }

    @Test
    fun parseNewLineAtEnd() {
        assertFailsWith<NumberFormatException> {
            "00010203\n04050607\r\n08090a0b\r0c0d0e0f\r\n10111213\n".hexToByteArray(HexFormat { bytes.bytesPerLine = 4 })
        }
    }

    // HexFormat default options

    @Test
    fun defaultBytesOptions() {
        val byteArray = ByteArray(20) { it.toByte() }
        assertEquals(
            "000102030405060708090a0b0c0d0e0f10111213",
            byteArray.toHexString()
        )
    }

    @Test
    fun defaultBytesLineSeparator() {
        val byteArray = ByteArray(20) { it.toByte() }
        assertEquals(
            "0001020304050607\n08090a0b0c0d0e0f\n10111213",
            byteArray.toHexString(HexFormat { bytes.bytesPerLine = 8 }))
    }

    @Test
    fun defaultBytesGroupSeparator() {
        val byteArray = ByteArray(20) { it.toByte() }
        assertEquals(
            "0001020304050607  08090a0b0c0d0e0f  10111213",
            byteArray.toHexString(HexFormat { bytes.bytesPerGroup = 8 })
        )
    }

    // HexFormat corner-case options configuration

    @Test
    fun emptyBytesGroupSeparator() {
        val byteArray = ByteArray(20) { it.toByte() }
        assertEquals(
            "000102030405060708090a0b0c0d0e0f10111213",
            byteArray.toHexString(HexFormat { bytes.bytesPerGroup = 8; bytes.groupSeparator = "" })
        )
        assertEquals(
            "00 01 02 03 04 05 06 0708 09 0a 0b 0c 0d 0e 0f10 11 12 13",
            byteArray.toHexString(HexFormat { bytes.bytesPerGroup = 8; bytes.groupSeparator = ""; bytes.byteSeparator = " " })
        )
    }

    @Test
    fun bytesGroupSeparatorBiggerThanLineSeparator() {
        val byteArray = ByteArray(20) { it.toByte() }
        assertEquals(
            "0001020304050607\n08090a0b0c0d0e0f\n10111213",
            byteArray.toHexString(HexFormat { bytes.bytesPerLine = 8; bytes.bytesPerGroup = 10 })
        )
    }

    // Invalid HexFormat configuration

    @Test
    fun nonPositiveBytesPerLine() {
        assertFailsWith<IllegalArgumentException> { HexFormat { bytes.bytesPerLine = 0 } }
        assertFailsWith<IllegalArgumentException> { HexFormat { bytes.bytesPerLine = -1 } }
        assertFailsWith<IllegalArgumentException> { HexFormat { bytes.bytesPerLine = Int.MIN_VALUE } }
    }

    @Test
    fun nonPositiveBytesPerGroup() {
        assertFailsWith<IllegalArgumentException> { HexFormat { bytes.bytesPerGroup = 0 } }
        assertFailsWith<IllegalArgumentException> { HexFormat { bytes.bytesPerGroup = -1 } }
        assertFailsWith<IllegalArgumentException> { HexFormat { bytes.bytesPerGroup = Int.MIN_VALUE } }
    }
}