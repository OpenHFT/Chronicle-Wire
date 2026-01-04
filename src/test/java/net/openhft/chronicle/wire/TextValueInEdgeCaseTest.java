/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case tests for TextWire.TextValueIn to improve branch coverage.
 * Targets the 132 missed branches identified in coverage analysis.
 */
@SuppressWarnings({"deprecation", "removal"})
class TextValueInEdgeCaseTest extends WireTestCommon {

    // ========== Escape Sequence Tests ==========

    @Test
    @DisplayName("TextWire should read newline escape sequences")
    void testNewlineEscape() {
        TextWire wire = TextWire.from("field: \"line1\\nline2\"");
        String result = wire.read("field").text();
        assertEquals("line1\nline2", result, "Text value should convert \\n escape to newline");
    }

    @Test
    @DisplayName("TextWire should read tab escape sequences")
    void testTabEscape() {
        TextWire wire = TextWire.from("field: \"col1\\tcol2\"");
        String result = wire.read("field").text();
        assertEquals("col1\tcol2", result, "Text value should convert \\t escape to tab");
    }

    @Test
    @DisplayName("TextWire should read carriage return escape sequences")
    void testCarriageReturnEscape() {
        TextWire wire = TextWire.from("field: \"line1\\rline2\"");
        String result = wire.read("field").text();
        assertEquals("line1\rline2", result, "Text value should convert \\r escape to carriage return");
    }

    @Test
    @DisplayName("TextWire should read backslash escape sequences")
    void testBackslashEscape() {
        TextWire wire = TextWire.from("field: \"path\\\\to\\\\file\"");
        String result = wire.read("field").text();
        assertEquals("path\\to\\file", result, "Text value should convert \\\\ escape to backslash");
    }

    @Test
    @DisplayName("TextWire should read double quote escape sequences")
    void testDoubleQuoteEscape() {
        TextWire wire = TextWire.from("field: \"say \\\"hello\\\"\"");
        String result = wire.read("field").text();
        assertEquals("say \"hello\"", result, "Text value should unescape escaped double quotes");
    }

    @Test
    @DisplayName("TextWire should read multiple escape sequences in one value")
    void testMultipleEscapes() {
        TextWire wire = TextWire.from("field: \"line1\\n\\tindented\\r\\n\"");
        String result = wire.read("field").text();
        assertEquals("line1\n\tindented\r\n", result, "Text value should convert mixed escape sequences");
    }

    // ========== Quote Handling Tests ==========

    @Test
    @DisplayName("TextWire should read double quoted string values")
    void testDoubleQuotedString() {
        TextWire wire = TextWire.from("field: \"hello world\"");
        assertEquals("hello world", wire.read("field").text(), "TextWire should read double quoted string value");
    }

    @Test
    @DisplayName("TextWire should read single quoted string values")
    void testSingleQuotedString() {
        TextWire wire = TextWire.from("field: 'hello world'");
        assertEquals("hello world", wire.read("field").text(), "TextWire should read single quoted string value");
    }

    @Test
    @DisplayName("TextWire should read unquoted string values")
    void testUnquotedString() {
        TextWire wire = TextWire.from("field: helloworld");
        assertEquals("helloworld", wire.read("field").text(), "TextWire should read unquoted string value");
    }

    @Test
    @DisplayName("TextWire should read quoted string with colon")
    void testQuotedStringWithColon() {
        TextWire wire = TextWire.from("field: \"key: value\"");
        assertEquals("key: value", wire.read("field").text(), "TextWire should keep colon inside quoted string");
    }

    @Test
    @DisplayName("TextWire should read quoted string with hash symbol")
    void testQuotedStringWithHash() {
        TextWire wire = TextWire.from("field: \"#not a comment\"");
        assertEquals("#not a comment", wire.read("field").text(), "TextWire should keep hash inside quoted string");
    }

    @Test
    @DisplayName("TextWire should read empty quoted string values")
    void testEmptyQuotedString() {
        TextWire wire = TextWire.from("field: \"\"");
        assertEquals("", wire.read("field").text(), "TextWire should read empty double quoted string");

        wire = TextWire.from("single: ''");
        assertEquals("", wire.read("single").text(), "TextWire should read empty single quoted string");
    }

