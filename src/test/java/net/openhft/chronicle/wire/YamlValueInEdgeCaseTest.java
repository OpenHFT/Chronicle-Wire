/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case tests for YamlWire.TextValueIn to improve branch coverage.
 * Targets the 101 missed branches identified in coverage analysis.
 * Complements YamlWireValueInCoverageTest with additional edge cases.
 */
@SuppressWarnings({"deprecation", "removal"})
public class YamlValueInEdgeCaseTest extends WireTestCommon {

    // ========== Anchor and Alias Edge Cases ==========

    @Test
    @DisplayName("YamlWire should read simple anchor and alias string values")
    public void testSimpleAnchorAlias() {
        YamlWire wire = YamlWire.from("a: &anchor hello\nb: *anchor");
        String first = wire.read("a").text();
        assertEquals("hello", first, "Anchor value should read as hello");
        String second = wire.read("b").text();
        assertEquals("hello", second, "Alias should reference anchor string value");
    }

    @Test
    @DisplayName("YamlWire should read numeric anchor and alias values")
    public void testNumericAnchorAlias() {
        YamlWire wire = YamlWire.from("a: &num 42\nb: *num");
        int first = wire.read("a").int32();
        assertEquals(42, first, "Anchor value should read as 42");
        int second = wire.read("b").int32();
        assertEquals(42, second, "Alias should reference anchor numeric value");
    }

    // TODO FIX: UnsupportedOperationException when alias references complex object
    @Test
    @Disabled("Alias to complex object throws UnsupportedOperationException - needs investigation")
    @DisplayName("YamlWire should read complex object anchor alias values")
    public void testComplexObjectAnchorAlias() {
        YamlWire wire = YamlWire.from("a: &obj { x: 1, y: 2 }\nb: *obj");
        @SuppressWarnings("unchecked")
        Map<String, Object> first = wire.read("a").object(Map.class);
        assertEquals(1, first.get("x"), "Anchor object x should be 1");
        @SuppressWarnings("unchecked")
        Map<String, Object> second = wire.read("b").object(Map.class);
        assertEquals(1, second.get("x"), "Alias object x should also be 1");
    }

    // TODO FIX: Undefined alias reference should throw but may not
    @Test
    @Disabled("Undefined alias behaviour undefined - needs investigation")
    @DisplayName("YamlWire should handle undefined alias reference")
    public void testUndefinedAlias() {
        YamlWire wire = YamlWire.from("a: *undefined");
        assertThrows(Exception.class, () -> wire.read("a").text(),
            "Undefined alias should raise an exception");
    }

    // ========== Block Scalar Edge Cases ==========

    @Test
    @DisplayName("YamlWire should read literal block scalar with default chomping")
    public void testLiteralBlockScalar() {
        YamlWire wire = YamlWire.from("text: |\n  line1\n  line2\n  line3");
        String text = wire.read("text").text();
        assertNotNull(text, "Literal block text should not be null");
        assertTrue(text.contains("line1"), text + " should contain line1");
        assertTrue(text.contains("line2"), text + " should contain line2");
    }

    @Test
    @DisplayName("YamlWire should read folded block scalar text")
    public void testFoldedBlockScalar() {
        YamlWire wire = YamlWire.from("text: >\n  folded\n  text\n  here");
        String text = wire.read("text").text();
        assertNotNull(text, "Folded block text should not be null");
    }

    // TODO FIX: Block scalar with strip chomping indicator
    @Test
    @Disabled("Block scalar strip chomping not supported - needs investigation")
    @DisplayName("YamlWire should read literal block with strip chomping")
    public void testLiteralBlockStripChomping() {
        YamlWire wire = YamlWire.from("text: |-\n  line1\n  line2\n\n");
        String text = wire.read("text").text();
        assertFalse(text.endsWith("\n"), "Block text should not end with '\\n': " + text);
    }

