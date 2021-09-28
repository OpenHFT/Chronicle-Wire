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
import org.junit.*;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.Assert.assertNull;
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

        @NotNull final List<Object[]> list = new ArrayList<>();
        list.add(new Object[]{WireType.BINARY, true});
        list.add(new Object[]{WireType.BINARY, false});
        list.add(new Object[]{WireType.TEXT, false});
             // list.add(new Object[]{WireType.RAW});
        return list;
    }

    @Test
    public void testHexLongNegativeTest() {
        final Bytes b = Bytes.elasticByteBuffer();
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
                Assert.assertEquals(expectedLong1, w);
                long x = dc.wire().read("x").int64();
                Assert.assertEquals(expectedLong2, x);
                Class<Object> y = dc.wire().read("y").typeLiteral();
                Assert.assertEquals(String.class, y);
            }
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void testLenientTypeLiteral() {
        final Bytes b = Bytes.elasticByteBuffer();
        try {
            final Wire wire = createWire(b);

            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().write("w").typeLiteral("DoesntExist");
            }

            try (DocumentContext dc = wire.readingDocument()) {
                Type t = dc.wire().read("w").lenientTypeLiteral();
                Assert.assertEquals("DoesntExist", t.getTypeName());
            }
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void testDate() {
        final Bytes b = Bytes.elasticByteBuffer();
        final Wire wire = createWire(b);

        wire.getValueOut()
                .object(new Date(1234567890000L));
        Assert.assertEquals(new Date(1234567890000L), wire.getValueIn()
                .object(Date.class));

        final Date expectedDate = new Date(1234567890000L);
        String longDateInDefaultLocale = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy").format(expectedDate);
        wire.getValueOut().object(longDateInDefaultLocale);

        Assert.assertEquals(expectedDate, wire.getValueIn()
                .object(Date.class));

        wire.getValueOut().object("2009-02-13 23:31:30.000");

        Assert.assertEquals(new Date(1234567890000L), wire.getValueIn()
                .object(Date.class));
    }

    @Test
    public void testLocalDateTime() {
        final Bytes b = Bytes.elasticByteBuffer();
        try {
            final Wire wire = createWire(b);
            LocalDateTime expected = LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
            wire.getValueOut().object(expected);
            Assert.assertEquals(expected, wire.getValueIn().object());
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void testZonedDateTime() {
        final Bytes b = Bytes.elasticByteBuffer();
        final Wire wire = createWire(b);
        ZonedDateTime expected = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
        wire.getValueOut().object(expected);
        Assert.assertEquals(expected, wire.getValueIn().object());

        b.releaseLast();
    }

    @Test
    public void testSkipValueWithNumbersAndStrings() {

        final Bytes b = Bytes.elasticByteBuffer();
        final Wire wire = createWire(b);

        wire.write("value1").text("text");
        wire.write("number").int64(125);

        StringBuilder field;

        field = new StringBuilder();
        wire.read(field).skipValue();
       // System.out.println("read field=" + field.toString());

        field = new StringBuilder();
        wire.read(field).skipValue();
       // System.out.println("read field=" + field.toString());

        b.releaseLast();
    }

    @Test
    public void testWriteNull() {

        final Bytes b = Bytes.elasticByteBuffer();
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
    public void testClassTypedMarshallableObject() throws Exception {

        @NotNull TestClass testClass = new TestClass(Boolean.class);

        final Bytes b = Bytes.elasticByteBuffer();
        final Wire wire = createWire(b);
        wire.write().typedMarshallable(testClass);

        @Nullable TestClass o = wire.read().typedMarshallable();
        Assert.assertEquals(Boolean.class, o.clazz());

        b.releaseLast();
    }

    @Test
    public void unknownFieldsAreClearedBetweenReadContexts() {
        final Bytes b = Bytes.elasticByteBuffer();
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
        Bytes b = Bytes.elasticByteBuffer();
        final Wire wire = createWire(b);
        Assert.assertEquals("", wire.readingPeekYaml());
        try (@NotNull DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("some-data!").marshallable(m -> {
                m.write("some-other-data").int64(0);
                Assert.assertEquals("", wire.readingPeekYaml());
            });
        }

        try (@NotNull DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("some-new").marshallable(m -> {
                m.write("some-other--new-data").int64(0);
                Assert.assertEquals("", wire.readingPeekYaml());
            });
        }
        Assert.assertEquals("", wire.readingPeekYaml());

        try (@NotNull DocumentContext dc = wire.readingDocument()) {
            Assert.assertEquals("--- !!data #binary\n" +
                    "some-data!: {\n" +
                    "  some-other-data: 0\n" +
                    "}\n", wire.readingPeekYaml());
            dc.wire().read("some-data");
            Assert.assertEquals("--- !!data #binary\n" +
                    "some-data!: {\n" +
                    "  some-other-data: 0\n" +
                    "}\n", wire.readingPeekYaml());

        }
        Assert.assertEquals("", wire.readingPeekYaml());

        try (@NotNull DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("some-data!").marshallable(m -> {
                m.write("some-other-data").int64(0);
                Assert.assertEquals("", wire.readingPeekYaml());
            });
        }

        try (@NotNull DocumentContext dc = wire.readingDocument()) {
            int position = usePadding ? 40 : 37;
            Assert.assertEquals("# position: "+position+", header: 0\n" +
                    "--- !!data #binary\n" +
                    "some-new: {\n" +
                    "  some-other--new-data: 0\n" +
                    "}\n", wire.readingPeekYaml());
            dc.wire().read("some-data");
            Assert.assertEquals("# position: "+position+", header: 0\n" +
                    "--- !!data #binary\n" +
                    "some-new: {\n" +
                    "  some-other--new-data: 0\n" +
                    "}\n", wire.readingPeekYaml());

        }

        b.releaseLast();
    }

    private Wire createWire(Bytes b) {
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
}
