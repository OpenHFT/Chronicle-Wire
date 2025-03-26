/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
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
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Field;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;

import static net.openhft.chronicle.bytes.Bytes.allocateElasticDirect;
import static net.openhft.chronicle.bytes.Bytes.allocateElasticOnHeap;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

@SuppressWarnings({"rawtypes","try"})
@RunWith(value = Parameterized.class)
public class BinaryWire2Test extends WireTestCommon {
    final boolean usePadding;
    @NotNull
    Bytes<?> bytes = new HexDumpBytes();

    // Constructor to set the padding parameter
    public BinaryWire2Test(boolean usePadding) {
        this.usePadding = usePadding;
    }

    // Collection of padding parameters for the tests
    @Parameterized.Parameters(name = "usePadding={0}")
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{true},
                new Object[]{false}
        );
    }

    // Create a new BinaryWire instance based on the current test configuration
    @SuppressWarnings("deprecation")
    @NotNull
    private BinaryWire createWire() {
        bytes.clear();
        @NotNull BinaryWire wire = new BinaryWire(bytes, false, false, false, 32, "lzw");
        wire.usePadding(usePadding);
        return wire;
    }

    // Test writing an object that is not marshallable and expecting an IllegalArgumentException
    @Test(expected = IllegalArgumentException.class)
    public void unmarshallableObject() {
        BinaryWire wire = createWire();
        wire.getValueOut().object(new Object());
    }

    // Test various reading length scenarios for different BinaryWireCode values
    @Test
    public void testReadLength() {
        Map<Integer, String> wireCodes = new TreeMap<>();
        for (Field field : BinaryWireCode.class.getDeclaredFields()) {
            if (field.getType() == int.class)
                try {
                    wireCodes.put(field.getInt(null), field.getName());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
        }
        wireCodes.remove(BinaryWireCode.U8_ARRAY); // should always be nested
        wireCodes.remove(BinaryWireCode.I64_ARRAY); // should always be nested
        wireCodes.remove(BinaryWireCode.FIELD_NAME_ANY); // should always be nested
        wireCodes.remove(BinaryWireCode.FIELD_NAME0); // should always be nested
        wireCodes.remove(BinaryWireCode.FIELD_NAME31); // should always be nested
        wireCodes.remove(BinaryWireCode.EVENT_OBJECT); // should always be nested
        wireCodes.remove(BinaryWireCode.EVENT_NAME); // should always be nested
        wireCodes.remove(BinaryWireCode.FIELD_NUMBER); // should always be nested

        wireCodes.remove(BinaryWireCode.PADDING32); // should always be consumed
        wireCodes.remove(BinaryWireCode.PADDING); // should always be consumed
        wireCodes.remove(BinaryWireCode.COMMENT); // should always be consumed
        wireCodes.remove(BinaryWireCode.HINT); // should always be consumed
        wireCodes.remove(BinaryWireCode.FLOAT_SET_LOW_0); // used by Delta Wire
        wireCodes.remove(BinaryWireCode.FLOAT_SET_LOW_2); // used by Delta Wire
        wireCodes.remove(BinaryWireCode.FLOAT_SET_LOW_4); // used by Delta Wire
        wireCodes.remove(BinaryWireCode.SET_LOW_INT8); // used by Delta Wire
        wireCodes.remove(BinaryWireCode.SET_LOW_INT16); // used by Delta Wire

        List<Consumer<ValueOut>> writeValue = Arrays.asList(
                v -> v.bool(false),
                v -> v.bool(true),
                v -> v.time(LocalTime.MAX),
                v -> v.date(LocalDate.MIN),
                v -> v.dateTime(LocalDateTime.MIN),
                v -> v.zonedDateTime(ZonedDateTime.now()),
                v -> v.marshallable(w -> {
                }),
                v -> v.set(new TreeSet<>()),
                v -> v.object(null),
                v -> v.text(""),
                v -> v.text("0123456789012345678901234567890"),
                v -> v.text("0123456789012345678901234567890a"),
                v -> v.typeLiteral(String.class),

                v -> v.bytes(new byte[1]),
                v -> v.bytes(new byte[257]),
                v -> v.bytes(new byte[65540]),
                v -> v.array(new long[4], 4),
                v -> v.float64(0.01),
                v -> v.float64(2.01),
                v -> v.float64(1e-4),
                v -> v.float64(2.001),
                v -> v.float64(1e-6),
                v -> v.float64(2.00001),
                v -> v.float64(1001, 1000),
                v -> v.float64(1000.01, 1000),
                v -> v.uint8(1),
                v -> v.uint8(130),
                v -> v.int8(-120),
                v -> v.uint16(257),
                v -> v.uint32((1 << 15) + 1),
                v -> v.uint32((1 << 16) + 1),
                v -> v.uint32(Integer.MAX_VALUE + 1L),
                v -> v.int64(Long.MIN_VALUE + 1),
                v -> v.int64_0x(Integer.MAX_VALUE + 1L),
                v -> v.float32((float) Math.PI),
                v -> v.float64(Math.PI),
                v -> v.uuid(UUID.randomUUID())
        );
        Wire wire = createWire();
        Wire wire2 = WireType.TEXT.apply(Bytes.allocateElasticOnHeap(32));

        for (Consumer<ValueOut> value : writeValue) {
            wire.clear();
            wire2.clear();
            value.accept(wire.getValueOut());
            wireCodes.remove(wire.bytes().peekUnsignedByte());
            value.accept(wire2.getValueOut());
            wire.bytes().writeByte((byte) 0);
            long readLength = wire.getValueIn().readLength();
            assertEquals(wire2.toString(), wire.bytes().readRemaining() - 1, readLength);
        }
        if (!wireCodes.isEmpty()) {
            System.err.println("Untested codes");
            wireCodes.forEach((k, v) -> System.err.println(v + "= " + Integer.toHexString(k)));
        }
    }

    // Test the reading and writing of boolean values including null
    @Test
    public void testBool() {
        @NotNull Wire wire = createWire();
        wire.write().bool(false)
                .write().bool(true)
                .write().bool(null);

        wire.read().bool("error", Assert::assertFalse)
                .read().bool("error", Assert::assertTrue)
                .read().bool("error", Assert::assertNull);
    }

    // Test writing and reading a BytesStore
    @Test
    public void testBytesStore() {
        @NotNull Wire wire = createWire();
        wire.write().object(Bytes.from("Hello"));

        Bytes<?> b = Bytes.elasticByteBuffer();
        wire.read().bytes(b);
        assertEquals("Hello", b.toString());
        b.releaseLast();
    }

    // Test the serialization and deserialization of an object containing a TreeMap
    @Test
    public void writeObjectWithTreeMap() {
        @NotNull Wire wire = createWire();
        ObjectWithTreeMap value = new ObjectWithTreeMap();
        value.map.put("hello", "world");
        wire.write().object(value);
        // System.out.println(Bytes.);
        ObjectWithTreeMap value2 = new ObjectWithTreeMap();
        wire.read().object(value2, ObjectWithTreeMap.class);
        assertEquals("{hello=world}", value2.map.toString());

        wire.bytes().readPosition(0);
        ObjectWithTreeMap value3 = new ObjectWithTreeMap();
        wire.read().object(value3, Object.class);
        assertEquals("{hello=world}", value3.map.toString());

        wire.bytes().readPosition(0);
        ObjectWithTreeMap value4 = wire.read().object(ObjectWithTreeMap.class);
        assertEquals("{hello=world}", value4.map.toString());
    }

    // Test reading and writing of 32-bit float values
    @Test
    public void testFloat32() {
        @NotNull Wire wire = createWire();
        wire.write().float32(0.0F)
                .write().float32(Float.NaN)
                .write().float32(Float.POSITIVE_INFINITY);

        wire.read().float32(this, (o, t) -> assertEquals(0.0F, t, 0.0F))
                .read().float32(this, (o, t) -> assertTrue(Float.isNaN(t)))
                .read().float32(this, (o, t) -> assertEquals(Float.POSITIVE_INFINITY, t, 0.0F));
    }

    // Test writing and reading of a NaN double value
    @Test
    public void testNaN() {
        @NotNull Wire wire = createWire();
        wire.getValueOut()
                .float64(Double.NaN);
        assertEquals(5, wire.bytes().readRemaining());
        assertTrue(Double.isNaN(wire.getValueIn().float64()));
    }

    // Test reading and writing of LocalTime values
    @Test
    public void testTime() {
        @NotNull Wire wire = createWire();
        LocalTime now = LocalTime.now();
        wire.write().time(now)
                .write().time(LocalTime.MAX)
                .write().time(LocalTime.MIN);

        wire.read().time(now, Assert::assertEquals)
                .read().time(LocalTime.MAX, Assert::assertEquals)
                .read().time(LocalTime.MIN, Assert::assertEquals);
    }

    // Test reading and writing of ZonedDateTime values
    @Test
    public void testZonedDateTime() {
        @NotNull Wire wire = createWire();
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime max = ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault());
        ZonedDateTime min = ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault());
        wire.write().zonedDateTime(now)
                .write().zonedDateTime(max)
                .write().zonedDateTime(min);

        wire.read().zonedDateTime(now, Assert::assertEquals)
                .read().zonedDateTime(max, Assert::assertEquals)
                .read().zonedDateTime(min, Assert::assertEquals);
    }

    // Test reading and writing of LocalDate values
    @Test
    public void testLocalDate() {
        @NotNull Wire wire = createWire();
        LocalDate now = LocalDate.now();
        wire.write().date(now)
                .write().date(LocalDate.MAX)
                .write().date(LocalDate.MIN);

        wire.read().date(now, Assert::assertEquals)
                .read().date(LocalDate.MAX, Assert::assertEquals)
                .read().date(LocalDate.MIN, Assert::assertEquals);
    }

    // Test reading and writing of java.util.Date values
    @Test
    public void testDate() {
        @NotNull Wire wire = createWire();

        try (final DocumentContext dc = wire.writingDocument(true)) {
            dc.wire().write().object(new Date(1234567890000L));
        }
        try (final DocumentContext dc = wire.readingDocument()) {
            // System.out.println(Wires.fromSizePrefixedBlobs(dc));
            Assert.assertEquals(1234567890000L, dc.wire().read().object(Date.class).getTime());
        }
    }

    // Test reading java.util.Date from a given string representation
    @Test
    public void testDateExisting() {
        final String dateString = "1999-12-31";
        final java.util.Date expected = java.sql.Date.valueOf(dateString);
        @NotNull Wire wire = createWire();

        try (final DocumentContext dc = wire.writingDocument(true)) {
            dc.wire().write().text(dateString);
        }
        try (final DocumentContext dc = wire.readingDocument()) {
            Assert.assertEquals(expected.getTime(), dc.wire().read().object(Date.class).getTime());
        }
    }

    // Test reading and writing UUID values
    @Test
    public void testUuid() {
        @NotNull Wire wire = createWire();
        UUID uuid = UUID.randomUUID();
        wire.write().uuid(uuid)
                .write().uuid(new UUID(0, 0))
                .write().uuid(new UUID(Long.MAX_VALUE, Long.MAX_VALUE));

        wire.read().uuid(this, (o, t) -> assertEquals(uuid, t))
                .read().uuid(this, (o, t) -> assertEquals(new UUID(0, 0), t))
                .read().uuid(this, (o, t) -> assertEquals(new UUID(Long.MAX_VALUE, Long.MAX_VALUE), t));
    }

    // Test writing sequences in both binary and text format with Chronicle Wire
    @Test
    public void testSequence() {
        @NotNull Wire wire = createWire();
        writeMessage(wire);
        assertEquals("" +
                        "--- !!meta-data #binary\n" +
                        "csp: //path/service\n" +
                        "tid: 123456789\n" +
                        "# position: 32, header: 0\n" +
                        "--- !!data #binary\n" +
                        "entrySet: [\n" +
                        "  {\n" +
                        "    key: key-1,\n" +
                        "    value: value-1\n" +
                        "  },\n" +
                        "  {\n" +
                        "    key: key-2,\n" +
                        "    value: value-2\n" +
                        "  }\n" +
                        "]\n",
                Wires.fromSizePrefixedBlobs(wire));

        @NotNull Wire twire = WireType.TEXT.apply(Bytes.elasticByteBuffer());
        writeMessage(twire);
        assertEquals("" +
                        "--- !!meta-data\n" +
                        "csp: //path/service\n" +
                        "tid: 123456789\n" +
                        "# position: 40, header: 0\n" +
                        "--- !!data\n" +
                        "entrySet: [\n" +
                        "  {\n" +
                        "    key: key-1,\n" +
                        "    value: value-1\n" +
                        "  },\n" +
                        "  {\n" +
                        "    key: key-2,\n" +
                        "    value: value-2\n" +
                        "  }\n" +
                        "]\n",
                Wires.fromSizePrefixedBlobs(twire));

        wire.bytes().releaseLast();
        twire.bytes().releaseLast();
    }

    // Helper function to write sample messages to a given wire
    private void writeMessage(@NotNull WireOut wire) {
        wire.writeDocument(true, w -> w
                .write("csp").text("//path/service")
                .write("tid").int64(123456789));
        wire.writeDocument(false, w -> w
                .write("entrySet").sequence(s -> {
                    s.marshallable(m -> m
                            .write("key").text("key-1")
                            .write("value").text("value-1"));
                    s.marshallable(m -> m
                            .write("key").text("key-2")
                            .write("value").text("value-2"));
                }));
    }

    // Test writing messages with padding and validate their binary and text representations
    @Test
    public void testSequenceContext() {
        assumeTrue(usePadding);
        @NotNull Wire wire = createWire();
        writeMessageContext(wire);

        // Expected binary hex representation of the written data
        assertEquals("" +
                        "1c 00 00 40                                     # msg-length\n" +
                        "c3 63 73 70                                     # csp:\n" +
                        "ee 2f 2f 70 61 74 68 2f 73 65 72 76 69 63 65    # //path/service\n" +
                        "c3 74 69 64                                     # tid:\n" +
                        "a6 15 cd 5b 07                                  # 123456789\n" +
                        "48 00 00 00                                     # msg-length\n" +
                        "c8 65 6e 74 72 79 53 65 74                      # entrySet:\n" +
                        "82 3a 00 00 00                                  # sequence\n" +
                        "82 18 00 00 00                                  # Marshallable\n" +
                        "c3 6b 65 79                                     # key:\n" +
                        "e5 6b 65 79 2d 31                               # key-1\n" +
                        "c5 76 61 6c 75 65                               # value:\n" +
                        "e7 76 61 6c 75 65 2d 31                         # value-1\n" +
                        "82 18 00 00 00                                  # Marshallable\n" +
                        "c3 6b 65 79                                     # key:\n" +
                        "e5 6b 65 79 2d 32                               # key-2\n" +
                        "c5 76 61 6c 75 65                               # value:\n" +
                        "e7 76 61 6c 75 65 2d 32                         # value-2\n",
                wire.bytes().toHexString());

        @NotNull Wire twire = WireType.TEXT.apply(Bytes.elasticByteBuffer());
        writeMessageContext(twire);

        // Expected textual representation of the written data
        assertEquals("" +
                        "--- !!meta-data\n" +
                        "csp: //path/service\n" +
                        "tid: 123456789\n" +
                        "# position: 39, header: 0\n" +
                        "#  has a 4 byte size prefix, 25856 > 102 len is 25856",
                Wires.fromSizePrefixedBlobs(twire.bytes()));

        wire.bytes().releaseLast();
        twire.bytes().releaseLast();
    }

    // Helper function to write sample messages with a DocumentContext to a given wire
    private void writeMessageContext(@NotNull WireOut wire) {
        // Writing meta-data
        try (DocumentContext ignored = wire.writingDocument(true)) {
            wire.write("csp").text("//path/service")
                    .write("tid").int64(123456789);
        }
        // Writing the actual data
        try (DocumentContext ignored = wire.writingDocument(false)) {
            wire.write("entrySet").sequence(s -> {
                s.marshallable(m -> m
                        .write("key").text("key-1")
                        .write("value").text("value-1"));
                s.marshallable(m -> m
                        .write("key").text("key-2")
                        .write("value").text("value-2"));
            });
        }
    }

    // Test the behavior of enums within the Wire system
    @Test
    public void testEnum() {
        @NotNull Wire wire = createWire();
        wire.write().object(WireType.BINARY)
                .write().object(WireType.TEXT)
                .write().object(WireType.RAW);

        // Validate that the enums have been correctly written and can be read back as expected
        assertEquals(WireType.BINARY, wire.read()
                .object(Object.class));
        assertEquals(WireType.TEXT, wire.read().object(Object.class));
        assertEquals(WireType.RAW, wire.read().object(Object.class));
    }

    // Test the serialization behavior when there's text data followed by field data
    @Test
    public void fieldAfterText() {
        assumeFalse(usePadding);  // Ensure padding is not used for this test

        @NotNull Wire wire = createWire();
        wire.writeDocument(false, w -> w.write("data")
                .typePrefix("!UpdateEvent").marshallable(
                        v -> v.write("assetName").text("/name")
                                .write("key").object("test")
                                .write("oldValue").object("world1")
                                .write("value").object("world2")));

        // Validate the serialized format of the written document
        assertEquals("--- !!data #binary\n" +
                "data: !!UpdateEvent {\n" +
                "  assetName: /name,\n" +
                "  key: test,\n" +
                "  oldValue: world1,\n" +
                "  value: world2\n" +
                "}\n", Wires.fromSizePrefixedBlobs(wire.bytes()));

        // Read back the document and assert each field's value
        wire.readDocument(null, w -> w.read(() -> "data").typePrefix(this, (o, t) -> assertEquals("!UpdateEvent", t.toString())).marshallable(
                m -> m.read(() -> "assetName").object(String.class, "/name", Assert::assertEquals)
                        .read(() -> "key").object(String.class, "test", Assert::assertEquals)
                        .read(() -> "oldValue").object(String.class, "world1", Assert::assertEquals)
                        .read(() -> "value").object(String.class, "world2", Assert::assertEquals)));
    }

    // Test the serialization behavior when there's a null field followed by another field
    @Test
    public void fieldAfterNull() {
        @NotNull Wire wire = createWire();
        wire.writeDocument(false, w -> w.write("data").typedMarshallable("!UpdateEvent",
                v -> v.write("assetName").text("/name")
                        .write("key").object("test")
                        .write("oldValue").object(null)
                        .write("value").object("world2")));

        // Validate the serialized format of the document, especially the null field representation
        assertEquals("--- !!data #binary\n" +
                "data: !!UpdateEvent {\n" +
                "  assetName: /name,\n" +
                "  key: test,\n" +
                "  oldValue: !!null \"\",\n" +
                "  value: world2\n" +
                "}\n", Wires.fromSizePrefixedBlobs(wire.bytes()));

        // Read back the document, especially ensuring the null field is read back correctly
        wire.readDocument(null, w -> w.read(() -> "data").typePrefix(this, (o, t) -> assertEquals("!UpdateEvent", t.toString())).marshallable(
                m -> m.read(() -> "assetName").object(String.class, "/name", Assert::assertEquals)
                        .read(() -> "key").object(String.class, "test", Assert::assertEquals)
                        .read(() -> "oldValue").object(String.class, "error", Assert::assertNull)
                        .read(() -> "value").object(String.class, "world2", Assert::assertEquals)));
    }

    // Test the serialization behavior when there's a null field in the context of other metadata and data fields
    @Test
    public void fieldAfterNullContext() {
        // Ignore a specific exception that might occur during this test
        ignoreException("Unable to copy object safely, message will not be repeated: " +
                "net.openhft.chronicle.core.util.ClassNotFoundRuntimeException: java.lang.ClassNotFoundException: !UpdateEvent");
        @NotNull Wire wire = createWire();

        // Write metadata containing an 'tid' field to the wire
        try (DocumentContext ignored = wire.writingDocument(true)) {
            wire.write("tid").int64(1234567890L);
        }

        // Write main data, which includes a null field, to the wire
        try (DocumentContext ignored = wire.writingDocument(false)) {
            wire.write("data").typedMarshallable("!UpdateEvent",
                    v -> v.write("assetName").text("/name")
                            .write("key").object("test")
                            .write("oldValue").object(null)
                            .write("value").object("world2"));
        }

        // Validate the serialized format of the entire wire content, including metadata and data
        assertEquals("" +
                        "--- !!meta-data #binary\n" +
                        "tid: 1234567890\n" +
                        "# position: 1X, header: 0\n" +
                        "--- !!data #binary\n" +
                        "data: !!UpdateEvent {\n" +
                        "  assetName: /name,\n" +
                        "  key: test,\n" +
                        "  oldValue: !!null \"\",\n" +
                        "  value: world2\n" +
                        "}\n",
                Wires.fromSizePrefixedBlobs(wire).replaceAll("position: 1\\d", "position: 1X"));

        // Read back the metadata and assert its content
        try (DocumentContext context = wire.readingDocument()) {
            assertTrue(context.isPresent());
            assertTrue(context.isMetaData());
            Assert.assertEquals(1234567890L, wire.read(() -> "tid").int64());
        }

        // Read back the main data and assert each field's value
        try (DocumentContext context = wire.readingDocument()) {
            assertTrue(context.isPresent());
            assertTrue(context.isData());
            wire.read(() -> "data").typePrefix(this, (o, t) -> assertEquals("!UpdateEvent", t.toString())).marshallable(
                    m -> m.read(() -> "assetName").object(String.class, "/name", Assert::assertEquals)
                            .read(() -> "key").object(String.class, "test", Assert::assertEquals)
                            .read(() -> "oldValue").object(String.class, "error", Assert::assertNull)
                            .read(() -> "value").object(String.class, "world2", Assert::assertEquals));
        }

        // Ensure no more data is available in the wire
        try (DocumentContext context = wire.readingDocument()) {
            assertFalse(context.isPresent());
        }
    }

    // Test the behavior of reading and writing a demarshallable object
    @Test
    public void readDemarshallable() {
        @NotNull Wire wire = createWire();

        // Write a DemarshallableObject instance to the wire
        try (DocumentContext $ = wire.writingDocument(true)) {
            wire.getValueOut().typedMarshallable(new DemarshallableObject("test", 123456));
        }

        // Validate the serialized format of the written object
        assertEquals("--- !!meta-data #binary\n" +
                "!net.openhft.chronicle.wire.DemarshallableObject {\n" +
                "  name: test,\n" +
                "  value: 123456\n" +
                "}\n", Wires.fromSizePrefixedBlobs(wire));

        // Read back the DemarshallableObject instance and validate its content
        try (DocumentContext $ = wire.readingDocument()) {
            @Nullable DemarshallableObject dobj = wire.getValueIn().typedMarshallable();
            assertEquals("test", dobj.name);
            assertEquals(123456, dobj.value);
        }
    }

    // Test the behavior of Gzip compression within the Wire system
    @Test
    public void testCompressWithGzip() {
        @NotNull Wire wire = createWire();

        // Create a repetitive string and convert it to Bytes
        @NotNull String s = "xxxxxxxxxxxxxxxx" +
                "xxxxxxxxxxxxxxxx" +
                "xxxxxxxxxxxxxxxx" +
                "xxxxxxxxxxxxxxxx" +
                "xxxxxxxxxxxxxxxx";
        Bytes<?> str = Bytes.from(s);

        // Write the string to the wire using Gzip compression
        wire.write("message").compress("gzip", str);

        // Read back the compressed string and validate its content
        wire.bytes().readPosition(0);
        @Nullable String str2 = wire.read("message").text();
        assertEquals(s, str2);

        // Convert the compressed content to plain text format and validate
        wire.bytes().readPosition(0);
        Bytes<?> asText = Bytes.elasticByteBuffer();
        wire.copyTo(WireType.TEXT.apply(asText));
        assertEquals("message: # gzip\n" + s +
                "\n", asText.toString());
        asText.releaseLast();
        str.releaseLast();
    }

    // Test the behavior when data is compressed using the "binary" scheme (likely no compression)
    @Test
    public void testBinaryCompression() {
        testCompression("binary");
    }

    // Test the behavior when data is compressed using the Gzip scheme
    @Test
    public void testGzipCompression() {
        testCompression("gzip");
    }

    // Test the behavior when data is compressed using the LZW scheme
    @Test
    public void testLzwCompression() {
        testCompression("lzw");
    }

    /**
     * Tests the behavior of different compression schemes within the Wire system.
     *
     * @param comp Compression scheme ("binary", "gzip", or "lzw")
     */
    public void testCompression(String comp) {
        bytes.clear();
        @NotNull Wire wire = new BinaryWire(bytes, false, false, false, 32, comp);

        // Create a repetitive string and convert it to BytesStore
        @NotNull String str = "xxxxxxxxxxxxxxxx2xxxxxxxxxxxxxxxxxxxxxxxxxxyyyyyyyyyyyyyyyyyyyyyy2yyyyyyyyyyyyyyyyy";
        BytesStore<?, ?> bytes = Bytes.from(str);

        // Write the string to the wire using the specified compression
        wire.write().bytes(bytes);

        // If compression is used (i.e., not binary), verify that the compressed size is smaller
        if (!comp.equals("binary"))
            assertTrue(wire.bytes().readRemaining() + " >= " + str.length(),
                    wire.bytes().readRemaining() < str.length());

        // Read back the compressed string and validate its content
        wire.bytes().readPosition(0);
        String str2 = wire.read().text();
        assertEquals(str, str2);
        bytes.releaseLast();
    }

    // Test the behavior of storing and retrieving a byte array containing negative values
    @Test
    public void testByteArrayValueWithRealBytesNegative() {
        @NotNull Wire wire = createWire();

        // Create an array of negative bytes
        @NotNull final byte[] expected = {-1, -2, -3, -4, -5, -6, -7};

        // Write the byte array to the wire under a key named "1"
        wire.writeDocument(false, wir -> wir.writeEventName(() -> "put")
                .marshallable(w -> w.write("key").text("1")
                        .write("value")
                        .object(expected)));
        // System.out.println(wire);

        // Read back the stored byte array and validate its content
        wire.readDocument(null, wir -> wire.read(() -> "put")
                .marshallable(w -> w.read(() -> "key").object(Object.class, "1", Assert::assertEquals)
                        .read(() -> "value").object(Object.class, expected, (e, v) -> {
                            Assert.assertArrayEquals(e, (byte[]) v);
                        })));
    }

    // Test the behavior of writing and reading from a wire using byte arrays of varying sizes
    @Test
    public void testBytesArray() {
        @NotNull Wire wire = createWire();
        @NotNull Random rand = new Random();
        for (int i = 0; i < 70000; i += rand.nextInt(i + 1) + 1) {
            // System.out.println(i);
            wire.clear();
            @NotNull final byte[] fromBytes = new byte[i];
            wire.writeDocument(false, w -> w.write("bytes").bytes(fromBytes));
            Wires.fromSizePrefixedBlobs(wire);
            int finalI = i;
            wire.readDocument(null, w -> assertEquals(finalI, w.read("bytes").bytes().length));
        }
    }

    // Test the writing and reading of a small array on the wire
    @Test
    public void testSmallArray() {
        @NotNull Wire wire = createWire();
        wire.writeDocument(false, w -> w.write("index")
                .int64array(10));
        assertEquals("--- !!data #binary\n" +
                "index: [\n" +
                "  # length: 10, used: 0\n" +
                "  0, 0, 0, 0, 0, 0, 0, 0, 0, 0\n" +
                "]\n", Wires.fromSizePrefixedBlobs(wire.bytes()));
    }

    // Test the writing and reading of different type literals on the wire
    @Test
    public void testTypeLiteral() {
        assumeFalse(usePadding);

        @NotNull Wire wire = createWire();
        wire.writeDocument(false, w -> w.write("a").typeLiteral(String.class)
                .write("b").typeLiteral(int.class)
                .write("c").typeLiteral(byte[].class)
                .write("d").typeLiteral(Double[].class)
                .write("z").typeLiteral((Class) null));
        assertEquals("--- !!data #binary\n" +
                "a: !type String\n" +
                "b: !type int\n" +
                "c: !type \"byte[]\"\n" +
                "d: !type \"[Ljava.lang.Double;\"\n" +
                "z: !!null \"\"\n", Wires.fromSizePrefixedBlobs(wire.bytes()));
    }

    // Test the behavior of writing and reading byte arrays of specific sizes and values
    @Test
    public void testByteArray() {
        assumeFalse(usePadding);
        @NotNull Wire wire = createWire();
        wire.writeDocument(false, w -> w.write("nothing").object(new byte[0]));
        @NotNull byte[] one = {1};
        wire.writeDocument(false, w -> w.write("one").object(one));
        @NotNull byte[] thirtytwo = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32};
        wire.writeDocument(false, w -> w.write("four").object(thirtytwo));

        final String expected = usePadding ?
                "" +
                        "--- !!data #binary\n" +
                        "nothing: !byte[] \"\"\n" +
                        "# position: 24, header: 1\n" +
                        "--- !!data #binary\n" +
                        "one: !byte[] \"\\x01\"\n" +
                        "# position: 44, header: 2\n" +
                        "--- !!data #binary\n" +
                        "four: !byte[] \"\\0\\x01\\x02\\x03\\x04\\x05\\x06\\a\\b\\t\\n\\v\\f\\r\\x0E\\x0F\\x10\\x11\\x12\\x13\\x14\\x15\\x16\\x17\\x18\\x19\\x1A\\e\\x1C\\x1D\\x1E\\x1F \"\n" :
                "" +
                        "--- !!data #binary\n" +
                        "nothing: !byte[] \"\"\n" +
                        "# position: 23, header: 1\n" +
                        "--- !!data #binary\n" +
                        "one: !byte[] \"\\x01\"\n" +
                        "# position: 43, header: 2\n" +
                        "--- !!data #binary\n" +
                        "four: !byte[] \"\\0\\x01\\x02\\x03\\x04\\x05\\x06\\a\\b\\t\\n\\v\\f\\r\\x0E\\x0F\\x10\\x11\\x12\\x13\\x14\\x15\\x16\\x17\\x18\\x19\\x1A\\e\\x1C\\x1D\\x1E\\x1F \"\n";
        assertEquals(expected, Wires.fromSizePrefixedBlobs(wire));
        wire.readDocument(null, w -> assertArrayEquals(new byte[0], (byte[]) w.read(() -> "nothing").object()));
        wire.readDocument(null, w -> assertArrayEquals(one, (byte[]) w.read(() -> "one").object()));
        wire.readDocument(null, w -> assertArrayEquals(thirtytwo, (byte[]) w.read(() -> "four").object()));
    }

    // Test the behavior of using complex objects (like MyMarshallable) as keys in a map
    // and writing and reading this map from a wire
    @Test
    public void testObjectKeys() {
        @NotNull Map<MyMarshallable, String> map = new LinkedHashMap<>();
        map.put(new MyMarshallable("key1"), "value1");
        map.put(new MyMarshallable("key2"), "value2");

        @NotNull Wire wire = createWire();
        @NotNull final MyMarshallable parent = new MyMarshallable("parent");
        wire.writeDocument(false, w -> w.writeEvent(MyMarshallable.class, parent).object(map));

        // Check that the wire's content matches the expected format
        assertEquals("--- !!data #binary\n" +
                        "? { MyField: parent }: {\n" +
                        "  ? !net.openhft.chronicle.wire.MyMarshallable { MyField: key1 }: value1,\n" +
                        "  ? !net.openhft.chronicle.wire.MyMarshallable { MyField: key2 }: value2\n" +
                        "}\n"
                , Wires.fromSizePrefixedBlobs(wire.bytes()));

        // Read the document from the wire and check the values
        wire.readDocument(null, w -> {
            MyMarshallable mm = w.readEvent(MyMarshallable.class);
            assertEquals(parent.toString(), mm.toString());
            parent.equals(mm);
            assertEquals(parent, mm);
            @Nullable final Map map2 = w.getValueIn()
                    .object(Map.class);
            assertEquals(map, map2);
        });
    }

    // Test the writing and reading of literal byte sequences in a wire
    @Test
    public void testBytesLiteral() {
        assumeFalse(usePadding);  // Skip this test if padding is used

        @NotNull Wire wire = new BinaryWire(Bytes.elasticByteBuffer());
        wire.write("test").text("Hello World");

        @NotNull final BinaryWire wire1 = createWire();
        wire1.writeDocument(false, (WireOut w) -> w.write(() -> "nested").bytesLiteral(wire.bytes()));

        // Check that the nested wire's content matches the expected format
        assertEquals("--- !!data #binary\n" +
                "nested: {\n" +
                "  test: Hello World\n" +
                "}\n", Wires.fromSizePrefixedBlobs(wire1));

        // Read the nested wire's content and check its value
        wire1.readDocument(null, w -> {
            @Nullable final BytesStore<?, ?> bytesStore = w.read(() -> "nested")
                    .bytesLiteral();
            assertEquals(wire.bytes(), bytesStore);
        });

        wire.bytes().releaseLast();  // Release the resources
    }

    @Ignore("TODO FIX")
    @Test
    public void testUnicodeReadAndWriteHex() {
        bytes.releaseLast();
        bytes = new HexDumpBytes();
        doTestUnicodeReadAndWrite();
    }

    // Test reading and writing Unicode characters directly (not on heap)
    @Test
    public void testUnicodeReadAndWriteDirect() {
        assumeFalse(usePadding);  // Skip this test if padding is used

        bytes.releaseLast();
        bytes = allocateElasticDirect();  // Directly allocate memory for the bytes
        doTestUnicodeReadAndWrite();  // Use the helper method to conduct the test
    }

    // Test reading and writing Unicode characters on heap
    // Note: This test has been marked to be ignored due to some issues
    @Test
    @Ignore("TODO FIX")
    public void testUnicodeReadAndWriteOnHeap() {
        bytes.releaseLast();
        bytes = allocateElasticOnHeap();  // Allocate memory for the bytes on the heap
        doTestUnicodeReadAndWrite();  // Use the helper method to conduct the test
    }

    // Helper method for reading and writing Unicode
    public void doTestUnicodeReadAndWrite() {
        @NotNull Wire wire = createWire();
        try {
            wire.writeDocument(false, w -> w.write("data")
                    .typePrefix("!UpdateEvent")
                    .marshallable(
                            v -> {
                                v.write("mm").text("你好")  // Write Chinese characters
                                        .write("value").float64(15.0);
                            }));
            // assertEquals("29 00 00 00 c4 64 61 74 61 b6 0c 21 55 70 64 61\n" +
            // "74 65 45 76 65 6e 74 82 11 00 00 00 c2 6d 6d e6\n" +
            // "e4 bd a0 e5 a5 bd c5 76 61 6c 75 65 0f\n", bytes.toHexString());
            // Ensure that the wire's content matches the expected format with the Chinese characters

            assertEquals("" +
                            "--- !!data #binary\n" +
                            "data: !!UpdateEvent {\n" +
                            "  mm: \"\\u4F60\\u597D\",\n" +
                            "  value: 15\n" +
                            "}\n",
                    Wires.fromSizePrefixedBlobs(wire.bytes()));
        } finally {
            wire.bytes().releaseLast();  // Release the resources
        }
    }

    // Test writing a map with diverse types to a wire and then reading it back
    @Test
    public void testWriteMap() {
        @NotNull Wire wire = new BinaryWire(Bytes.elasticByteBuffer());

        // Create a map with different types of values
        @NotNull Map<String, Object> putMap = new HashMap<String, Object>();
        putMap.put("TestKey", "TestValue");
        putMap.put("TestKey2", 1.0);

        wire.writeAllAsMap(String.class, Object.class, putMap);  // Write the map to the wire

        @NotNull Map<String, Object> newMap = new HashMap<String, Object>();

        wire.readAllAsMap(String.class, Object.class, newMap); // Read the map from the wire

        Assert.assertEquals(putMap, newMap); // Ensure that the read map matches the original one

        wire.bytes().releaseLast(); // Release the resources
    }