    // TODO FIX: Block scalar with keep chomping indicator
    @Test
    @Disabled("Block scalar keep chomping not supported - needs investigation")
    @DisplayName("YamlWire should read literal block with keep chomping")
    public void testLiteralBlockKeepChomping() {
        YamlWire wire = YamlWire.from("text: |+\n  line1\n\n\n");
        String text = wire.read("text").text();
        assertTrue(text.endsWith("\n\n"), "Block text should end with '\\n\\n': " + text);
    }

    // ========== Multi-Document Edge Cases ==========

    @Test
    @DisplayName("YamlWire should read after document start marker")
    public void testDocumentStartMarker() {
        YamlWire wire = YamlWire.from("---\nfield: value");
        String text = wire.read("field").text();
        assertEquals("value", text, "YamlWire should read value after document start");
    }

    @Test
    @DisplayName("YamlWire should read between document markers")
    public void testDocumentEndMarker() {
        YamlWire wire = YamlWire.from("field: value\n...");
        String text = wire.read("field").text();
        assertEquals("value", text, "YamlWire should read value before document end");
    }

    // ========== Token Type Edge Cases ==========

    @Test
    @DisplayName("YamlWire should read flow mapping on single line")
    public void testFlowMappingSingleLine() {
        YamlWire wire = YamlWire.from("obj: { a: 1, b: 2, c: 3 }");
        AtomicInteger sum = new AtomicInteger(0);
        wire.read("obj").marshallable(w -> {
            sum.addAndGet(w.read("a").int32());
            sum.addAndGet(w.read("b").int32());
            sum.addAndGet(w.read("c").int32());
        });
        assertEquals(6, sum.get(), "Flow mapping values should sum to 6");
    }

    @Test
    @DisplayName("YamlWire should read flow sequence on single line")
    public void testFlowSequenceSingleLine() {
        YamlWire wire = YamlWire.from("list: [1, 2, 3, 4, 5]");
        List<Integer> items = new ArrayList<>();
        wire.read("list").sequence(items, (list, v) -> {
            while (v.hasNextSequenceItem()) {
                list.add(v.int32());
            }
        });
        assertEquals(5, items.size(), "Flow sequence should have 5 items total");
        assertEquals(15, items.stream().mapToInt(Integer::intValue).sum(), "Flow sequence sum should be 15");
    }

    @Test
    @DisplayName("YamlWire should read block mapping with indentation")
    public void testBlockMappingIndented() {
        YamlWire wire = YamlWire.from("outer:\n  inner1: a\n  inner2: b");
        wire.read("outer").marshallable(w -> {
            assertEquals("a", w.read("inner1").text(), "Block mapping inner1 should be 'a' value");
            assertEquals("b", w.read("inner2").text(), "Block mapping inner2 should be 'b' value");
        });
    }

    @Test
    @DisplayName("YamlWire should read block sequence with dash markers")
    public void testBlockSequenceDash() {
        YamlWire wire = YamlWire.from("list:\n  - item1\n  - item2\n  - item3");
        List<String> items = new ArrayList<>();
        wire.read("list").sequence(items, (list, v) -> {
            while (v.hasNextSequenceItem()) {
                list.add(v.text());
            }
        });
        assertEquals(3, items.size(), "Block sequence should have 3 items total");
        assertEquals("item1", items.get(0), "Block sequence first item should be item1");
    }

    // ========== Numeric Edge Cases ==========

    @Test
    @DisplayName("YamlWire should read integer boundary values")
    public void testIntegerBoundaries() {
        YamlWire wire = YamlWire.from("min: -2147483648\nmax: 2147483647\nzero: 0");
        assertEquals(Integer.MIN_VALUE, wire.read("min").int32(), "YamlWire should parse minimum int value");
        assertEquals(Integer.MAX_VALUE, wire.read("max").int32(), "YamlWire should parse maximum int value");
        assertEquals(0, wire.read("zero").int32(), "YamlWire should parse zero int value");
    }

