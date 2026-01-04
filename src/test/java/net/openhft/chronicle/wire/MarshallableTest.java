/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.IOException;
import java.lang.annotation.RetentionPolicy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.openhft.chronicle.bytes.Bytes.allocateElasticOnHeap;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"deprecation", "removal"})
public class MarshallableTest extends WireTestCommon {
    private static final String EXPECTED_DTO1 = "!net.openhft.chronicle.wire.MarshallableTest$DTO1 {\n" +
            "  one: CLASS,\n" +
            "  two: [\n" +
            "    1,\n" +
            "    22\n" +
            "  ],\n" +
            "  three: 2018-11-02\n" +
            "}\n";

    // Test to check if the fromFile() method of Marshallable throws an IOException for an empty file.
    @Test
    @DisplayName("Rejects empty file when reading marshallable")
    public void fromFile() {
        assertThrows(IOException.class, () ->
                fail("fromFile should throw IOException for empty-file.yaml, but returned "
                        + Marshallable.fromFile("empty-file.yaml")),
                "fromFile should throw IOException when reading empty-file.yaml");
    }

    // Test to check if Marshallable.fromString() method returns an empty string when an empty string is provided.
    @Test
    @DisplayName("Marshallable.fromString should return zero-length output string for empty input text")
    public void testEmptyFromString() {
        assertEquals("", Marshallable.fromString(""),
                "fromString should return a zero-length string for blank input text");
    }

    // Test for undefined behavior when a string with a single double-quote is passed.
    @Test
    @Disabled("Undefined behaviour for single double-quote input")
    @DisplayName("Rejects single double-quote input string content")
    public void testFromString2() {
        assertThrows(IllegalArgumentException.class, () -> {
            Object o = Marshallable.fromString("\"");
            assertNotNull(o, "Parsed object for double-quote input should not be null before exception");
        }, "Single double-quote input should be rejected");
    }

    // Test for undefined behavior when a string with a single single-quote is passed.
    @Test
    @Disabled("Undefined behaviour for single quote input parsing")
    @DisplayName("Rejects single quote input string content")
    public void testFromString3() {
        assertThrows(IllegalArgumentException.class, () -> {
            Object o = Marshallable.fromString("'");
            assertNotNull(o, "Parsed object for single-quote input should not be null before exception");
        }, "Single single-quote input should be rejected");
    }

    // Test for verifying the marshallable operation on bytes.
    @Test
    @SuppressWarnings("rawtypes")
    @DisplayName("Writes and reads marshallable to bytes")
    public void testBytesMarshallable() {
        @NotNull Marshallable m = new MyTypes();

        @NotNull Bytes<?> bytes = allocateElasticOnHeap();
        assertTrue(bytes.isElastic(), "Bytes buffer should be elastic for marshallable test");
        @NotNull Wire wire = WireType.TEXT.apply(bytes);
        m.writeMarshallable(wire);

        m.readMarshallable(wire);
    }

    // Test for verifying the equals operation on marshalled objects.
    @Test
    @SuppressWarnings("rawtypes")
    @DisplayName("Checks equals after round-trip marshalling")
    public void testEquals() {
        @NotNull final Bytes<?> bytes = allocateElasticOnHeap();
        assertTrue(bytes.isElastic(), "Bytes buffer should be elastic for equals marshalling test");
        @NotNull final MyTypes source = new MyTypes();
        //change default value fields in order to let destination to be changed from its default values too
        source.flag(true);
        source.s((short) 1);
        source.d(1.0);
        source.l(1L);
        source.i(1);
        source.ch((char)0xFFFF);
        source.text("a");
        @NotNull final Marshallable destination = new MyTypes();
        assertNotEquals(source, destination, "Distinct objects should differ before marshalling");
        @NotNull final Wire wire = WireType.TEXT.apply(bytes);
        source.writeMarshallable(wire);
        destination.readMarshallable(wire);
        assertEquals(source, destination, "Round-tripped object should match source");
    }

    // Helper method to test the copy operation across different data transfer objects using the specified wire type.
    private static String doTestCopy(WireType wireType) {
        DTO2 dto2 = new DTO2();
        dto2.one = RetentionPolicy.CLASS;
        dto2.two = Arrays.asList(1L, 22L);
        dto2.three = "2018-11-02";

        String s = wireType.asString(dto2);
        DTO1 dto1 = wireType.fromString(DTO1.class, s);
        return wireType.asString(dto1);
    }

    // Test the copying process using WireType.TEXT
    @Test
    @DisplayName("Copies DTO using text wire format")
    public void testCopy() {
        assertEquals(EXPECTED_DTO1, doTestCopy(WireType.TEXT),
                "Copy via TEXT wire should match expected DTO1");
    }

    // TODO: This test is currently ignored. The copy process using WireType.YAML_ONLY needs to be fixed.
    @Test
    @Disabled("YAML_ONLY copy does not match expected DTO1 yet")
    @DisplayName("Copies DTO using YAML_ONLY wire format")
    public void testCopyYaml() {
        assertEquals(EXPECTED_DTO1, doTestCopy(WireType.YAML_ONLY),
                "Copy via YAML_ONLY wire should match expected DTO1");
    }

    // Test equality of two objects containing arrays
    @Test
    @DisplayName("Compares equality for DTO array content")
    public void equalsWithArray() {
        WithArray a = new WithArray();
        WithArray b = new WithArray();
        assertEquals(a, b, "Empty DTO arrays should compare equal");

        a.dto1s = new DTO1[1];
        a.dto1s[0] = new DTO1();
        b.dto1s = new DTO1[1];
        b.dto1s[0] = new DTO1();
        if (!a.equals(b))
            assertEquals(a, b, "Arrays should compare equal after population");
    }

