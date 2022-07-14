/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

@SuppressWarnings("rawtypes")
@RunWith(value = Parameterized.class)
public class WireTests {

    private final WireType wireType;
    private final boolean usePadding;

    @NotNull
    @Rule
    public TestName name = new TestName();

    public WireTests(WireType wireType, boolean usePadding) {
        this.wireType = wireType;
        this.usePadding = usePadding;
    }

    @NotNull
    @Parameterized.Parameters(name = "{index}: {0} padding: {1}")
    public static Collection<Object[]> data() {
        Object[][] list = {
                {WireType.BINARY, true},
                {WireType.BINARY, false},
                {WireType.TEXT, false},
                {WireType.JSON, false}
        };
        return Arrays.asList(list);
    }

    @Test
    public void testHexLongNegativeTest() {
        final Bytes<?> b = Bytes.elasticByteBuffer();
        final long expectedLong1 = -1;
        final long expectedLong2 = Long.MIN_VALUE;
        try {
            final Wire wire = createWire(b);

            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().write("w")
                        .int64_0x(expectedLong1);
                dc.wire().write("x")
                        .int64_0x(expectedLong2);
                dc.wire().write("y").typeLiteral(String.class);
            }

            try (DocumentContext dc = wire.readingDocument()) {
                long w = dc.wire().read("w").int64();
                assertEquals(expectedLong1, w);
                long x = dc.wire().read("x").int64();
                assertEquals(expectedLong2, x);
                Class<Object> y = dc.wire().read("y").typeLiteral();
                assertEquals(String.class, y);
            }
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void testLenientTypeLiteral() {
        final Bytes<?> b = Bytes.elasticByteBuffer();
        try {
            final Wire wire = createWire(b);

            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().write("w").typeLiteral("DoesntExist");
            }

            try (DocumentContext dc = wire.readingDocument()) {
                Type t = dc.wire().read("w").lenientTypeLiteral();
                assertEquals("DoesntExist", t.getTypeName());
            }
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void testDate() {
        final Bytes<?> b = Bytes.elasticByteBuffer();
        final Wire wire = createWire(b);

        wire.getValueOut()
                .object(new Date(1234567890000L));
        assertEquals(new Date(1234567890000L), wire.getValueIn()
                .object(Date.class));

        /* Not sure why this would work
        final Date expectedDate = new Date(1234567890000L);
        String longDateInDefaultLocale = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy").format(expectedDate);
        wire.getValueOut().object(longDateInDefaultLocale);

        assertEquals(expectedDate, wire.getValueIn()
                .object(Date.class));

        wire.getValueOut().object("2009-02-13 23:31:30.000");

        assertEquals(new Date(1234567890000L), wire.getValueIn()
                .object(Date.class));

         */
    }

    @Test
    public void testLocalDateTime() {
        final Bytes<?> b = Bytes.elasticByteBuffer();
        try {
            final Wire wire = createWire(b);
            LocalDateTime expected = LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
            wire.getValueOut().object(expected);
            // is a hint needed?
            Class type = wireType == WireType.JSON ? LocalDateTime.class : Object.class;
            assertEquals(expected, wire.getValueIn().object(type));
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void testZonedDateTime() {
        final Bytes<?> b = Bytes.elasticByteBuffer();
        final Wire wire = createWire(b);
        ZonedDateTime expected = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
        wire.getValueOut().object(expected);
        // is a hint needed?
        Class type = wireType == WireType.JSON ? ZonedDateTime.class : Object.class;
        assertEquals(expected, wire.getValueIn().object(type));

        b.releaseLast();
    }

    @Test
    public void testSkipValueWithNumbersAndStrings() {

        final Bytes<?> b = Bytes.elasticByteBuffer();
        final Wire wire = createWire(b);

        wire.write("value1").text("text");
        wire.write("number").int64(125);

        StringBuilder field;

        field = new StringBuilder();
        wire.read(field).skipValue();
        assertEquals("value1", field.toString());

        field = new StringBuilder();
        wire.read(field).skipValue();
        assertEquals("number", field.toString());

        b.releaseLast();
    }

    @Test
    public void testWriteNull() {
        final Bytes<?> b = Bytes.elasticByteBuffer();
        final Wire wire = createWire(b);
        wire.write().object(null);
        wire.write().object(null);
        wire.write().object(null);
        wire.write().object(null);

        @Nullable Object o = wire.read().object(Object.class);
        Assert.assertNull(o);
        @Nullable String s = wire.read().object(String.class);
        Assert.assertNull(s);
        @Nullable RetentionPolicy rp = wire.read().object(RetentionPolicy.class);
        Assert.assertNull(rp);
        @Nullable Circle c = wire.read().object(Circle.class);  // this fails without the check.
        Assert.assertNull(c);

        b.releaseLast();
    }

    @Test
    public void testClassTypedMarshallableObject() {
        assumeFalse(wireType == WireType.JSON);

        @NotNull TestClass testClass = new TestClass(Boolean.class);

        final Bytes<?> b = Bytes.elasticByteBuffer();
        final Wire wire = createWire(b);
        wire.write().typedMarshallable(testClass);

        @Nullable TestClass o = wire.read().typedMarshallable();
        assertEquals(Boolean.class, o.clazz());

        b.releaseLast();
    }

    @Test
    public void unknownFieldsAreClearedBetweenReadContexts() {
        final Bytes<?> b = Bytes.elasticByteBuffer();
        final Wire wire = createWire(b);

        try (final DocumentContext documentContext = wire.writingDocument()) {
            documentContext.wire().write("first").text("firstValue");
        }
        try (final DocumentContext documentContext = wire.writingDocument()) {
            documentContext.wire().write("second").text("secondValue");
        }

        try (final DocumentContext documentContext = wire.readingDocument()) {
            assertNull(documentContext.wire().read("not_there").text());
        }
        try (final DocumentContext documentContext = wire.readingDocument()) {
            assertNull(documentContext.wire().read("first").text());
        }
    }

    @Test
    public void testReadingPeekYaml() {
        assumeTrue(usePadding);
        assumeTrue(wireType == WireType.BINARY);
        Bytes<?> b = Bytes.elasticByteBuffer();
        final Wire wire = createWire(b);
        assertEquals("", wire.readingPeekYaml());
        try (@NotNull DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("some-data!").marshallable(m -> {
                m.write("some-other-data").int64(0);
                assertEquals("", wire.readingPeekYaml());
            });
        }

        try (@NotNull DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("some-new").marshallable(m -> {
                m.write("some-other--new-data").int64(0);
                assertEquals("", wire.readingPeekYaml());
            });
        }
        assertEquals("", wire.readingPeekYaml());

        try (@NotNull DocumentContext dc = wire.readingDocument()) {
            assertEquals("" +
                    "--- !!data #binary\n" +
                    "some-data!: {\n" +
                    "  some-other-data: 0\n" +
                    "}\n", wire.readingPeekYaml());
            dc.wire().read("some-data");
            assertEquals("" +
                    "--- !!data #binary\n" +
                    "some-data!: {\n" +
                    "  some-other-data: 0\n" +
                    "}\n", wire.readingPeekYaml());

        }
        assertEquals("", wire.readingPeekYaml());

        try (@NotNull DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("some-data!").marshallable(m -> {
                m.write("some-other-data").int64(0);
                assertEquals("", wire.readingPeekYaml());
            });
        }

        try (@NotNull DocumentContext dc = wire.readingDocument()) {
            int position = usePadding ? 40 : 37;
            assertEquals("" +
                    "# position: " + position + ", header: 0\n" +
                    "--- !!data #binary\n" +
                    "some-new: {\n" +
                    "  some-other--new-data: 0\n" +
                    "}\n", wire.readingPeekYaml());
            dc.wire().read("some-data");
            assertEquals("" +
                    "# position: " + position + ", header: 0\n" +
                    "--- !!data #binary\n" +
                    "some-new: {\n" +
                    "  some-other--new-data: 0\n" +
                    "}\n", wire.readingPeekYaml());

        }

        b.releaseLast();
    }

    @Test
    public void isPresentReturnsTrueWhenValueIsPresent() {
        Bytes<?> b = Bytes.elasticByteBuffer();
        final Wire wire = createWire(b);
        wire.write("value").int32(12345);
        assertTrue(wire.read("value").isPresent());
    }

    @Test
    public void isPresentReturnsFalseWhenValueIsNotPresent() {
        Bytes<?> b = Bytes.elasticByteBuffer();
        final Wire wire = createWire(b);
        wire.write("value").int32(12345);
        assertFalse(wire.read("anotherValue").isPresent());
    }

    private Wire createWire(Bytes<?> b) {
        final Wire wire = wireType.apply(b);
        wire.usePadding(usePadding);
        return wire;
    }

    static class TestClass extends SelfDescribingMarshallable {

        Class o;

        TestClass(Class o) {
            this.o = o;
        }

        Class clazz() {
            return o;
        }
    }

    class Circle implements Marshallable {
    }
}
