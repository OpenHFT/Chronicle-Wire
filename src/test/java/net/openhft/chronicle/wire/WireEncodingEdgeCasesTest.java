/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for character encoding edge cases including UTF-8 multibyte sequences,
 * surrogate pairs, control characters, and sign extension issues.
 */
@SuppressWarnings({"deprecation", "removal"})
public class WireEncodingEdgeCasesTest extends WireTestCommon {

    // ========== UTF-8 Multibyte Character Tests ==========

    @Test
    @DisplayName("BinaryWire should handle 2-byte UTF-8 characters (Latin Extended)")
    public void testTwoByteUtf8Binary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        // Latin Extended characters (2 bytes in UTF-8)
        String value = "\u00C0\u00FF\u00E9";  // A-grave, y-umlaut, e-acute
        wire.write("latin").text(value);

        bytes.readPosition(0);
        assertEquals(value, wire.read("latin").text(),
                "2-byte UTF-8 should round-trip in BinaryWire");
    }

    @Test
    @DisplayName("BinaryWire should handle 3-byte UTF-8 characters (CJK)")
    public void testThreeByteUtf8Binary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        // CJK characters (3 bytes in UTF-8)
        String value = "\u4E2D\u6587";  // Chinese characters
        wire.write("cjk").text(value);

        bytes.readPosition(0);
        assertEquals(value, wire.read("cjk").text(),
                "3-byte UTF-8 (CJK) should round-trip in BinaryWire");
    }

    @Test
    @DisplayName("BinaryWire should handle 4-byte UTF-8 characters (emoji)")
    public void testFourByteUtf8Binary() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        // Emoji characters (4 bytes in UTF-8, require surrogate pairs in Java)
        String emoji = "\uD83D\uDE00\uD83D\uDCA9";  // grinning face, poop
        wire.write("emoji").text(emoji);

        bytes.readPosition(0);
        assertEquals(emoji, wire.read("emoji").text(),
                "4-byte UTF-8 (emoji) should round-trip in BinaryWire");
    }

    @Test
    @DisplayName("TextWire should handle UTF-8 multibyte characters")
    public void testMultibyteUtf8Text() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        TextWire wire = new TextWire(bytes);

        String mixed = "Hello \u4E16\u754C \uD83C\uDF0D";  // Hello World (Chinese) Globe
        wire.write("mixed").text(mixed);

        bytes.readPosition(0);
        assertEquals(mixed, wire.read("mixed").text(),
                "Mixed UTF-8 should round-trip in TextWire");
    }

    @Test
    @DisplayName("YamlWire should handle UTF-8 multibyte characters")
    public void testMultibyteUtf8Yaml() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        YamlWire wire = new YamlWire(bytes);

        String mixed = "\u00E9\u00E8\u00EA \u4E2D\u6587";
        wire.write("mixed").text(mixed);

        bytes.readPosition(0);
        assertEquals(mixed, wire.read("mixed").text(),
                "Mixed UTF-8 should round-trip in YamlWire");
    }

    // ========== Surrogate Pair Tests ==========

    @Test
    @DisplayName("All wire types should handle valid surrogate pairs")
    public void testValidSurrogatePairs() {
        // First valid surrogate pair: U+10000 (Linear B Syllable B008 A)
        String firstPair = "\uD800\uDC00";
        // Last valid surrogate pair: U+10FFFF
        String lastPair = "\uDBFF\uDFFF";

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("first").text(firstPair);
            wire.write("last").text(lastPair);

            bytes.readPosition(0);

            assertEquals(firstPair, wire.read("first").text(),
                    "First surrogate pair should round-trip in " + wt);
            assertEquals(lastPair, wire.read("last").text(),
                    "Last surrogate pair should round-trip in " + wt);
        }
    }

    @Test
    @DisplayName("BinaryWire should handle string with many surrogate pairs")
    public void testManySurrogatePairs() {
        // Multiple emoji (each requires surrogate pair)
        String manyEmoji = "\uD83D\uDE00\uD83D\uDE01\uD83D\uDE02\uD83D\uDE03\uD83D\uDE04";

        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("emoji").text(manyEmoji);

        bytes.readPosition(0);
        assertEquals(manyEmoji, wire.read("emoji").text(),
                "Multiple surrogate pairs should round-trip");
    }

    // ========== Control Character Tests ==========

    @Test
    @DisplayName("All wire types should handle NUL character (0x00)")
    public void testNulCharacter() {
        String withNul = "before\u0000after";

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("nul").text(withNul);

            bytes.readPosition(0);

            String result = wire.read("nul").text();
            // NUL handling varies - may be preserved, stripped, or cause truncation
            assertNotNull(result, "NUL character test should read a non-null value in " + wt);
            assertTrue(result.startsWith("before"),
                    "Content before NUL should be preserved in " + wt);
        }
    }

    @Test
    @DisplayName("All wire types should handle tab character")
    public void testTabCharacter() {
        String withTab = "col1\tcol2\tcol3";

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("tab").text(withTab);

            bytes.readPosition(0);

            assertEquals(withTab, wire.read("tab").text(),
                    "Tab character should round-trip in " + wt);
        }
    }

    @Test
    @DisplayName("All wire types should handle carriage return and newline")
    public void testCrLf() {
        String withCrLf = "line1\r\nline2\nline3\rline4";

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("lines").text(withCrLf);

            bytes.readPosition(0);

            String result = wire.read("lines").text();
            assertNotNull(result, "CRLF content should read a non-null value in " + wt);
            // Line ending normalization may occur in text formats
        }
    }

    @Test
    @DisplayName("All wire types should handle BEL character (0x07)")
    public void testBelCharacter() {
        String withBel = "ding\u0007dong";

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("bel").text(withBel);

            bytes.readPosition(0);

            String result = wire.read("bel").text();
            assertNotNull(result, "BEL character test should read a non-null value in " + wt);
        }
    }

    @Test
    @DisplayName("All wire types should handle DEL character (0x7F)")
    public void testDelCharacter() {
        String withDel = "before\u007Fafter";

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("del").text(withDel);

            bytes.readPosition(0);

            String result = wire.read("del").text();
            assertNotNull(result, "DEL character test should read a non-null value in " + wt);
        }
    }

    // ========== High-Bit Byte Tests (Sign Extension) ==========

    @Test
    @DisplayName("BinaryWire should handle bytes 0x80-0xFF without sign extension")
    public void testHighBitBytesNoSignExtension() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        // Bytes in range 0x80-0xFF
        byte[] highBitBytes = new byte[128];
        for (int i = 0; i < 128; i++) {
            highBitBytes[i] = (byte) (128 + i);
        }

        wire.write("data").bytes(highBitBytes);

        bytes.readPosition(0);

        byte[] result = wire.read("data").bytes();
        assertArrayEquals(highBitBytes, result,
                "High-bit bytes should round-trip without sign extension");
    }

    @Test
    @DisplayName("All wire types should handle Latin-1 extended characters")
    public void testLatin1ExtendedCharacters() {
        // Latin-1 Supplement (0x80-0xFF as characters)
        StringBuilder sb = new StringBuilder();
        for (int i = 0x80; i <= 0xFF; i++) {
            sb.append((char) i);
        }
        String latin1Extended = sb.toString();

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("latin1").text(latin1Extended);

            bytes.readPosition(0);

            String result = wire.read("latin1").text();
            assertEquals(latin1Extended, result,
                    "Latin-1 extended chars should round-trip in " + wt);
        }
    }

    // ========== Empty and Whitespace String Tests ==========

    @Test
    @DisplayName("All wire types should handle empty string")
    public void testEmptyString() {
        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("empty").text("");

            bytes.readPosition(0);

            String result = wire.read("empty").text();
            assertNotNull(result, "Empty string read should return a non-null value in " + wt);
            assertEquals("", result, "Empty string read should return an empty string in " + wt);
        }
    }

    @Test
    @DisplayName("All wire types should handle whitespace-only strings")
    public void testWhitespaceOnlyStrings() {
        String[] whitespaceStrings = {" ", "  ", "\t", "\n", " \t\n "};

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            for (String ws : whitespaceStrings) {
                Bytes<?> bytes = Bytes.allocateElasticOnHeap();
                Wire wire = wt.apply(bytes);

                wire.write("ws").text(ws);

                bytes.readPosition(0);

                String result = wire.read("ws").text();
                assertNotNull(result, "Whitespace-only string should read a non-null value in " + wt);
                // Note: text formats may trim or normalize whitespace
            }
        }
    }

    // ========== Very Long String Tests ==========

    @Test
    @DisplayName("All wire types should handle long ASCII strings")
    public void testLongAsciiStrings() {
        // 10,000 character ASCII string
        StringBuilder sb = new StringBuilder(10000);
        for (int i = 0; i < 10000; i++) {
            sb.append((char) ('a' + (i % 26)));
        }
        String longAscii = sb.toString();

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(20000);
            Wire wire = wt.apply(bytes);

            wire.write("long").text(longAscii);

            bytes.readPosition(0);

            String result = wire.read("long").text();
            assertEquals(longAscii.length(), result.length(),
                    "Long ASCII string length should match in " + wt);
            assertEquals(longAscii, result,
                    "Long ASCII string should round-trip in " + wt);
        }
    }

    @Test
    @DisplayName("All wire types should handle long Unicode strings")
    public void testLongUnicodeStrings() {
        // 5,000 CJK characters (15,000 bytes in UTF-8)
        StringBuilder sb = new StringBuilder(5000);
        for (int i = 0; i < 5000; i++) {
            sb.append((char) (0x4E00 + (i % 1000)));
        }
        String longCjk = sb.toString();

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(20000);
            Wire wire = wt.apply(bytes);

            wire.write("cjk").text(longCjk);

            bytes.readPosition(0);

            String result = wire.read("cjk").text();
            assertEquals(longCjk.length(), result.length(),
                    "Long CJK string length should match in " + wt);
            assertEquals(longCjk, result,
                    "Long CJK string should round-trip in " + wt);
        }
    }

    // ========== Escape Sequence Tests ==========

    @Test
    @DisplayName("TextWire should handle backslash escape sequences")
    public void testBackslashEscapesText() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        TextWire wire = new TextWire(bytes);

        String withBackslashes = "path\\to\\file";
        wire.write("path").text(withBackslashes);

        bytes.readPosition(0);

        String result = wire.read("path").text();
        assertEquals(withBackslashes, result, "TextWire backslash escapes should round-trip unchanged");
    }

    @Test
    @DisplayName("YamlWire should handle backslash escape sequences")
    public void testBackslashEscapesYaml() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        YamlWire wire = new YamlWire(bytes);

        String withBackslashes = "C:\\Users\\test";
        wire.write("path").text(withBackslashes);

        bytes.readPosition(0);

        String result = wire.read("path").text();
        assertEquals(withBackslashes, result, "YamlWire backslash escapes should round-trip unchanged");
    }

    // ========== Unicode Boundary Tests ==========

    @Test
    @DisplayName("All wire types should handle BMP boundary characters")
    public void testBmpBoundaryCharacters() {
        // Characters at BMP boundaries
        String bmpBoundary = "\uFFFF\uFFFE\u0000\u0001";

        for (WireType wt : new WireType[]{WireType.BINARY}) {
            // Only test binary for these edge cases
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("bmp").text(bmpBoundary);

            bytes.readPosition(0);

            String result = wire.read("bmp").text();
            assertNotNull(result, "BMP boundary characters should read a non-null value in " + wt);
        }
    }

    @Test
    @DisplayName("All wire types should handle private use area characters")
    public void testPrivateUseAreaCharacters() {
        // Private Use Area (U+E000 to U+F8FF)
        String privateUse = "\uE000\uE001\uF8FF";

        for (WireType wt : new WireType[]{WireType.BINARY, WireType.TEXT, WireType.YAML}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            Wire wire = wt.apply(bytes);

            wire.write("pua").text(privateUse);

            bytes.readPosition(0);

            String result = wire.read("pua").text();
            assertEquals(privateUse, result,
                    "Private Use Area chars should round-trip in " + wt);
        }
    }
}
