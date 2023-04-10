/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text

import kotlin.test.*

class BytesHexFormatTest {
    companion object {
        val fourBytes = ByteArray(4) { (10 + it).toByte() }
        val twentyBytes = ByteArray(20) { it.toByte() }
    }

    private fun testDefaultFormatAndParse(bytes: ByteArray, expected: String) {
        assertEquals(expected, bytes.toHexString())
        assertContentEquals(bytes, expected.hexToByteArray())
    }

    private fun testFormatAndParse(bytes: ByteArray, expected: String, format: HexFormat) {
        assertEquals(expected, bytes.toHexString(format))
        assertContentEquals(bytes, expected.hexToByteArray(format))
    }

    @Test
    fun ignoreNumberFormat() {
        val format = HexFormat {
            number {
                prefix = "0x"
                suffix = "h"
                removeLeadingZeros = true
            }
        }

        testFormatAndParse(fourBytes, "0a0b0c0d", format)
    }

    @Test
    fun upperCase() {
        testFormatAndParse(fourBytes, "0A0B0C0D", HexFormat { upperCase = true })
        testFormatAndParse(fourBytes, "0A0B0C0D", HexFormat.UpperCase)
    }

    @Test
    fun lowerCase() {
        testFormatAndParse(fourBytes, "0a0b0c0d", HexFormat { upperCase = false })
    }

    @Test
    fun defaultCase() {
        testDefaultFormatAndParse(fourBytes, "0a0b0c0d")
        testFormatAndParse(fourBytes, "0a0b0c0d", HexFormat.Default)
    }

    @Test
    fun byteSeparatorPrefixSuffix() {
        // byteSeparator
        testFormatAndParse(fourBytes, "0a 0b 0c 0d", HexFormat { bytes.byteSeparator = " " })
        // bytePrefix
        testFormatAndParse(fourBytes, "0x0a0x0b0x0c0x0d", HexFormat { bytes.bytePrefix = "0x" })
        // bytePrefix
        testFormatAndParse(fourBytes, "0a;0b;0c;0d;", HexFormat { bytes.byteSuffix = ";" })
        // all together
        testFormatAndParse(fourBytes, "0x0a; 0x0b; 0x0c; 0x0d;", HexFormat {
            bytes {
                byteSeparator = " "
                bytePrefix = "0x"
                byteSuffix = ";"
            }
        })
    }

    @Test
    fun bytesPerLine() {
        // Fewer bytes in the last line
        run {
            val format = HexFormat { bytes.bytesPerLine = 8 }
            val expected = "0001020304050607\n08090a0b0c0d0e0f\n10111213"
            testFormatAndParse(twentyBytes, expected, format)
        }

        // The last line is not ended by the line separator
        run {
            val format = HexFormat { bytes.bytesPerLine = 4 }
            val expected = "00010203\n04050607\n08090a0b\n0c0d0e0f\n10111213"
            testFormatAndParse(twentyBytes, expected, format)
        }
    }

    @Test
    fun bytesPerGroup() {
        // The default group separator, and fewer bytes in the last group
        run {
            val format = HexFormat { bytes.bytesPerGroup = 8 }
            val expected = "0001020304050607  08090a0b0c0d0e0f  10111213"
            testFormatAndParse(twentyBytes, expected, format)
        }

        // The specified group separator
        run {
            val format = HexFormat {
                bytes {
                    bytesPerGroup = 8
                    groupSeparator = "---"
                }
            }
            val expected = "0001020304050607---08090a0b0c0d0e0f---10111213"
            testFormatAndParse(twentyBytes, expected, format)
        }

        // The last group is not ended by the group separator
        run {
            val format = HexFormat { bytes.bytesPerGroup = 4 }
            val expected = "00010203  04050607  08090a0b  0c0d0e0f  10111213"
            testFormatAndParse(twentyBytes, expected, format)
        }
    }

    @Test
    fun bytesPerLineAndBytesPerGroup() {
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

        testFormatAndParse(byteArray, expected, format)
    }

    @Test
    fun macAddress() {
        val address = byteArrayOf(0x00, 0x1b, 0x63, 0x84.toByte(), 0x45, 0xe6.toByte())

        testFormatAndParse(
            address,
            "00:1b:63:84:45:e6",
            HexFormat { bytes.byteSeparator = ":" }
        )
        testFormatAndParse(
            address,
            "00-1B-63-84-45-E6",
            HexFormat { upperCase = true; bytes.byteSeparator = "-" }
        )
        testFormatAndParse(
            address,
            "001B.6384.45E6",
            HexFormat { upperCase = true; bytes.bytesPerGroup = 2; bytes.groupSeparator = "." }
        )
    }

    // Parsing strictness

    @Test
    fun parseRequiresTwoDigitsPerByte() {
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

        assertFailsWith<NumberFormatException> {
            "0a0b0c0".hexToByteArray()
        }
    }

    @Test
    fun parseIgnoresCase() {
        assertContentEquals(
            twentyBytes,
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
            twentyBytes,
            hexString.hexToByteArray(format)
        )
    }

    @Test
    fun parseAcceptsAllNewLineSequences() {
        assertContentEquals(
            twentyBytes,
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

    // HexFormat corner-case options configuration

    @Test
    fun emptyGroupSeparator() {
        testFormatAndParse(
            twentyBytes,
            "000102030405060708090a0b0c0d0e0f10111213",
            HexFormat { bytes.bytesPerGroup = 8; bytes.groupSeparator = "" }
        )
        testFormatAndParse(
            twentyBytes,
            "00 01 02 03 04 05 06 0708 09 0a 0b 0c 0d 0e 0f10 11 12 13",
            HexFormat { bytes.bytesPerGroup = 8; bytes.groupSeparator = ""; bytes.byteSeparator = " " }
        )
    }

    @Test
    fun groupSeparatorBiggerThanLineSeparator() {
        val format = HexFormat { bytes.bytesPerLine = 8; bytes.bytesPerGroup = 10 }
        val expected = "0001020304050607\n08090a0b0c0d0e0f\n10111213"
        testFormatAndParse(twentyBytes, expected, format)
    }

    @Test
    fun groupSeparatorWithNewLine() {
        val format = HexFormat { bytes.bytesPerLine = 8; bytes.bytesPerGroup = 3; bytes.groupSeparator = "\n" }
        val expected = "000102\n030405\n0607\n08090a\n0b0c0d\n0e0f\n101112\n13"
        testFormatAndParse(twentyBytes, expected, format)
    }

    @Test
    fun byteSeparatorWithNewLine() {
        val format = HexFormat { bytes.bytesPerLine = 8; bytes.byteSeparator = "\n" }
        val expected = "00\n01\n02\n03\n04\n05\n06\n07\n08\n09\n0a\n0b\n0c\n0d\n0e\n0f\n10\n11\n12\n13"
        testFormatAndParse(twentyBytes, expected, format)
    }

//    @Test
//    fun bytePrefixWithNewLine() {
//        // line separator is '\r', byte prefix is '\n'
//        val format = HexFormat { bytes.bytesPerLine = 8; bytes.bytePrefix = "\n" }
//        val expected = "\n00\n01\n02\n03\n04\n05\n06\n07\r\n08\n09\n0a\n0b\n0c\n0d\n0e\n0f\r\n10\n11\n12\n13"
//        assertContentEquals(twentyBytes, expected.hexToByteArray(format))
//    }

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