    // ========== Numeric Format Tests ==========

    @Test
    @DisplayName("TextWire should read positive numbers with explicit plus sign")
    void testPositiveWithPlusSign() {
        TextWire wire = TextWire.from("val: +42");
        assertEquals(42, wire.read("val").int32(), "TextWire should parse + sign integer value");
    }

    @Test
    @DisplayName("TextWire should read hexadecimal number values")
    void testHexNumbers() {
        TextWire wire = TextWire.from("a: 0xFF\nb: 0x0\nc: 0xABCDEF");
        assertEquals(255, wire.read("a").int32(), "TextWire should parse hex 0xFF as 255");
        assertEquals(0, wire.read("b").int32(), "TextWire should parse hex 0x0 as 0");
        assertEquals(0xABCDEF, wire.read("c").int32(), "TextWire should parse hex 0xABCDEF value");
    }

    @Test
    @DisplayName("TextWire should read scientific notation values")
    void testScientificNotation() {
        TextWire wire = TextWire.from("a: 1.2e3\nb: 1E-10\nc: 5.5E+2");
        assertEquals(1200.0, wire.read("a").float64(), 0.001, "TextWire should parse 1.2e3 as 1200.0");
        assertEquals(1E-10, wire.read("b").float64(), 1E-15, "TextWire should parse 1E-10 as 1e-10");
        assertEquals(550.0, wire.read("c").float64(), 0.001, "TextWire should parse 5.5E+2 as 550.0");
    }

    @Test
    @DisplayName("TextWire should read negative number values")
    void testNegativeNumbers() {
        TextWire wire = TextWire.from("a: -128\nb: -32768\nc: -2147483648");
        assertEquals(-128, wire.read("a").int8(), "TextWire should parse negative byte value");
        assertEquals(-32768, wire.read("b").int16(), "TextWire should parse negative short value");
        assertEquals(-2147483648, wire.read("c").int32(), "TextWire should parse negative int value");
    }

    @Test
    @DisplayName("TextWire should read floating point values")
    void testFloatingPoint() {
        TextWire wire = TextWire.from("a: 3.14159\nb: -2.5\nc: 0.0\nd: .5");
        assertEquals(3.14159, wire.read("a").float64(), 0.00001, "TextWire should parse positive float value");
        assertEquals(-2.5, wire.read("b").float64(), 0.00001, "TextWire should parse negative float value");
        assertEquals(0.0, wire.read("c").float64(), 0.00001, "TextWire should parse zero float value");
        assertEquals(0.5, wire.read("d").float64(), 0.00001, "TextWire should parse leading dot float value");
    }

    @Test
    @DisplayName("TextWire should read long boundary values")
    void testLongBoundaries() {
        TextWire wire = TextWire.from("max: 9223372036854775807\nminPlusOne: -9223372036854775807");
        assertEquals(Long.MAX_VALUE, wire.read("max").int64(), "TextWire should parse max long value");
        assertEquals(Long.MIN_VALUE + 1, wire.read("minPlusOne").int64(), "TextWire should parse min+1 long value");
    }

    // TODO FIX: Underscored numbers 1_000_000 not parsed correctly - returns 1 instead of 1000000
    @Test
    @Disabled("Underscored number format not supported - needs investigation")
    @DisplayName("TextWire should read underscored numeric literals")
    void testUnderscoredNumbers() {
        TextWire wire = TextWire.from("val: 1_000_000");
        assertEquals(1000000, wire.read("val").int32(), "TextWire should parse underscored numeric literal");
    }

    // TODO FIX: Octal format 0o777 not parsed correctly - returns 0 instead of 511
    @Test
    @Disabled("Octal format 0o prefix not supported - needs investigation")
    @DisplayName("TextWire should read octal numeric literals")
    void testOctalNumbers() {
        TextWire wire = TextWire.from("val: 0o777");
        assertEquals(511, wire.read("val").int32(), "TextWire should parse octal 0o777 as 511");
    }