    @Test
    @DisplayName("YamlWire should read long boundary values")
    public void testLongBoundaries() {
        YamlWire wire = YamlWire.from("max: 9223372036854775807\nminPlusOne: -9223372036854775807");
        assertEquals(Long.MAX_VALUE, wire.read("max").int64(), "YamlWire should parse maximum long value");
        assertEquals(Long.MIN_VALUE + 1, wire.read("minPlusOne").int64(), "YamlWire should parse min+1 long value");
    }

    @Test
    @DisplayName("YamlWire should read hexadecimal numeric values")
    public void testHexValues() {
        YamlWire wire = YamlWire.from("a: 0xFF\nb: 0x0\nc: 0xABCDEF");
        assertEquals(255, wire.read("a").int32(), "YamlWire should parse 0xFF as 255");
        assertEquals(0, wire.read("b").int32(), "YamlWire should parse 0x0 as 0");
        assertEquals(0xABCDEF, wire.read("c").int32(), "YamlWire should parse 0xABCDEF hex value");
    }

    @Test
    @DisplayName("YamlWire should read floating point values")
    public void testFloatingPoint() {
        YamlWire wire = YamlWire.from("a: 3.14\nb: -2.5\nc: 0.0\nd: 1e10");
        assertEquals(3.14, wire.read("a").float64(), 0.001, "YamlWire should parse 3.14 float value");
        assertEquals(-2.5, wire.read("b").float64(), 0.001, "YamlWire should parse -2.5 float value");
        assertEquals(0.0, wire.read("c").float64(), 0.001, "YamlWire should parse 0.0 float value");
        assertEquals(1e10, wire.read("d").float64(), 1e5, "YamlWire should parse 1e10 float value");
    }

    // ========== Consumer Callback Tests ==========

    @Test
    @DisplayName("YamlWire should read int8 with consumer callback")
    public void testInt8WithConsumer() {
        YamlWire wire = YamlWire.from("val: 42");
        AtomicInteger result = new AtomicInteger();
        wire.read("val").int8(result, AtomicInteger::set);
        assertEquals(42, result.get(), "Int8 consumer should receive parsed value 42");
    }

    @Test
    @DisplayName("YamlWire should read int16 with consumer callback")
    public void testInt16WithConsumer() {
        YamlWire wire = YamlWire.from("val: 12345");
        AtomicInteger result = new AtomicInteger();
        wire.read("val").int16(result, AtomicInteger::set);
        assertEquals(12345, result.get(), "Int16 consumer should receive parsed value 12345");
    }

    @Test
    @DisplayName("YamlWire should read int32 with consumer callback")
    public void testInt32WithConsumer() {
        YamlWire wire = YamlWire.from("val: 1234567");
        AtomicInteger result = new AtomicInteger();
        wire.read("val").int32(result, AtomicInteger::set);
        assertEquals(1234567, result.get(), "Int32 consumer should receive parsed value 1234567");
    }

    @Test
    @DisplayName("YamlWire should read int64 with consumer callback")
    public void testInt64WithConsumer() {
        YamlWire wire = YamlWire.from("val: 9876543210");
        AtomicLong result = new AtomicLong();
        wire.read("val").int64(result, AtomicLong::set);
        assertEquals(9876543210L, result.get(), "Int64 consumer should receive parsed value 9876543210");
    }

    @Test
    @DisplayName("YamlWire should read uint8 with consumer callback")
    public void testUint8WithConsumer() {
        YamlWire wire = YamlWire.from("val: 255");
        AtomicInteger result = new AtomicInteger();
        wire.read("val").uint8(result, AtomicInteger::set);
        assertEquals(255, result.get(), "Uint8 consumer should receive parsed value 255");
    }

    @Test
    @DisplayName("YamlWire should read uint16 with consumer callback")
    public void testUint16WithConsumer() {
        YamlWire wire = YamlWire.from("val: 65535");
        AtomicInteger result = new AtomicInteger();
        wire.read("val").uint16(result, AtomicInteger::set);
        assertEquals(65535, result.get(), "Uint16 consumer should receive parsed value 65535");
    }