    // Test to confirm certain expected exceptions and object behaviors during marshalling
    @Test
    @DisplayName("Validates defaults for static and non static data")
    public void test() {
        expectException("Found this$0, in class net.openhft.chronicle.wire.MarshallableTest$NonStaticData which will be ignored!");

        StaticData staticData0 = Marshallable.fromString(StaticData.class, "{ }");
        assertNotNull(staticData0, "StaticData instance should be created from empty input document text");
        assertEquals(100, staticData0.anInt, "StaticData default anInt should be 100");
        assertNotNull(staticData0.aList, "StaticData list should default to non-null");

        StaticData staticData = Marshallable.fromString(StaticData.class, "anInt: 42");
        assertNotNull(staticData, "StaticData instance should be created from populated input");
        assertEquals(42, staticData.anInt, "StaticData anInt should reflect configured value");
        assertNotNull(staticData.aList, "StaticData list should remain non-null");

        NonStaticData nonStaticData = Marshallable.fromString(NonStaticData.class, "{ }");
        assertNotNull(nonStaticData, "NonStaticData instance should be created from empty input document text");
        assertEquals(0, nonStaticData.anInt, "NonStaticData default anInt should be 0");
        assertNull(nonStaticData.aList, "NonStaticData list should remain null");
    }

    // Test the reset functionality of the MyTypes object. This should reset all its fields to default values.
    @Test
    @DisplayName("Resets marshallable fields to default values")
    public void testReset() {
        MyTypes mt = new MyTypes()
                .flag(true)
                .b((byte) 1)
                .s((short) 2)
                .ch('3')
                .i(4)
                .f(5)
                .d(6)
                .l(7)
                .text("text");
        assertEquals("!net.openhft.chronicle.wire.MyTypes {\n" +
                "  text: text,\n" +
                "  flag: true,\n" +
                "  b: 1,\n" +
                "  s: 2,\n" +
                "  ch: \"3\",\n" +
                "  i: 4,\n" +
                "  f: 5.0,\n" +
                "  d: 6.0,\n" +
                "  l: 7\n" +
                "}\n", mt.toString(), "Marshallable string should reflect assigned values");
        mt.reset();
        assertEquals("!net.openhft.chronicle.wire.MyTypes {\n" +
                "  text: \"\",\n" +
                "  flag: false,\n" +
                "  b: 0,\n" +
                "  s: 0,\n" +
                "  ch: \"\\0\",\n" +
                "  i: 0,\n" +
                "  f: 0.0,\n" +
                "  d: 0.0,\n" +
                "  l: 0\n" +
                "}\n", mt.toString(), "Marshallable string should reflect reset values");
    }

    // Test to get and set the field "three" in DTO1 using getField and setField methods
    @Test
    @DisplayName("Gets and sets field using reflection helpers")
    public void getField() throws NoSuchFieldException {
        DTO1 dto1 = new DTO1();
        LocalDate three = dto1.getField("three", LocalDate.class);
        assertNull(three, "DTO1.three should default to null");
        LocalDate date = LocalDate.of(2020, 11, 20);
        dto1.setField("three", date);
        LocalDate three2 = dto1.getField("three", LocalDate.class);
        assertEquals(date, three2, "DTO1.three should return assigned value");
    }

    // Test the getLongField and setLongField methods with different edge cases on the StaticData class
    @Test
    @DisplayName("Gets and sets long fields with edge values")
    public void getLongField() throws NoSuchFieldException {
        StaticData sd = new StaticData();
        long anInt = sd.getLongField("anInt");
        assertEquals(100, anInt, "StaticData.anInt default should be 100");

        sd.setLongField("anInt", Integer.MAX_VALUE);
        long anInt2 = sd.getLongField("anInt");
        assertEquals(Integer.MAX_VALUE, anInt2, "StaticData.anInt should reflect Integer.MAX_VALUE");

        sd.setLongField("anInt", Long.MIN_VALUE);
        long anInt3 = sd.getLongField("anInt");
        assertEquals((int) Long.MIN_VALUE, anInt3, "StaticData.anInt should reflect cast Long.MIN_VALUE");

        long aLong = sd.getLongField("aLong");
        assertEquals(~100L, aLong, "StaticData.aLong default should be bitwise complement of 100");

        sd.setLongField("aLong", Integer.MAX_VALUE);
        long aLong2 = sd.getLongField("aLong");
        assertEquals(Integer.MAX_VALUE, aLong2, "StaticData.aLong should reflect Integer.MAX_VALUE");

        sd.setLongField("aLong", Long.MIN_VALUE);
        long aLong3 = sd.getLongField("aLong");
        assertEquals(Long.MIN_VALUE, aLong3, "StaticData.aLong should reflect Long.MIN_VALUE");
    }

    // DTO containing an array of DTO1 objects
    static class WithArray extends SelfDescribingMarshallable {
        DTO1[] dto1s;
    }

    // Sample DTO with fields of different types
    static class DTO1 extends SelfDescribingMarshallable {
        public String one;
        public List<Integer> two;
        public LocalDate three;
    }

    // Another sample DTO, similar to DTO1 but with some differences
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static class DTO2 extends SelfDescribingMarshallable {
        RetentionPolicy one;
        List<Long> two;
        String three;
    }

    // A data class with static properties and default values
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD", "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static class StaticData extends AbstractMarshallableCfg {
        int anInt = 100;
        public long aLong = ~100L;
        List<String> aList = new ArrayList<>();
    }

    // A data class similar to StaticData, but non-static and without default values for some fields
    class NonStaticData extends AbstractMarshallableCfg {
        int anInt;
        List<String> aList = new ArrayList<>();
    }
}