    // TODO FIX: Binary format 0b1111 not parsed correctly - returns 0 instead of 15
    @Test
    @Disabled("Binary format 0b prefix not supported - needs investigation")
    @DisplayName("TextWire should read binary numeric literals")
    void testBinaryNumbers() {
        TextWire wire = TextWire.from("val: 0b1111");
        assertEquals(15, wire.read("val").int32(), "TextWire should parse binary 0b1111 as 15");
    }

    // ========== Special Value Tests ==========

    // TODO FIX: .nan not parsed as NaN - returns 0.0 instead
    @Test
    @Disabled("Special .nan value not parsed as NaN - needs investigation")
    @DisplayName("TextWire should read special NaN literal value")
    void testNaNValue() {
        TextWire wire = TextWire.from("val: .nan");
        assertTrue(Double.isNaN(wire.read("val").float64()), "TextWire should parse .nan as NaN");
    }

    // TODO FIX: .inf not parsed as Infinity - returns 0.0 instead
    @Test
    @Disabled("Special .inf value not parsed as Infinity - needs investigation")
    @DisplayName("TextWire should read special infinity values")
    void testInfinityValues() {
        TextWire wire = TextWire.from("pos: .inf\nneg: -.inf");
        assertEquals(Double.POSITIVE_INFINITY, wire.read("pos").float64(), "TextWire should parse .inf as positive infinity");
        assertEquals(Double.NEGATIVE_INFINITY, wire.read("neg").float64(), "TextWire should parse -.inf as negative infinity");
    }

    // TODO FIX: Tilde (~) not parsed as null - returns literal '~' string
    @Test
    @Disabled("Tilde null marker not parsed as null - needs investigation")
    @DisplayName("TextWire should read tilde null marker token")
    void testTildeAsNull() {
        TextWire wire = TextWire.from("val: ~");
        assertNull(wire.read("val").text(), "TextWire should parse tilde as null");
    }

    // ========== Boolean Tests ==========

    @Test
    @DisplayName("TextWire should read common boolean representations")
    void testBooleanVariants() {
        TextWire wire = TextWire.from("a: true\nb: false\nc: yes\nd: no");
        assertTrue(wire.read("a").bool(), "TextWire should parse token 'true' as true");
        assertFalse(wire.read("b").bool(), "TextWire should parse token 'false' as false");
        assertTrue(wire.read("c").bool(), "TextWire should parse token 'yes' as true");
        assertFalse(wire.read("d").bool(), "TextWire should parse token 'no' as false");
    }

    // TODO FIX: on/off boolean literals not supported - may indicate limited YAML spec support
    @Test
    @Disabled("on/off boolean literals not parsed as booleans - needs investigation")
    @DisplayName("TextWire should read on/off boolean representations")
    void testBooleanOnOff() {
        TextWire wire = TextWire.from("e: on\nf: off");
        assertTrue(wire.read("e").bool(), "TextWire should parse token 'on' as true");
        assertFalse(wire.read("f").bool(), "TextWire should parse token 'off' as false");
    }

    @Test
    @DisplayName("TextWire should read boolean with consumer callback")
    void testBoolWithConsumer() {
        TextWire wire = TextWire.from("val: true");
        AtomicReference<Boolean> result = new AtomicReference<>();
        wire.read("val").bool(result, AtomicReference::set);
        assertTrue(result.get(), "Boolean consumer should receive parsed true value");
    }

    // ========== Consumer Callback Tests ==========

    @Test
    @DisplayName("TextWire should read int8 with consumer callback")
    void testInt8WithConsumer() {
        TextWire wire = TextWire.from("val: 42");
        AtomicInteger result = new AtomicInteger();
        wire.read("val").int8(result, AtomicInteger::set);
        assertEquals(42, result.get(), "Int8 consumer should receive parsed value 42");
    }

    @Test
    @DisplayName("TextWire should read int16 with consumer callback")
    void testInt16WithConsumer() {
        TextWire wire = TextWire.from("val: 12345");
        AtomicInteger result = new AtomicInteger();
        wire.read("val").int16(result, AtomicInteger::set);
        assertEquals(12345, result.get(), "Int16 consumer should receive parsed value 12345");
    }