// This test is designed to check if the wire correctly reads a Bytes object from a marshallable representation.
    @Test
    public void testreadBytes() {
        // Create a new BinaryWire with heap allocated storage
        @NotNull Wire wire = new BinaryWire(allocateElasticOnHeap());

        // Write a marshallable BytesHolder object with a "Hello World" text
        wire.write("a").typePrefix(BytesHolder.class).marshallable(w -> w.write("bytes").text("Hello World"));

        // Read the BytesHolder object from the wire
        BytesHolder bh2 = new BytesHolder();
        wire.read("a").object(bh2, BytesHolder.class);

        // Check if the read BytesHolder contains the expected content
        assertEquals("Hello World", bh2.bytes.toString());
    }

    // This test checks the efficiency of writing decimal numbers to the wire.
    // It tries to ensure that decimal numbers are written and read back correctly,
    // and that they use a minimal amount of space.
    @Test
    public void testWritingDecimals() {
        // Create a new BinaryWire with heap allocated storage
        @NotNull Wire wire = new BinaryWire(allocateElasticOnHeap());
        @NotNull final ValueOut out = wire.getValueOut();
        @NotNull final ValueIn in = wire.getValueIn();
        // try all the values of 0.xxxxxx which will fit
        @NotNull Random rand = new Random();
        final int runs = 100000;

        // Define test scenarios for writing different types of decimal numbers

        // Testing 6 decimal places numbers
        for (int t = 0; t < runs; t++) {
            long i = (rand.nextLong() >> -42) | 1; // make it odd.
            if (i < 0) i >>= 7;
            wire.clear();
            double d = i / 1e6;
            out.float64(d);
            final double v = in.float64();

            // Check if the number is correctly read back
            assertEquals(d, v, 0.0);

            // Check if the size used by the wire is less than 8 bytes
            final long size = wire.bytes().readPosition();
            assertTrue("i: " + i + ", size: " + size, size < 8);
        }

        // Testing 4 decimal places numbers
        for (int t = 0; t < runs; t++) {
            long i = (rand.nextLong() >> -42) / 100 | 1; // make it odd.
            if (i < 0) i >>= 7;
            wire.clear();
            double d = i / 1e4;
            if (i == -13721782305L)
                Thread.yield();
            out.float64(d);
            final double v = in.float64();

            // Check if the number is correctly read back
            assertEquals(d, v, 0.0);

            // Check if the size used by the wire is less than 8 bytes
            final long size = wire.bytes().readPosition();
            assertTrue("i: " + i + ", size: " + size, size < 8);
        }
        // try all the values of 0.xx which will fit
        for (int t = 0; t < runs; t++) {
            long i = (rand.nextLong() >> -42) / 10000 | 1; // make it odd.
            if (i < 0) i >>= 7;
            wire.clear();
            double d = i / 1e2;
            out.float64(d);
            final double v = in.float64();

            // Check if the number is correctly read back
            assertEquals(d, v, 0.0);

            // Check if the size used by the wire is less than 8 bytes
            final long size = wire.bytes().readPosition();
            assertTrue("i: " + i + ", size: " + size, size < 8);
        }
    }

    // This test checks the correctness of writing a series of decimal numbers to the wire.
    // It is aiming to ensure that for numbers in a certain range (here, 0 to 2 with 2 decimal places),
    // the numbers are written and read back correctly.
    @Test
    public void testWritingDecimals2() {
        // Create a new BinaryWire with heap allocated storage
        @NotNull Wire wire = new BinaryWire(allocateElasticOnHeap());
        @NotNull final ValueOut out = wire.getValueOut();
        @NotNull final ValueIn in = wire.getValueIn();

        // Loop to write and read back decimal numbers from 0 to 1.99 in increments of 0.01
        for (int t = 0; t < 200; t++) {
            wire.clear();
            double d = t / 1e2;  // converting to decimal
            out.float64(d);  // writing decimal to wire
            final double v = in.float64();  // reading back the decimal

            // Asserting that the read value is the same as the written value
            assertEquals(d, v, 0.0);
            final long size = wire.bytes().readPosition();
            // System.out.println(d + " size: " + size);
        }
    }

    // This test checks the capability of the Wire to read a CharSequence correctly.
    @Test
    public void readCharSequence() {
        // Create a wire and write "hello world" as an object
        Wire wire = createWire();
        wire.write().object("hello world");

        // Read back the CharSequence from the wire
        CharSequence s = wire.read()
                .object(CharSequence.class);
        // Asserting that the read value is the same as the written value
        assertEquals("hello world", s);
    }

    // Class representing a holder for Bytes. It is a marshallable object with a provision
    // to read its content from a wire.
    static class BytesHolder extends SelfDescribingMarshallable {
        // Allocating a Bytes object with a heap storage and initial capacity of 64 bytes
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);

        // Overridden method to read the "bytes" field from a wire
        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            wire.read("bytes").bytes(bytes);
        }
    }
}
