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
    @Test(expected = IOException.class)
    public void fromFile() throws IOException {
        fail("Got " + Marshallable.fromFile("empty-file.yaml"));
    }

    @Test
    public void testEmptyFromString() {
        assertEquals("", Marshallable.fromString(""));
    }

    @Ignore("Undefined behaviour")
    @Test(expected = IllegalArgumentException.class)
    public void testFromString2() {
        Object o = Marshallable.fromString("\"");
        assertNotNull(o);
    }

    @Ignore("Undefined behaviour")
    @Test(expected = IllegalArgumentException.class)
    public void testFromString3() {
        Object o = Marshallable.fromString("'");
        assertNotNull(o);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testBytesMarshallable() {
        @NotNull Marshallable m = new MyTypes();

        @NotNull Bytes bytes = allocateElasticOnHeap();
        assertTrue(bytes.isElastic());
        @NotNull TextWire wire = new TextWire(bytes);
        m.writeMarshallable(wire);

        m.readMarshallable(wire);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testEquals() {
        @NotNull final Bytes bytes = allocateElasticOnHeap();
        assertTrue(bytes.isElastic());
        @NotNull final MyTypes source = new MyTypes();
        //change default value fields in order to let destination to be changed from its default values too
        source.flag(true);
        source.s((short) 1);
        source.d(1.0);
        source.l(1L);
        source.i(1);
        source.text("a");
        @NotNull final Marshallable destination = new MyTypes();
        assertNotEquals(source, destination);
        @NotNull final TextWire wire = new TextWire(bytes);
        source.writeMarshallable(wire);
        destination.readMarshallable(wire);
        assertEquals(source, destination);
    }

    @Test
    public void testCopy() {
        DTO2 dto2 = new DTO2();
        dto2.one = RetentionPolicy.CLASS;
        dto2.two = Arrays.asList(1L, 22L);
        dto2.three = "2018-11-02";

        String s = WireType.TEXT.asString(dto2);
        System.out.println(s);
        DTO1 dto1 = WireType.TEXT.fromString(DTO1.class, s);
        assertEquals("!net.openhft.chronicle.wire.MarshallableTest$DTO1 {\n" +
                "  one: CLASS,\n" +
                "  two: [\n" +
                "    1,\n" +
                "    22\n" +
                "  ],\n" +
                "  three: 2018-11-02\n" +
                "}\n", WireType.TEXT.asString(dto1));
    }

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

    @Test
    public void test() {
        StaticData staticData0 = Marshallable.fromString(StaticData.class, "{ }");
        assertNotNull(staticData0);
        assertEquals(100, staticData0.anInt);
        assertNotNull(staticData0.aList);   // <== OK, EXPECTED

        StaticData staticData = Marshallable.fromString(StaticData.class, "anInt: 42");
        assertNotNull(staticData);
        assertEquals(42, staticData.anInt);
        assertNotNull(staticData.aList);   // <== OK, EXPECTED

        NonStaticData nonStaticData = Marshallable.fromString(NonStaticData.class, "{ }");
        assertNotNull(nonStaticData);
        assertEquals(0, nonStaticData.anInt);
        assertNull(nonStaticData.aList);   // <== UNEXPECTED
    }

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

    @Test
    public void getField() throws NoSuchFieldException {
        DTO1 dto1 = new DTO1();
        LocalDate three = dto1.getField("three", LocalDate.class);
        assertNull(three);
        LocalDate date = LocalDate.of(2020, 11, 20);
        dto1.setField("three", date);
        LocalDate three2 = dto1.getField("three", LocalDate.class);
        assertEquals(date, three2);
    }

    @Test
    public void getLongField() throws NoSuchFieldException {
        StaticData sd = new StaticData();
        long anInt = sd.getLongField("anInt");
        assertEquals(100, anInt);
        sd.setLongField("anInt", Integer.MAX_VALUE);
        long anInt2 = sd.getLongField("anInt");
        assertEquals(Integer.MAX_VALUE, anInt2);

        sd.setLongField("anInt", Long.MIN_VALUE);
        long anInt3 = sd.getLongField("anInt");
        assertEquals((int) Long.MIN_VALUE, anInt3);

        long aLong = sd.getLongField("aLong");
        assertEquals(~100L, aLong);
        sd.setLongField("aLong", Integer.MAX_VALUE);
        long aLong2 = sd.getLongField("aLong");
        assertEquals(Integer.MAX_VALUE, aLong2);

        sd.setLongField("aLong", Long.MIN_VALUE);
        long aLong3 = sd.getLongField("aLong");
        assertEquals(Long.MIN_VALUE, aLong3);


    }

    static class WithArray extends SelfDescribingMarshallable {
        DTO1[] dto1s;
    }

    static class DTO1 extends SelfDescribingMarshallable {
        String one;
        List<Integer> two;
        LocalDate three;
    }

    static class DTO2 extends SelfDescribingMarshallable {
        RetentionPolicy one;
        List<Long> two;
        String three;
    }

    static class StaticData extends AbstractMarshallableCfg {
        int anInt = 100;
        long aLong = ~100L;
        List<String> aList = new ArrayList<>();
    }

    class NonStaticData extends AbstractMarshallableCfg {
        int anInt;
        List<String> aList = new ArrayList<>();
    }
}