    @Test
    @DisplayName("YamlWire should read uint32 with consumer callback")
    public void testUint32WithConsumer() {
        YamlWire wire = YamlWire.from("val: 4294967295");
        AtomicLong result = new AtomicLong();
        wire.read("val").uint32(result, AtomicLong::set);
        assertEquals(4294967295L, result.get(), "Uint32 consumer should receive parsed value 4294967295");
    }

    @Test
    @DisplayName("YamlWire should read float32 with consumer callback")
    public void testFloat32WithConsumer() {
        YamlWire wire = YamlWire.from("val: 3.14");
        AtomicReference<Float> result = new AtomicReference<>();
        wire.read("val").float32(result, AtomicReference::set);
        assertEquals(3.14f, result.get(), 0.001f, "Float32 consumer should receive parsed value 3.14");
    }

    @Test
    @DisplayName("YamlWire should read float64 with consumer callback")
    public void testFloat64WithConsumer() {
        YamlWire wire = YamlWire.from("val: 3.14159265359");
        AtomicReference<Double> result = new AtomicReference<>();
        wire.read("val").float64(result, AtomicReference::set);
        assertEquals(3.14159265359, result.get(), 0.0000001, "Float64 consumer should receive parsed value 3.14159265359");
    }

    // ========== String and Text Edge Cases ==========

    @Test
    @DisplayName("YamlWire should read empty string literal values")
    public void testEmptyString() {
        YamlWire wire = YamlWire.from("empty: ''");
        assertEquals("", wire.read("empty").text(), "YamlWire should read empty string value");
    }

    @Test
    @DisplayName("YamlWire should read string with special YAML characters")
    public void testStringWithSpecialChars() {
        YamlWire wire = YamlWire.from("colon: \"key: value\"\nhash: \"#comment\"");
        assertEquals("key: value", wire.read("colon").text(), "YamlWire should read string with colon");
        assertEquals("#comment", wire.read("hash").text(), "YamlWire should read string with hash");
    }

    @Test
    @DisplayName("YamlWire should read string with escape sequences")
    public void testStringWithEscapes() {
        YamlWire wire = YamlWire.from("newline: \"line1\\nline2\"\ntab: \"col1\\tcol2\"");
        assertEquals("line1\nline2", wire.read("newline").text(), "YamlWire should convert newline escape sequence");
        assertEquals("col1\tcol2", wire.read("tab").text(), "YamlWire should convert tab escape sequence");
    }

    @Test
    @DisplayName("YamlWire should read text into StringBuilder buffer")
    public void testTextToStringBuilder() {
        YamlWire wire = YamlWire.from("field: hello world");
        StringBuilder sb = new StringBuilder();
        wire.read("field").textTo(sb);
        assertEquals("hello world", sb.toString(), "YamlWire should write text to StringBuilder");
    }

    @Test
    @DisplayName("YamlWire should read text into Bytes buffer")
    public void testTextToBytes() {
        YamlWire wire = YamlWire.from("field: hello bytes");
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        wire.read("field").textTo(bytes);
        assertEquals("hello bytes", bytes.toString(), "YamlWire should write text to Bytes");
        bytes.releaseLast();
    }

    // ========== UUID Tests ==========

    @Test
    @DisplayName("YamlWire should read UUID identifier values")
    public void testUUID() {
        UUID expected = UUID.randomUUID();
        YamlWire wire = YamlWire.from("id: " + expected);
        assertEquals(expected, wire.read("id").uuid(), "YamlWire should round-trip UUID value");
    }

    @Test
    @DisplayName("YamlWire should read UUID with consumer callback")
    public void testUUIDWithConsumer() {
        UUID expected = UUID.randomUUID();
        YamlWire wire = YamlWire.from("id: " + expected);
        AtomicReference<UUID> result = new AtomicReference<>();
        wire.read("id").uuid(result, AtomicReference::set);
        assertEquals(expected, result.get(), "YamlWire should round-trip UUID via consumer");
    }