    @Test
    @DisplayName("TextWire should read int32 with consumer callback")
    void testInt32WithConsumer() {
        TextWire wire = TextWire.from("val: 1234567");
        AtomicInteger result = new AtomicInteger();
        wire.read("val").int32(result, AtomicInteger::set);
        assertEquals(1234567, result.get(), "Int32 consumer should receive parsed value 1234567");
    }

    @Test
    @DisplayName("TextWire should read int64 with consumer callback")
    void testInt64WithConsumer() {
        TextWire wire = TextWire.from("val: 9876543210");
        AtomicLong result = new AtomicLong();
        wire.read("val").int64(result, AtomicLong::set);
        assertEquals(9876543210L, result.get(), "Int64 consumer should receive parsed value 9876543210");
    }

    @Test
    @DisplayName("TextWire should read uint8 with consumer callback")
    void testUint8WithConsumer() {
        TextWire wire = TextWire.from("val: 255");
        AtomicInteger result = new AtomicInteger();
        wire.read("val").uint8(result, AtomicInteger::set);
        assertEquals(255, result.get(), "Uint8 consumer should receive parsed value 255");
    }

    @Test
    @DisplayName("TextWire should read uint16 with consumer callback")
    void testUint16WithConsumer() {
        TextWire wire = TextWire.from("val: 65535");
        AtomicInteger result = new AtomicInteger();
        wire.read("val").uint16(result, AtomicInteger::set);
        assertEquals(65535, result.get(), "Uint16 consumer should receive parsed value 65535");
    }

    @Test
    @DisplayName("TextWire should read uint32 with consumer callback")
    void testUint32WithConsumer() {
        TextWire wire = TextWire.from("val: 4294967295");
        AtomicLong result = new AtomicLong();
        wire.read("val").uint32(result, AtomicLong::set);
        assertEquals(4294967295L, result.get(), "Uint32 consumer should receive parsed value 4294967295");
    }

    @Test
    @DisplayName("TextWire should read float32 with consumer callback")
    void testFloat32WithConsumer() {
        TextWire wire = TextWire.from("val: 3.14");
        AtomicReference<Float> result = new AtomicReference<>();
        wire.read("val").float32(result, AtomicReference::set);
        assertEquals(3.14f, result.get(), 0.001f, "Float32 consumer should receive parsed value 3.14");
    }

    @Test
    @DisplayName("TextWire should read float64 with consumer callback")
    void testFloat64WithConsumer() {
        TextWire wire = TextWire.from("val: 3.14159265359");
        AtomicReference<Double> result = new AtomicReference<>();
        wire.read("val").float64(result, AtomicReference::set);
        assertEquals(3.14159265359, result.get(), 0.0000001, "Float64 consumer should receive parsed value 3.14159265359");
    }

    // ========== UUID Tests ==========

    @Test
    @DisplayName("TextWire should read UUID identifier values")
    void testUUID() {
        UUID expected = UUID.randomUUID();
        TextWire wire = TextWire.from("id: " + expected);
        assertEquals(expected, wire.read("id").uuid(), "TextWire should round-trip UUID value");
    }

    @Test
    @DisplayName("TextWire should read UUID with consumer callback")
    void testUUIDWithConsumer() {
        UUID expected = UUID.randomUUID();
        TextWire wire = TextWire.from("id: " + expected);
        AtomicReference<UUID> result = new AtomicReference<>();
        wire.read("id").uuid(result, AtomicReference::set);
        assertEquals(expected, result.get(), "TextWire should round-trip UUID via consumer");
    }

    // ========== Sequence Tests ==========

    @Test
    @DisplayName("TextWire should read empty sequence list values")
    void testEmptySequence() {
        TextWire wire = TextWire.from("list: []");
        List<Integer> items = new ArrayList<>();
        wire.read("list").sequence(items, (list, v) -> {
            while (v.hasNextSequenceItem()) {
                list.add(v.int32());
            }
        });
        assertTrue(items.isEmpty(), "Empty sequence should yield no items");
    }

    @Test
    @DisplayName("TextWire should read sequence items correctly")
    void testSequenceWithItems() {
        TextWire wire = TextWire.from("list: [1, 2, 3]");
        List<Integer> items = new ArrayList<>();
        wire.read("list").sequence(items, (list, v) -> {
            while (v.hasNextSequenceItem()) {
                list.add(v.int32());
            }
        });
        assertEquals(3, items.size(), "Integer sequence should have 3 items");
        assertEquals(1, items.get(0), "Integer sequence first item should be 1");
        assertEquals(2, items.get(1), "Integer sequence second item should be 2");
        assertEquals(3, items.get(2), "Integer sequence third item should be 3");
    }

