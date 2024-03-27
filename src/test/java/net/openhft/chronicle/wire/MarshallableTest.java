/*
 * Copyright 2016-2020 chronicle.software
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.lang.annotation.RetentionPolicy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.openhft.chronicle.bytes.Bytes.allocateElasticOnHeap;
import static org.junit.Assert.*;

public class MarshallableTest extends WireTestCommon {

    // Test to check if the fromFile() method of Marshallable throws an IOException for an empty file.
    @Test(expected = IOException.class)
    public void fromFile() throws IOException {
        fail("Got " + Marshallable.fromFile("empty-file.yaml"));
    }

    // Test to check if Marshallable.fromString() method returns an empty string when an empty string is provided.
    @Test
    public void testEmptyFromString() {
        assertEquals("", Marshallable.fromString(""));
    }

    // Test for undefined behavior when a string with a single double-quote is passed.
    @Ignore("Undefined behaviour")
    @Test(expected = IllegalArgumentException.class)
    public void testFromString2() {
        Object o = Marshallable.fromString("\"");
        assertNotNull(o);
    }

    // Test for undefined behavior when a string with a single single-quote is passed.
    @Ignore("Undefined behaviour")
    @Test(expected = IllegalArgumentException.class)
    public void testFromString3() {
        Object o = Marshallable.fromString("'");
        assertNotNull(o);
    }

    // Test for verifying the marshallable operation on bytes.
    @SuppressWarnings("rawtypes")
    @Test
    public void testBytesMarshallable() {
        @NotNull Marshallable m = new MyTypes();

        @NotNull Bytes<?> bytes = allocateElasticOnHeap();
        assertTrue(bytes.isElastic());
        @NotNull Wire wire = WireType.TEXT.apply(bytes);
        m.writeMarshallable(wire);

        m.readMarshallable(wire);
    }

    // Test for verifying the equals operation on marshalled objects.
    @SuppressWarnings("rawtypes")
    @Test
    public void testEquals() {
        @NotNull final Bytes<?> bytes = allocateElasticOnHeap();
        assertTrue(bytes.isElastic());
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
        assertNotEquals(source, destination);
        @NotNull final Wire wire = WireType.TEXT.apply(bytes);
        source.writeMarshallable(wire);
        destination.readMarshallable(wire);
        assertEquals(source, destination);
    }

    // Helper method to test the copy operation across different data transfer objects using the specified wire type.
    private static void doTestCopy(WireType wireType) {
        DTO2 dto2 = new DTO2();
        dto2.one = RetentionPolicy.CLASS;
        dto2.two = Arrays.asList(1L, 22L);
        dto2.three = "2018-11-02";

        String s = wireType.asString(dto2);
        // System.out.println(s);
        DTO1 dto1 = wireType.fromString(DTO1.class, s);
        assertEquals("!net.openhft.chronicle.wire.MarshallableTest$DTO1 {\n" +
                "  one: CLASS,\n" +
                "  two: [\n" +
                "    1,\n" +
                "    22\n" +
                "  ],\n" +
                "  three: 2018-11-02\n" +
                "}\n", wireType.asString(dto1));
    }

    // Test the copying process using WireType.TEXT
    @Test
    public void testCopy() {
        doTestCopy(WireType.TEXT);
    }

    // TODO: This test is currently ignored. The copy process using WireType.YAML_ONLY needs to be fixed.
    @Ignore(/* TODO FIX */)
    @Test
    public void testCopyYaml() {
        doTestCopy(WireType.YAML_ONLY);
    }

    // Test equality of two objects containing arrays
    @Test
    public void equalsWithArray() {
        WithArray a = new WithArray();
        WithArray b = new WithArray();
        assertEquals(a, b);

        a.dto1s = new DTO1[1];
        a.dto1s[0] = new DTO1();
        b.dto1s = new DTO1[1];
        b.dto1s[0] = new DTO1();
        if (!a.equals(b))
            assertEquals(a, b);
    }

    // Test to confirm certain expected exceptions and object behaviors during marshalling
    @Test
    public void test() {
        expectException("Found this$0, in class net.openhft.chronicle.wire.MarshallableTest$NonStaticData which will be ignored!");

        StaticData staticData0 = Marshallable.fromString(StaticData.class, "{ }");
        assertNotNull(staticData0);
        assertEquals(100, staticData0.anInt);
        assertNotNull(staticData0.aList);   // This is the expected behavior

        StaticData staticData = Marshallable.fromString(StaticData.class, "anInt: 42");
        assertNotNull(staticData);
        assertEquals(42, staticData.anInt);
        assertNotNull(staticData.aList);   // This is the expected behavior

        NonStaticData nonStaticData = Marshallable.fromString(NonStaticData.class, "{ }");
        assertNotNull(nonStaticData);
        assertEquals(0, nonStaticData.anInt);
        assertNull(nonStaticData.aList);   // This is unexpected
    }

    // Test the reset functionality of the MyTypes object. This should reset all its fields to default values.
    @Test
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
                "}\n", mt.toString());
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
                "}\n", mt.toString());
    }

    // Test to get and set the field "three" in DTO1 using getField and setField methods
    @Test
    public void getField() throws NoSuchFieldException {
        DTO1 dto1 = new DTO1();
        LocalDate three = dto1.getField("three", LocalDate.class);
        assertNull(three);  // Initially, the field should be null
        LocalDate date = LocalDate.of(2020, 11, 20);
        dto1.setField("three", date);
        LocalDate three2 = dto1.getField("three", LocalDate.class);
        assertEquals(date, three2);  // After setting, the field should match the set value
    }

    // Test the getLongField and setLongField methods with different edge cases on the StaticData class
    @Test
    public void getLongField() throws NoSuchFieldException {
        StaticData sd = new StaticData();
        long anInt = sd.getLongField("anInt");
        assertEquals(100, anInt);  // Default value is 100

        sd.setLongField("anInt", Integer.MAX_VALUE);
        long anInt2 = sd.getLongField("anInt");
        assertEquals(Integer.MAX_VALUE, anInt2);  // Changed to MAX_VALUE of Integer

        sd.setLongField("anInt", Long.MIN_VALUE);
        long anInt3 = sd.getLongField("anInt");
        assertEquals((int) Long.MIN_VALUE, anInt3);  // Set to MIN_VALUE of Long, but casted to int

        long aLong = sd.getLongField("aLong");
        assertEquals(~100L, aLong);  // Default value is ~100L

        sd.setLongField("aLong", Integer.MAX_VALUE);
        long aLong2 = sd.getLongField("aLong");
        assertEquals(Integer.MAX_VALUE, aLong2);  // Changed to MAX_VALUE of Integer

        sd.setLongField("aLong", Long.MIN_VALUE);
        long aLong3 = sd.getLongField("aLong");
        assertEquals(Long.MIN_VALUE, aLong3);  // Set to MIN_VALUE of Long
    }

    // DTO containing an array of DTO1 objects
    static class WithArray extends SelfDescribingMarshallable {
        DTO1[] dto1s;
    }

    // Sample DTO with fields of different types
    static class DTO1 extends SelfDescribingMarshallable {
        String one;
        List<Integer> two;
        LocalDate three;
    }

    // Another sample DTO, similar to DTO1 but with some differences
    static class DTO2 extends SelfDescribingMarshallable {
        RetentionPolicy one;
        List<Long> two;
        String three;
    }

    // A data class with static properties and default values
    static class StaticData extends AbstractMarshallableCfg {
        int anInt = 100;
        long aLong = ~100L;
        List<String> aList = new ArrayList<>();
    }

    // A data class similar to StaticData, but non-static and without default values for some fields
    class NonStaticData extends AbstractMarshallableCfg {
        int anInt;
        List<String> aList = new ArrayList<>();
    }
}