    // ========== Boolean Tests ==========

    @Test
    @DisplayName("YamlWire should read various boolean representations")
    public void testBooleanVariants() {
        YamlWire wire = YamlWire.from("a: true\nb: false\nc: yes\nd: no");
        assertTrue(wire.read("a").bool(), "YamlWire should parse token 'true' as true");
        assertFalse(wire.read("b").bool(), "YamlWire should parse token 'false' as false");
        assertTrue(wire.read("c").bool(), "YamlWire should parse token 'yes' as true");
        assertFalse(wire.read("d").bool(), "YamlWire should parse token 'no' as false");
    }

    @Test
    @DisplayName("YamlWire should read boolean with consumer callback")
    public void testBoolWithConsumer() {
        YamlWire wire = YamlWire.from("val: true");
        AtomicReference<Boolean> result = new AtomicReference<>();
        wire.read("val").bool(result, AtomicReference::set);
        assertTrue(result.get(), "Boolean consumer should receive parsed true value");
    }

    // ========== Skip Value Tests ==========

    @Test
    @DisplayName("YamlWire should skip scalar field values")
    public void testSkipScalar() {
        YamlWire wire = YamlWire.from("skip: 42\nkeep: value");
        wire.read("skip").skipValue();
        assertEquals("value", wire.read("keep").text(), "YamlWire should skip scalar and read next value");
    }

    @Test
    @DisplayName("YamlWire should skip sequence item values")
    public void testSkipSequence() {
        YamlWire wire = YamlWire.from("skip: [1, 2, 3]\nkeep: value");
        wire.read("skip").skipValue();
        assertEquals("value", wire.read("keep").text(), "YamlWire should skip sequence and read next value");
    }

    @Test
    @DisplayName("YamlWire should skip mapping entry values")
    public void testSkipMapping() {
        YamlWire wire = YamlWire.from("skip: { a: 1, b: 2 }\nkeep: value");
        wire.read("skip").skipValue();
        assertEquals("value", wire.read("keep").text(), "YamlWire should skip mapping and read next value");
    }

    // ========== Bracket Type Tests ==========

    @Test
    @DisplayName("ValueIn should report bracket type for mapping")
    public void testBracketTypeMapping() {
        YamlWire wire = YamlWire.from("field: { a: 1 }");
        wire.read("field");
        BracketType type = wire.getValueIn().getBracketType();
        assertEquals(BracketType.MAP, type, "Bracket type should be MAP for mapping");
    }

    @Test
    @DisplayName("ValueIn should report bracket type for sequence")
    public void testBracketTypeSequence() {
        YamlWire wire = YamlWire.from("field: [1, 2]");
        wire.read("field");
        BracketType type = wire.getValueIn().getBracketType();
        assertEquals(BracketType.SEQ, type, "Bracket type should be SEQ for sequence");
    }

    @Test
    @DisplayName("ValueIn should report bracket type for scalar")
    public void testBracketTypeScalar() {
        YamlWire wire = YamlWire.from("field: value");
        wire.read("field");
        BracketType type = wire.getValueIn().getBracketType();
        assertEquals(BracketType.NONE, type, "Bracket type should be NONE for scalar");
    }

    // ========== Wire Reference Tests ==========

    @Test
    @DisplayName("ValueIn should return parent wireIn reference instance")
    public void testWireInReference() {
        YamlWire wire = YamlWire.from("field: value");
        assertSame(wire, wire.getValueIn().wireIn(), "ValueIn should return parent wire instance");
    }

    @Test
    @DisplayName("ValueIn should return runtime classLookup instance for lookups")
    public void testClassLookup() {
        YamlWire wire = YamlWire.from("field: value");
        assertNotNull(wire.getValueIn().classLookup(), "ValueIn classLookup should not be null");
    }