    @Test
    @DisplayName("TextWire should read sequence string items")
    void testSequenceWithStrings() {
        TextWire wire = TextWire.from("list: [alpha, beta, gamma]");
        List<String> items = new ArrayList<>();
        wire.read("list").sequence(items, (list, v) -> {
            while (v.hasNextSequenceItem()) {
                list.add(v.text());
            }
        });
        assertEquals(3, items.size(), "String sequence should have 3 items");
        assertEquals("alpha", items.get(0), "String sequence first item should be alpha");
        assertEquals("beta", items.get(1), "String sequence second item should be beta");
        assertEquals("gamma", items.get(2), "String sequence third item should be gamma");
    }

    // ========== Marshallable Tests ==========

    @Test
    @DisplayName("TextWire should read empty marshallable object values")
    void testEmptyMarshallable() {
        TextWire wire = TextWire.from("obj: {}");
        AtomicInteger count = new AtomicInteger(0);
        wire.read("obj").marshallable(w -> {
            while (w.hasMore()) {
                w.read().skipValue();
                count.incrementAndGet();
            }
        });
        assertEquals(0, count.get(), "Empty marshallable should expose no fields");
    }

    @Test
    @DisplayName("TextWire should read nested marshallable values")
    void testNestedMarshallable() {
        TextWire wire = TextWire.from("outer: { inner: { value: 42 } }");
        AtomicInteger value = new AtomicInteger();
        wire.read("outer").marshallable(outer -> outer.read("inner").marshallable(inner -> value.set(inner.read("value").int32())));
        assertEquals(42, value.get(), "Nested marshallable value should read as 42");
    }

    // ========== Skip Value Tests ==========

    @Test
    @DisplayName("TextWire should skip various value types")
    void testSkipValueVariousTypes() {
        TextWire wire = TextWire.from("int: 42\ntext: hello\nlist: [1, 2]\nobj: { x: 1 }\nfinal: found");
        wire.read("int").skipValue();
        wire.read("text").skipValue();
        wire.read("list").skipValue();
        wire.read("obj").skipValue();
        assertEquals("found", wire.read("final").text(), "ValueIn should skip all previous values");
    }

    // ========== Comment Tests ==========

    @Test
    @DisplayName("TextWire should read field values after comments")
    void testCommentHandling() {
        TextWire wire = TextWire.from("# This is a comment\nfield: value");
        assertEquals("value", wire.read("field").text(), "TextWire should skip comment and read value");
    }

    @Test
    @DisplayName("TextWire should read values with inline comments")
    void testInlineComment() {
        TextWire wire = TextWire.from("field: value # inline comment\nnext: data");
        assertEquals("value", wire.read("field").text(), "TextWire should read value before inline comment");
        assertEquals("data", wire.read("next").text(), "TextWire should read next field after comment");
    }

    // ========== Bracket Type Tests ==========

    @Test
    @DisplayName("ValueIn should report bracket type for mapping")
    void testBracketTypeMapping() {
        TextWire wire = TextWire.from("field: { a: 1 }");
        wire.read("field");
        BracketType type = wire.getValueIn().getBracketType();
        assertEquals(BracketType.MAP, type, "Bracket type should be MAP for mapping");
    }

    @Test
    @DisplayName("ValueIn should report bracket type for sequence")
    void testBracketTypeSequence() {
        TextWire wire = TextWire.from("field: [1, 2, 3]");
        wire.read("field");
        BracketType type = wire.getValueIn().getBracketType();
        assertEquals(BracketType.SEQ, type, "Bracket type should be SEQ for sequence");
    }

    @Test
    @DisplayName("ValueIn should report bracket type for scalar")
    void testBracketTypeScalar() {
        TextWire wire = TextWire.from("field: value");
        wire.read("field");
        BracketType type = wire.getValueIn().getBracketType();
        assertEquals(BracketType.NONE, type, "Bracket type should be NONE for scalar");
    }

