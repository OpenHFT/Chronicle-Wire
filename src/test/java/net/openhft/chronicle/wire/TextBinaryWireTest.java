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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.ObjIntConsumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

@RunWith(value = Parameterized.class)
public class TextBinaryWireTest extends WireTestCommon {

    private final WireType wireType;

    public TextBinaryWireTest(WireType wireType) {

        this.wireType = wireType;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> combinations() {
        Object[][] list = {
                {WireType.BINARY},
                {WireType.FIELDLESS_BINARY},
                {WireType.RAW},
                {WireType.TEXT},
                {WireType.YAML},
                {WireType.JSON}
        };
        return Arrays.asList(list);
    }

    @Test
    public void testValueOf() {
        Wire wire = createWire();
        @NotNull WireType wt = WireType.valueOf(wire);
        assertEquals(wireType, wt);
        wire.bytes().releaseLast();

    }

    public Wire createWire() {
        final Wire wire = wireType.apply(Bytes.elasticByteBuffer());
        wire.usePadding(wire.isBinary());
        return wire;
    }

    @Test
    public void readingDocumentLocation() {
        Wire wire = createWire();
        if (wire instanceof TextWire)
            ((TextWire) wire).useBinaryDocuments();

        wire.writeDocument(true, w -> w.write("header").text("data"));
        long position = wire.bytes().writePosition();
        wire.writeDocument(false, w -> w.write("message").text("text"));

        try (DocumentContext dc = wire.readingDocument(position)) {
            assertEquals("text", dc.wire().read(() -> "message").text());
        }
        wire.bytes().releaseLast();
    }

    @Test
    public void testReadComment() {
        assumeTrue(wireType == WireType.TEXT || wireType == WireType.BINARY || wireType == WireType.YAML);

        Wire wire = createWire();
        wire.writeComment("This is a comment");
        @NotNull StringBuilder sb = new StringBuilder();
        wire.readComment(sb);
        assertEquals("This is a comment", sb.toString());

        wire.bytes().releaseLast();
    }

    @Test
    public void readFieldAsObject() {
        assumeFalse(wireType == WireType.RAW || wireType == WireType.FIELDLESS_BINARY);

        Wire wire = createWire();
        wire.write("CLASS").text("class")
                .write("RUNTIME").text("runtime");
        assertEquals(RetentionPolicy.CLASS, wire.readEvent(RetentionPolicy.class));
        assertEquals("class", wire.getValueIn().text());
        assertEquals(RetentionPolicy.RUNTIME, wire.readEvent(RetentionPolicy.class));
        assertEquals("runtime", wire.getValueIn().text());

        assertNull(wire.readEvent(RetentionPolicy.class));

        wire.bytes().releaseLast();
    }

    @Test
    public void readFieldAsLong() {
        assumeFalse(wireType == WireType.RAW || wireType == WireType.FIELDLESS_BINARY);

        Wire wire = createWire();
        // todo fix to ensure a field number is used.
        wire.writeEvent(Long.class, 1L).text("class")
                .writeEvent(Long.class, 2L).text("runtime");

        assertEquals((Long) 1L, wire.readEvent(Long.class));
        assertEquals("class", wire.getValueIn().text());
        @NotNull StringBuilder sb = new StringBuilder();
        wire.readEventName(sb);
        assertEquals("2", sb.toString());
        assertEquals("runtime", wire.getValueIn().text());

        assertNull(wire.readEvent(RetentionPolicy.class));

        wire.bytes().releaseLast();
    }

    @Test
    public void testConvertToNum() {
        assumeFalse(wireType == WireType.RAW || /* No support for bool conversions */ wireType == WireType.YAML);

        Wire wire = createWire();
        wire.write("a").bool(false)
                .write("b").bool(true)
                .write("c").float32(2.0f)
                .write("d").float64(3.0);

        @NotNull final ObjIntConsumer<Integer> assertEquals = (expected, actual) -> Assert.assertEquals((long) expected, actual);
        wire.read(() -> "a").int32(0, assertEquals);
        wire.read(() -> "b").int32(1, assertEquals);
        wire.read(() -> "c").int32(2, assertEquals);
        wire.read(() -> "d").int32(3, assertEquals);

        wire.bytes().releaseLast();
    }
}