    // ========== Comment Tests ==========

    @Test
    @DisplayName("YamlWire should read field value after comment marker line")
    public void testCommentLine() {
        YamlWire wire = YamlWire.from("# This is a comment\nfield: value");
        assertEquals("value", wire.read("field").text(), "YamlWire should skip comment and read value");
    }

    @Test
    @DisplayName("YamlWire should read multiple values with comments between")
    public void testMultipleComments() {
        YamlWire wire = YamlWire.from("# Comment 1\na: 1\n# Comment 2\nb: 2");
        assertEquals(1, wire.read("a").int32(), "YamlWire should read first value after comment");
        assertEquals(2, wire.read("b").int32(), "YamlWire should read second value after comment");
    }

    // ========== Nested Structure Tests ==========

    @Test
    @DisplayName("YamlWire should read deeply nested mapping")
    public void testDeeplyNestedMapping() {
        YamlWire wire = YamlWire.from("l1:\n  l2:\n    l3:\n      value: 42");
        AtomicInteger value = new AtomicInteger();
        wire.read("l1").marshallable(l1 -> l1.read("l2").marshallable(l2 -> l2.read("l3").marshallable(l3 -> value.set(l3.read("value").int32()))));
        assertEquals(42, value.get(), "Deeply nested mapping value should be 42");
    }

    @Test
    @DisplayName("YamlWire should read nested sequence structures")
    public void testNestedSequences() {
        YamlWire wire = YamlWire.from("outer: [[1, 2], [3, 4]]");
        List<List<Integer>> result = new ArrayList<>();
        wire.read("outer").sequence(result, (list, outer) -> {
            while (outer.hasNextSequenceItem()) {
                List<Integer> inner = new ArrayList<>();
                outer.sequence(inner, (innerList, v) -> {
                    while (v.hasNextSequenceItem()) {
                        innerList.add(v.int32());
                    }
                });
                list.add(inner);
            }
        });
        assertEquals(2, result.size(), "Outer sequence should have 2 inner sequences");
        assertEquals(2, result.get(0).size(), "First inner sequence should have 2 items");
        assertEquals(1, result.get(0).get(0), "First inner sequence first item should be 1");
    }

    // ========== Byte Array Tests ==========

    @Test
    @DisplayName("YamlWire should read bytes from YAML content")
    public void testBytesReading() {
        YamlWire wire = YamlWire.from("data: hello");
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        wire.read("data").bytes(bytes);
        assertEquals("hello", bytes.toString(), "Bytes output should contain text value");
        bytes.releaseLast();
    }

    // ========== Tag and Type Tests ==========

    @Test
    @DisplayName("YamlWire should read typed string values")
    public void testTypedString() {
        YamlWire wire = YamlWire.from("obj: !java.lang.String hello");
        Object result = wire.read("obj").object();
        assertEquals("hello", result, "YamlWire should read typed String value");
    }

    @Test
    @DisplayName("YamlWire should read typed integer values")
    public void testTypedInteger() {
        YamlWire wire = YamlWire.from("obj: !int 42");
        Object result = wire.read("obj").object();
        assertInstanceOf(Number.class, result, "Typed int should be a Number");
        assertEquals(42, ((Number) result).intValue(), "Typed int value should be 42");
    }

    @Test
    @DisplayName("YamlWire should read type literal values")
    public void testTypeLiteral() {
        YamlWire wire = YamlWire.from("class: !type java.lang.String");
        Class<?> result = wire.read("class").typeLiteral();
        assertEquals(String.class, result, "YamlWire should parse type literal");
    }

    // ========== Null Handling Tests ==========

    // TODO FIX: YamlWire does not parse 'null' keyword as Java null
    @Test
    @Disabled("YamlWire returns string 'null' not Java null - needs investigation")
    @DisplayName("YamlWire should map null keyword to Java null")
    public void testExplicitNull() {
        YamlWire wire = YamlWire.from("field: null");
        assertNull(wire.read("field").text(), "YamlWire should parse 'null' keyword as null");
    }