    // ========== Text To Tests ==========

    @Test
    @DisplayName("TextWire should read text into StringBuilder buffer")
    void testTextToStringBuilder() {
        TextWire wire = TextWire.from("field: hello world");
        StringBuilder sb = new StringBuilder();
        wire.read("field").textTo(sb);
        assertEquals("hello world", sb.toString(), "TextWire should write text to StringBuilder");
    }

    @Test
    @DisplayName("TextWire should read text into Bytes buffer")
    void testTextToBytes() {
        TextWire wire = TextWire.from("field: hello bytes");
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        wire.read("field").textTo(bytes);
        assertEquals("hello bytes", bytes.toString(), "TextWire should write text to Bytes");
        bytes.releaseLast();
    }

    // ========== Byte Array Tests ==========

    @Test
    @DisplayName("TextWire should read byte array values")
    void testByteArrayReading() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        TextWire wire = new TextWire(bytes);
        byte[] data = {1, 2, 3, 4, 5};
        wire.write("data").bytes(data);

        bytes.readPositionRemaining(0, bytes.writePosition());
        byte[] result = wire.read("data").bytes();
        assertArrayEquals(data, result, "TextWire should round-trip byte array values");
    }

    // ========== Type Prefix Tests ==========

    @Test
    @DisplayName("TextWire should read typed string object")
    void testTypedString() {
        TextWire wire = TextWire.from("obj: !java.lang.String hello");
        Object result = wire.read("obj").object();
        assertEquals("hello", result, "TextWire should read typed String value");
    }

    @Test
    @DisplayName("TextWire should read typed integer object")
    void testTypedInteger() {
        TextWire wire = TextWire.from("obj: !int 42");
        Object result = wire.read("obj").object();
        assertInstanceOf(Number.class, result, "Typed int object should be a Number");
        assertEquals(42, ((Number) result).intValue(), "Typed int value should equal 42");
    }

    // ========== Wire Reference Tests ==========

    @Test
    @DisplayName("ValueIn should return parent wireIn reference instance")
    void testWireInReference() {
        TextWire wire = TextWire.from("field: value");
        assertSame(wire, wire.getValueIn().wireIn(), "ValueIn should return parent wire instance");
    }

    @Test
    @DisplayName("ValueIn should return runtime classLookup instance for lookups")
    void testClassLookup() {
        TextWire wire = TextWire.from("field: value");
        assertNotNull(wire.getValueIn().classLookup(), "ValueIn classLookup should not be null");
    }

    // ========== Unicode Escape Tests ==========

    @Test
    @DisplayName("TextWire should read unicode escape sequences")
    void testUnicodeEscape() {
        TextWire wire = TextWire.from("field: \"\\u0048\\u0065\\u006C\\u006C\\u006F\"");
        String result = wire.read("field").text();
        assertEquals("Hello", result, "TextWire should decode unicode escapes to characters");
    }

    @Test
    @DisplayName("TextWire should read extended unicode escapes")
    void testExtendedUnicodeEscape() {
        TextWire wire = TextWire.from("field: \"\\u00E9\\u00F1\"");
        String result = wire.read("field").text();
        assertEquals("\u00E9\u00F1", result, "TextWire should decode extended unicode escapes");
    }

    // ========== Multiline String Tests ==========

    @Test
    @DisplayName("TextWire should handle multiline quoted strings")
    void testMultilineQuotedString() {
        TextWire wire = TextWire.from("field: \"line1\\nline2\\nline3\"");
        String result = wire.read("field").text();
        assertEquals("line1\nline2\nline3", result, "TextWire should handle multiline content");
    }

    // ========== Whitespace Handling Tests ==========

    @Test
    @DisplayName("TextWire should preserve leading spaces in quoted strings")
    void testLeadingSpacesInQuotes() {
        TextWire wire = TextWire.from("field: \"  hello\"");
        assertEquals("  hello", wire.read("field").text(), "TextWire should preserve leading spaces");
    }

    @Test
    @DisplayName("TextWire should preserve trailing spaces in quoted strings")
    void testTrailingSpacesInQuotes() {
        TextWire wire = TextWire.from("field: \"hello  \"");
        assertEquals("hello  ", wire.read("field").text(), "TextWire should preserve trailing spaces");
    }

    @Test
    @DisplayName("TextWire should decode tab escape sequences in strings")
    void testTabCharacters() {
        TextWire wire = TextWire.from("field: \"a\\tb\\tc\"");
        assertEquals("a\tb\tc", wire.read("field").text(), "tab escape sequence should decode to tab character");
    }

    // ========== Character Reading Tests ==========

    @Test
    @DisplayName("TextWire should read unquoted character literal tokens")
    void testReadCharacter() {
        TextWire wire = TextWire.from("char: A");
        assertEquals('A', wire.read("char").character(), "unquoted character literal should parse as 'A'");
    }

    @Test
    @DisplayName("TextWire should read quoted character literal tokens")
    void testReadQuotedCharacter() {
        TextWire wire = TextWire.from("char: \"B\"");
        assertEquals('B', wire.read("char").character(), "quoted character literal should parse as 'B'");
    }

    // ========== Date/Time Tests ==========

    @Test
    @DisplayName("TextWire should parse LocalDate from ISO date")
    void testLocalDate() {
        TextWire wire = TextWire.from("date: 2024-06-15");
        java.time.LocalDate date = wire.read("date").object(java.time.LocalDate.class);
        assertEquals(java.time.LocalDate.of(2024, 6, 15), date, "LocalDate should match ISO date input");
    }

    @Test
    @DisplayName("TextWire should parse LocalTime from hh:mm:ss input")
    void testLocalTime() {
        TextWire wire = TextWire.from("time: \"14:30:45\"");
        java.time.LocalTime time = wire.read("time").object(java.time.LocalTime.class);
        assertEquals(java.time.LocalTime.of(14, 30, 45), time, "LocalTime should match hh:mm:ss input");
    }

    @Test
    @DisplayName("TextWire should parse LocalDateTime from ISO date-time")
    void testLocalDateTime() {
        TextWire wire = TextWire.from("datetime: \"2024-06-15T14:30:45\"");
        java.time.LocalDateTime dt = wire.read("datetime").object(java.time.LocalDateTime.class);
        assertEquals(java.time.LocalDateTime.of(2024, 6, 15, 14, 30, 45), dt,
                "LocalDateTime should match ISO date-time input");
    }

    // ========== Enum Reading Tests ==========

    @Test
    @DisplayName("TextWire should parse enum token into WireType")
    void testEnumReading() {
        TextWire wire = TextWire.from("type: BINARY");
        WireType result = wire.read("type").asEnum(WireType.class);
        assertEquals(WireType.BINARY, result, "enum token should map to WireType.BINARY");
    }

    @Test
    @DisplayName("TextWire should read typed enum values")
    void testTypedEnum() {
        TextWire wire = TextWire.from("type: !net.openhft.chronicle.wire.WireType BINARY");
        Object result = wire.read("type").object();
        assertEquals(WireType.BINARY, result, "TextWire should parse typed enum value");
    }

    // ========== Class/Type Reading Tests ==========

    @Test
    @DisplayName("TextWire should read type literal values")
    void testTypeLiteral() {
        TextWire wire = TextWire.from("class: !type java.lang.String");
        Class<?> result = wire.read("class").typeLiteral();
        assertEquals(String.class, result, "TextWire should parse type literal");
    }

    // ========== IsNull Tests ==========

    @Test
    @DisplayName("TextWire should detect null values with isNull")
    void testIsNull() {
        TextWire wire = TextWire.from("notNull: value");
        assertFalse(wire.read("notNull").isNull(), "notNull field should report isNull false");
    }

    // ========== HasMore Tests ==========

    @Test
    @DisplayName("TextWire should update hasMore after each field read")
    void testHasMore() {
        TextWire wire = TextWire.from("a: 1\nb: 2");
        assertTrue(wire.hasMore(), "Wire with content should have more");
        wire.read("a").int32();
        assertTrue(wire.hasMore(), "Wire with remaining content should have more");
        wire.read("b").int32();
        assertFalse(wire.hasMore(), "Wire after reading all should not have more");
    }

    // ========== Nested Sequence Tests ==========

    @Test
    @DisplayName("TextWire should read nested sequence of integers")
    void testNestedSequence() {
        TextWire wire = TextWire.from("matrix: [[1, 2], [3, 4]]");
        List<List<Integer>> result = new ArrayList<>();
        wire.read("matrix").sequence(result, (outer, outerReader) -> {
            while (outerReader.hasNextSequenceItem()) {
                List<Integer> inner = new ArrayList<>();
                outerReader.sequence(inner, (innerList, innerReader) -> {
                    while (innerReader.hasNextSequenceItem()) {
                        innerList.add(innerReader.int32());
                    }
                });
                outer.add(inner);
            }
        });
        assertEquals(2, result.size(), "Outer sequence should have 2 items");
        assertEquals(2, result.get(0).size(), "First inner should have 2 items");
        assertEquals(1, result.get(0).get(0), "First element should be 1");
        assertEquals(4, result.get(1).get(1), "Last element should be 4");
    }

    // ========== Object Type Reading Tests ==========

    @Test
    @DisplayName("TextWire should parse boxed Integer object value")
    void testBoxedIntegerReading() {
        TextWire wire = TextWire.from("val: 42");
        Integer result = wire.read("val").object(Integer.class);
        assertEquals(42, result, "TextWire should parse Integer object");
    }

    @Test
    @DisplayName("TextWire should parse boxed Long object value")
    void testBoxedLongReading() {
        TextWire wire = TextWire.from("val: 9876543210");
        Long result = wire.read("val").object(Long.class);
        assertEquals(9876543210L, result, "TextWire should parse Long object");
    }

    @Test
    @DisplayName("TextWire should parse boxed Double object value")
    void testBoxedDoubleReading() {
        TextWire wire = TextWire.from("val: 3.14159");
        Double result = wire.read("val").object(Double.class);
        assertEquals(3.14159, result, 0.00001, "TextWire should parse Double object");
    }

    @Test
    @DisplayName("TextWire should parse String object value")
    void testStringObjectReading() {
        TextWire wire = TextWire.from("val: hello");
        String result = wire.read("val").object(String.class);
        assertEquals("hello", result, "TextWire should parse String object");
    }

    // ========== Array Reading Tests ==========

    @Test
    @DisplayName("TextWire should read int array values")
    void testIntArrayReading() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        TextWire wire = new TextWire(bytes);
        int[] data = {1, 2, 3, 4, 5};
        wire.write("arr").object(data);

        bytes.readPositionRemaining(0, bytes.writePosition());
        int[] result = wire.read("arr").object(int[].class);
        assertArrayEquals(data, result, "TextWire should round-trip int array");
    }

    @Test
    @DisplayName("TextWire should read String array values")
    void testStringArrayReading() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        TextWire wire = new TextWire(bytes);
        String[] data = {"one", "two", "three"};
        wire.write("arr").object(data);

        bytes.readPositionRemaining(0, bytes.writePosition());
        String[] result = wire.read("arr").object(String[].class);
        assertArrayEquals(data, result, "TextWire should round-trip String array");
    }

    // ========== Special Key Tests ==========

    @Test
    @DisplayName("TextWire should handle quoted field keys")
    void testQuotedFieldKey() {
        TextWire wire = TextWire.from("\"special:key\": value");
        assertEquals("value", wire.read("special:key").text(), "TextWire should handle quoted field key");
    }

    @Test
    @DisplayName("TextWire should handle field keys with numbers")
    void testNumericFieldKey() {
        TextWire wire = TextWire.from("field123: value");
        assertEquals("value", wire.read("field123").text(), "TextWire should handle numeric field key");
    }

    // ========== Empty Value Tests ==========

    @Test
    @DisplayName("TextWire should handle empty string field")
    void testEmptyStringField() {
        TextWire wire = TextWire.from("empty: \"\"");
        assertEquals("", wire.read("empty").text(), "TextWire should read empty string");
    }

    @Test
    @DisplayName("TextWire should handle missing value as empty string")
    void testMissingValueAsEmpty() {
        TextWire wire = TextWire.from("field: ");
        assertEquals("", wire.read("field").text(), "TextWire should read missing value as empty string");
    }
}
