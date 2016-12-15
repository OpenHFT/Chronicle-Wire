/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Rob Austin.
 */

@RunWith(value = Parameterized.class)
public class WireTests {

    private final WireType wireType;

    @Rule
    public TestName name = new TestName();

    public WireTests(WireType wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {

        final List<Object[]> list = new ArrayList<>();
        list.add(new Object[]{WireType.BINARY});
        list.add(new Object[]{WireType.TEXT});
        //      list.add(new Object[]{WireType.RAW});
        return list;
    }

    @Test
    public void testDate() {
        final Bytes b = Bytes.elasticByteBuffer();
        final Wire wire = wireType.apply(b);
        wire.getValueOut()
                .object(new Date(0));
        Assert.assertEquals(new Date(0), wire.getValueIn()
                .object());
    }


    @Test
    public void testLocalDateTime() {
        final Bytes b = Bytes.elasticByteBuffer();
        final Wire wire = wireType.apply(b);
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
        wire.getValueOut().object(expected);
        Assert.assertEquals(expected, wire.getValueIn().object());
    }


    @Test
    public void testZonedDateTime() {
        final Bytes b = Bytes.elasticByteBuffer();
        final Wire wire = wireType.apply(b);
        ZonedDateTime expected = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());
        wire.getValueOut().object(expected);
        Assert.assertEquals(expected, wire.getValueIn().object());
    }


    @Test
    public void testWriteNull() {

        final Bytes b = Bytes.elasticByteBuffer();
        final Wire wire = wireType.apply(b);
        wire.write().object(null);
        wire.write().object(null);
        wire.write().object(null);
        wire.write().object(null);

        Object o = wire.read().object(Object.class);
        Assert.assertEquals(null, o);
        String s = wire.read().object(String.class);
        Assert.assertEquals(null, s);
        RetentionPolicy rp = wire.read().object(RetentionPolicy.class);
        Assert.assertEquals(null, rp);
        Circle c = wire.read().object(Circle.class);  // this fails without the check.
        Assert.assertEquals(null, c);
    }

    @Test
    public void testClassTypedMarshallableObject() throws Exception {

        TestClass testClass = new TestClass(Boolean.class);

        final Bytes b = Bytes.elasticByteBuffer();
        final Wire wire = wireType.apply(b);
        wire.write().typedMarshallable(testClass);

        TestClass o = wire.read().typedMarshallable();
        Assert.assertEquals(Boolean.class, o.clazz());
    }

    @Test
    public void testReadingPeekYaml() {
        Bytes b = Bytes.elasticByteBuffer();
        BinaryWire wire = (BinaryWire) WireType.BINARY.apply(b);
        Assert.assertEquals("", wire.readingPeekYaml());
        try (DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("some-data").marshallable(m -> {
                m.write("some-other-data").int64(0);
                Assert.assertEquals("", wire.readingPeekYaml());
            });
        }

        try (DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("some-new").marshallable(m -> {
                m.write("some-other--new-data").int64(0);
                Assert.assertEquals("", wire.readingPeekYaml());
            });
        }
        Assert.assertEquals("", wire.readingPeekYaml());

        try (DocumentContext dc = wire.readingDocument()) {
            Assert.assertEquals("--- !!data #binary\n" +
                    "some-data: {\n" +
                    "  some-other-data: 0\n" +
                    "}\n", wire.readingPeekYaml());
            dc.wire().read("some-data");
            Assert.assertEquals("--- !!data #binary\n" +
                    "some-data: {\n" +
                    "  some-other-data: 0\n" +
                    "}\n", wire.readingPeekYaml());

        }
        Assert.assertEquals("", wire.readingPeekYaml());


        try (DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("some-data").marshallable(m -> {
                m.write("some-other-data").int64(0);
                Assert.assertEquals("", wire.readingPeekYaml());
            });
        }

        try (DocumentContext dc = wire.readingDocument()) {
            Assert.assertEquals("# position: 36, header: 0\n" +
                    "--- !!data #binary\n" +
                    "some-new: {\n" +
                    "  some-other--new-data: 0\n" +
                    "}\n", wire.readingPeekYaml());
            dc.wire().read("some-data");
            Assert.assertEquals("# position: 36, header: 0\n" +
                    "--- !!data #binary\n" +
                    "some-new: {\n" +
                    "  some-other--new-data: 0\n" +
                    "}\n", wire.readingPeekYaml());

        }
    }

    static class TestClass extends AbstractMarshallable {

        Class o;

        TestClass(Class o) {
            this.o = o;
        }

        Class clazz() {
            return o;
        }
    }
}