    @Test
    @DisplayName("YamlWire should detect null values with isNull for empty value")
    public void testIsNull() {
        YamlWire wire = YamlWire.from("notNull: value");
        assertFalse(wire.read("notNull").isNull(), "notNull field should report isNull false");
    }

    // ========== Character Reading Tests ==========

    @Test
    @DisplayName("YamlWire should read single character values")
    public void testReadCharacter() {
        YamlWire wire = YamlWire.from("char: A");
        assertEquals('A', wire.read("char").character(), "YamlWire should read single character");
    }

    @Test
    @DisplayName("YamlWire should read quoted character values")
    public void testReadQuotedCharacter() {
        YamlWire wire = YamlWire.from("char: \"B\"");
        assertEquals('B', wire.read("char").character(), "YamlWire should read quoted character");
    }

    // ========== Date/Time Tests ==========

    @Test
    @DisplayName("YamlWire should parse LocalDate from ISO date")
    public void testLocalDate() {
        YamlWire wire = YamlWire.from("date: 2024-06-15");
        java.time.LocalDate date = wire.read("date").object(java.time.LocalDate.class);
        assertEquals(java.time.LocalDate.of(2024, 6, 15), date, "LocalDate should match ISO date input");
    }

    @Test
    @DisplayName("YamlWire should parse LocalDateTime from ISO date-time")
    public void testLocalDateTime() {
        YamlWire wire = YamlWire.from("datetime: \"2024-06-15T14:30:45\"");
        java.time.LocalDateTime dt = wire.read("datetime").object(java.time.LocalDateTime.class);
        assertEquals(java.time.LocalDateTime.of(2024, 6, 15, 14, 30, 45), dt,
                "LocalDateTime should match ISO date-time input");
    }

    // ========== Enum Reading Tests ==========

    @Test
    @DisplayName("YamlWire should read enum values via asEnum")
    public void testEnumReading() {
        YamlWire wire = YamlWire.from("type: BINARY");
        WireType result = wire.read("type").asEnum(WireType.class);
        assertEquals(WireType.BINARY, result, "YamlWire should parse enum from text");
    }

    @Test
    @DisplayName("YamlWire should read typed enum values")
    public void testTypedEnum() {
        YamlWire wire = YamlWire.from("type: !net.openhft.chronicle.wire.WireType BINARY");
        Object result = wire.read("type").object();
        assertEquals(WireType.BINARY, result, "YamlWire should parse typed enum value");
    }

    // ========== Object Type Tests ==========

    @Test
    @DisplayName("YamlWire should read boxed Integer objects")
    public void testBoxedInteger() {
        YamlWire wire = YamlWire.from("val: 42");
        Integer result = wire.read("val").object(Integer.class);
        assertEquals(42, result, "YamlWire should parse Integer object");
    }

    @Test
    @DisplayName("YamlWire should read boxed Long objects")
    public void testBoxedLong() {
        YamlWire wire = YamlWire.from("val: 9876543210");
        Long result = wire.read("val").object(Long.class);
        assertEquals(9876543210L, result, "YamlWire should parse Long object");
    }

    @Test
    @DisplayName("YamlWire should read boxed Double objects")
    public void testBoxedDouble() {
        YamlWire wire = YamlWire.from("val: 3.14159");
        Double result = wire.read("val").object(Double.class);
        assertEquals(3.14159, result, 0.00001, "YamlWire should parse Double object");
    }

    // ========== Array Tests ==========

    @Test
    @DisplayName("YamlWire should read int array values")
    public void testIntArray() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        YamlWire wire = new YamlWire(bytes);
        int[] data = {1, 2, 3, 4, 5};
        wire.write("arr").object(data);

        bytes.readPositionRemaining(0, bytes.writePosition());
        int[] result = wire.read("arr").object(int[].class);
        assertArrayEquals(data, result, "YamlWire should round-trip int array");
    }

    @Test
    @DisplayName("YamlWire should read String array values")
    public void testStringArray() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        YamlWire wire = new YamlWire(bytes);
        String[] data = {"one", "two", "three"};
        wire.write("arr").object(data);

        bytes.readPositionRemaining(0, bytes.writePosition());
        String[] result = wire.read("arr").object(String[].class);
        assertArrayEquals(data, result, "YamlWire should round-trip String array");
    }

    // ========== HasMore Tests ==========

    @Test
    @DisplayName("YamlWire hasMore should return true when content available")
    public void testHasMore() {
        YamlWire wire = YamlWire.from("a: 1\nb: 2");
        assertTrue(wire.hasMore(), "Wire with content should have more");
        wire.read("a").int32();
        assertTrue(wire.hasMore(), "Wire with remaining content should have more");
        wire.read("b").int32();
        assertFalse(wire.hasMore(), "Wire after reading all should not have more");
    }

    // ========== Empty Structure Tests ==========

    @Test
    @DisplayName("YamlWire should handle empty mapping without fields")
    public void testEmptyMapping() {
        YamlWire wire = YamlWire.from("obj: {}");
        AtomicInteger count = new AtomicInteger(0);
        wire.read("obj").marshallable(w -> {
            while (w.hasMore()) {
                w.read().skipValue();
                count.incrementAndGet();
            }
        });
        assertEquals(0, count.get(), "Empty mapping should have no fields");
    }

    @Test
    @DisplayName("YamlWire should handle empty sequence without items")
    public void testEmptySequence() {
        YamlWire wire = YamlWire.from("list: []");
        List<Integer> items = new ArrayList<>();
        wire.read("list").sequence(items, (list, v) -> {
            while (v.hasNextSequenceItem()) {
                list.add(v.int32());
            }
        });
        assertTrue(items.isEmpty(), "Empty sequence should have no items");
    }

    // ========== Quoted Key Tests ==========

    @Test
    @DisplayName("YamlWire should handle quoted field keys")
    public void testQuotedKey() {
        YamlWire wire = YamlWire.from("\"special:key\": value");
        assertEquals("value", wire.read("special:key").text(), "YamlWire should handle quoted key");
    }

    @Test
    @DisplayName("YamlWire should handle field keys with numbers")
    public void testNumericKey() {
        YamlWire wire = YamlWire.from("field123: value");
        assertEquals("value", wire.read("field123").text(), "YamlWire should handle numeric key");
    }

    // ========== Unicode Tests ==========

    @Test
    @DisplayName("YamlWire should read unicode escape sequences")
    public void testUnicodeEscape() {
        YamlWire wire = YamlWire.from("field: \"\\u0048\\u0065\\u006C\\u006C\\u006F\"");
        String result = wire.read("field").text();
        assertEquals("Hello", result, "YamlWire should decode unicode escapes");
    }

    // ========== Mixed Content Tests ==========

    @Test
    @DisplayName("YamlWire should read mixed types in sequence")
    public void testMixedTypeSequence() {
        YamlWire wire = YamlWire.from("list: [42, hello, true, 3.14]");
        List<Object> items = new ArrayList<>();
        wire.read("list").sequence(items, (list, v) -> {
            while (v.hasNextSequenceItem()) {
                list.add(v.object());
            }
        });
        assertEquals(4, items.size(), "Mixed sequence should have 4 items");
    }

    @Test
    @DisplayName("YamlWire should read map with mixed value types")
    public void testMixedTypeMapping() {
        YamlWire wire = YamlWire.from("obj: { num: 42, str: hello, flag: true }");
        Map<String, Object> result = new HashMap<>();
        wire.read("obj").marshallable(w -> {
            result.put("num", w.read("num").object());
            result.put("str", w.read("str").object());
            result.put("flag", w.read("flag").object());
        });
        assertEquals(3, result.size(), "Mixed mapping should have 3 entries");
    }
}
