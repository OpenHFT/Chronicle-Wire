/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicReference;

import static net.openhft.chronicle.bytes.Bytes.allocateElasticDirect;
import static net.openhft.chronicle.bytes.Bytes.allocateElasticOnHeap;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SuppressWarnings({"rawtypes", "try", "deprecation", "removal"})
class BinaryWire2Test extends WireTestCommon {
    private static final String COMPRESSION_SAMPLE = "xxxxxxxxxxxxxxxx2xxxxxxxxxxxxxxxxxxxxxxxxxxyyyyyyyyyyyyyyyyyyyyyy2yyyyyyyyyyyyyyyyy";
    private static final String EXPECTED_UNICODE_WIRE = "--- !!data #binary\n" +
            "data: !!UpdateEvent {\n" +
            "  mm: \"\\u4F60\\u597D\",\n" +
            "  value: 15\n" +
            "}\n";

    private boolean usePadding;
    @NotNull
    private
    Bytes<?> bytes = new HexDumpBytes();

    // Constructor to set the padding parameter
    void initBinaryWire2Test(boolean usePadding) {
        this.usePadding = usePadding;
    }

    // Collection of padding parameters for the tests
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
    @DisplayName("Binary wire should reject unmarshallable object")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire unmarshallable Object uses padding {0}")
    void unmarshallableObject(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        assertThrows(IllegalArgumentException.class, () -> {
            BinaryWire wire = createWire();
            wire.getValueOut().object(new Object());
        }, "non-marshallable Object should be rejected by binary wire");
    }

    // Test various reading length scenarios for different BinaryWireCode values
    @DisplayName("Binary wire should read correct length")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Read Length uses padding {0}")
    void testReadLength(boolean usePadding) {
        initBinaryWire2Test(usePadding);
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
                Jvm.maxDirectMemory() > 0 ? v -> v.marshallable(w -> {
                }) : v -> v.text("na"),
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
            assertEquals(wire.bytes().readRemaining() - 1, readLength, "readLength should return correct byte count for binary wire value type: " + wire2);
        }
        if (!wireCodes.isEmpty()) {
            System.err.println("Untested codes");
            wireCodes.forEach((k, v) -> System.err.println(v + "= " + Integer.toHexString(k)));
        }
    }

    // Test the reading and writing of boolean values including null
    @DisplayName("Binary wire should round trip boolean values")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Bool uses padding {0}")
    void testBool(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        @NotNull Wire wire = createWire();
        wire.write().bool(false)
                .write().bool(true)
                .write().bool(null);

        AtomicReference<Boolean> actual0 = new AtomicReference<>();
        AtomicReference<Boolean> actual1 = new AtomicReference<>();
        AtomicReference<Boolean> actual2 = new AtomicReference<>();
        wire.read().bool(actual0, AtomicReference::set)
                .read().bool(actual1, AtomicReference::set)
                .read().bool(actual2, AtomicReference::set);

        assertEquals(Boolean.FALSE, actual0.get(), "boolean false should round-trip as first value");
        assertEquals(Boolean.TRUE, actual1.get(), "boolean true should round-trip as second value");
        assertNull(actual2.get(), "null boolean should round-trip as third value");
    }

    // Test writing and reading a BytesStore
    @DisplayName("Binary wire should round trip BytesStore values")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Bytes Store uses padding {0}")
    void testBytesStore(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        @NotNull Wire wire = createWire();
        wire.write().object(Bytes.from("Hello"));

        Bytes<?> b = allocateElasticOnHeap();
        wire.read().bytes(b);
        assertEquals("Hello", b.toString(), "bytesstore should deserialize to original text value");
        b.releaseLast();
    }

    // Test the serialization and deserialization of an object containing a TreeMap
    @DisplayName("Binary wire should write object with TreeMap")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire write Object With Tree Map uses padding {0}")
    void writeObjectWithTreeMap(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        WireMapTestSupport.assertObjectWithTreeMap(WireType.BINARY::apply);
    }

    // Test reading and writing of 32-bit float values
    @DisplayName("Binary wire should round trip float32 values")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Float32 uses padding {0}")
    void testFloat32(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        @NotNull Wire wire = createWire();
        wire.write().float32(0.0F)
                .write().float32(Float.NaN)
                .write().float32(Float.POSITIVE_INFINITY);

        assertEquals(0.0F, wire.read().float32(), 0.0F, "float32 first value should round-trip as 0.0");
        assertTrue(Float.isNaN(wire.read().float32()), "float32 second value should be NaN");
        assertEquals(Float.POSITIVE_INFINITY, wire.read().float32(), 0.0F, "float32 third value should round-trip as +Infinity");
    }

    // Test writing and reading of a NaN double value
    @DisplayName("Binary wire should preserve NaN values")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire NaN uses padding {0}")
    void testNaN(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        @NotNull Wire wire = createWire();
        wire.getValueOut()
                .float64(Double.NaN);
        assertEquals(5, wire.bytes().readRemaining(), "NaN should be encoded in 5 bytes in binary wire format");
        assertTrue(Double.isNaN(wire.getValueIn().float64()), "NaN value should round-trip correctly through binary wire");
    }

    // Test reading and writing of LocalTime values
    @DisplayName("Binary wire should round trip time values")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Time uses padding {0}")
    void testTime(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        @NotNull Wire wire = createWire();
        LocalTime now = LocalTime.now();
        wire.write().time(now)
                .write().time(LocalTime.MAX)
                .write().time(LocalTime.MIN);

        assertEquals(now, wire.read().time(), "time first value should round-trip as current LocalTime");
        assertEquals(LocalTime.MAX, wire.read().time(), "time second value should round-trip as LocalTime.MAX");
        assertEquals(LocalTime.MIN, wire.read().time(), "time third value should round-trip as LocalTime.MIN");
    }

    // Test reading and writing of ZonedDateTime values
    @DisplayName("Binary wire should round trip zoned date time")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Zoned Date Time uses padding {0}")
    void testZonedDateTime(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        @NotNull Wire wire = createWire();
        WireTemporalTestSupport.assertZonedDateTimes(wire);
    }

    // Test reading and writing of LocalDate values
    @DisplayName("Binary wire should round trip local date")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Local Date uses padding {0}")
    void testLocalDate(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        @NotNull Wire wire = createWire();
        WireTemporalTestSupport.assertLocalDates(wire);
    }

    // Test reading and writing of java.util.Date values
    @DisplayName("Binary wire should round trip legacy date")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Date uses padding {0}")
    void testDate(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        @NotNull Wire wire = createWire();

        try (final DocumentContext dc = wire.writingDocument(true)) {
            dc.wire().write().object(new Date(1234567890000L));
        }
        try (final DocumentContext dc = wire.readingDocument()) {
            Assertions.assertEquals(1234567890000L, dc.wire().read().object(Date.class).getTime(),
                    "java.util.Date should round-trip with millisecond precision in binary wire");
        }
    }

    // Test reading java.util.Date from a given string representation
    @DisplayName("Binary wire should reuse existing date instance")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Date Existing uses padding {0}")
    void testDateExisting(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        final String dateString = "1999-12-31";
        final java.util.Date expected = java.sql.Date.valueOf(dateString);
        @NotNull Wire wire = createWire();

        try (final DocumentContext dc = wire.writingDocument(true)) {
            dc.wire().write().text(dateString);
        }
        try (final DocumentContext dc = wire.readingDocument()) {
            Assertions.assertEquals(expected.getTime(), dc.wire().read().object(Date.class).getTime(), "date should parse from ISO date string in binary wire format");
        }
    }

    // Test reading and writing UUID values
    @DisplayName("Binary wire should round trip UUID values")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Uuid uses padding {0}")
    void testUuid(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        @NotNull Wire wire = createWire();
        UUID uuid = UUID.randomUUID();
        wire.write().uuid(uuid)
                .write().uuid(new UUID(0, 0))
                .write().uuid(new UUID(Long.MAX_VALUE, Long.MAX_VALUE));

        assertEquals(uuid, wire.read().uuid(), "uuid first value should round-trip");
        assertEquals(new UUID(0, 0), wire.read().uuid(), "uuid second value should round-trip as all-zero");
        assertEquals(new UUID(Long.MAX_VALUE, Long.MAX_VALUE), wire.read().uuid(), "uuid third value should round-trip as max components");
    }

    // Test writing sequences in both binary and text format with Chronicle Wire
    @DisplayName("Binary wire should serialise sequence values")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Sequence uses padding {0}")
    void testSequence(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        assumeFalse(Jvm.maxDirectMemory() == 0, "direct memory is required for sequence test");

        @NotNull Wire wire = createWire();
        writeMessage(wire);
        assertEquals("--- !!meta-data #binary\n" +
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
                Wires.fromSizePrefixedBlobs(wire), "binary wire should serialize metadata and sequence of marshallables with correct field names");

        @NotNull Wire twire = WireType.TEXT.apply(allocateElasticOnHeap());
        writeMessage(twire);
        assertEquals("--- !!meta-data\n" +
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
                Wires.fromSizePrefixedBlobs(twire), "text wire should serialize same structure as binary with field names in YAML format");

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
    @DisplayName("Binary wire should write sequence context data")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Sequence Context uses padding {0}")
    void testSequenceContext(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        assumeFalse(Jvm.maxDirectMemory() == 0, "direct memory is required for sequence context test");

        assumeTrue(usePadding, "sequence context test requires padding enabled");
        @NotNull Wire wire = createWire();
        writeMessageContext(wire);

        // Expected binary hex representation of the written data
        assertEquals("1c 00 00 40                                     # msg-length\n" +
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
                wire.bytes().toHexString(), "binary wire with padding should produce expected compact hex format with field name prefixes");

        @NotNull Wire twire = WireType.TEXT.apply(allocateElasticOnHeap());
        writeMessageContext(twire);

        // Expected textual representation of the written data
        assertEquals("--- !!meta-data\n" +
                        "csp: //path/service\n" +
                        "tid: 123456789\n" +
                        "# position: 39, header: 0\n" +
                        "#  has a 4 byte size prefix, 25856 > 102 len is 25856",
                Wires.fromSizePrefixedBlobs(twire.bytes()), "text wire metadata should serialize with YAML format and position markers");

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
    @DisplayName("Binary wire should round trip enum values")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Enum uses padding {0}")
    void testEnum(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        @NotNull Wire wire = createWire();
        wire.write().object(WireType.BINARY)
                .write().object(WireType.TEXT)
                .write().object(WireType.RAW);

        // Validate that the enums have been correctly written and can be read back as expected
        assertEquals(WireType.BINARY, wire.read()
                .object(Object.class), "wiretype enum should deserialize as BINARY with correct type identity");
        assertEquals(WireType.TEXT, wire.read().object(Object.class), "wiretype enum should deserialize as TEXT with correct type identity");
        assertEquals(WireType.RAW, wire.read().object(Object.class), "wiretype enum should deserialize as RAW with correct type identity");
    }

    private void writeUpdateEvent(WireOut wireOut, Object oldValue) {
        wireOut.write("data").typedMarshallable("!UpdateEvent",
                v -> v.write("assetName").text("/name")
                        .write("key").object("test")
                        .write("oldValue").object(oldValue)
                        .write("value").object("world2"));
    }

    private void assertUpdateEvent(ValueIn valueIn, boolean expectNullOldValue, String context) {
        String prefix = context + ": ";
        valueIn.typePrefix(this, (o, t) -> assertEquals("!UpdateEvent", t.toString(),
                prefix + "type prefix should identify UpdateEvent type in binary wire")).marshallable(
                m -> {
                    m.read(() -> "assetName").object(String.class, "/name", (expected, actual) -> Assertions.assertEquals(expected, actual,
                                    prefix + "assetName field should deserialize correctly in typed marshallable"))
                            .read(() -> "key").object(String.class, "test", (expected, actual) -> Assertions.assertEquals(expected, actual,
                                    prefix + "key field should deserialize correctly in typed marshallable"));
                    if (expectNullOldValue) {
                        m.read(() -> "oldValue").object(String.class, "oldValue", (message, actual) -> Assertions.assertNull(actual,
                                prefix + "oldValue field should be null when written as null in binary wire"));
                    } else {
                        m.read(() -> "oldValue").object(String.class, "world1", (expected, actual) -> Assertions.assertEquals(expected, actual,
                                prefix + "oldValue field should deserialize correctly in typed marshallable"));
                    }
                    m.read(() -> "value").object(String.class, "world2", (expected, actual) -> Assertions.assertEquals(expected, actual,
                            prefix + "value field should deserialize correctly in typed marshallable"));
                });
    }

    // Test the serialization behavior when there's text data followed by field data
    @DisplayName("Binary wire should read field after text")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire field After Text uses padding {0}")
    void fieldAfterText(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        assumeFalse(usePadding, "field-after-text test requires padding disabled");  // Ensure padding is not used for this test

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
                "}\n", Wires.fromSizePrefixedBlobs(wire.bytes()), "typed marshallable with text fields should serialize with type prefix and all field names");

        // Read back the document and assert each field's value
        wire.readDocument(null, w -> assertUpdateEvent(w.read(() -> "data"), false,
                "typed marshallable after text field"));
    }

    // Test the serialization behavior when there's a null field followed by another field
    @DisplayName("Binary wire should read field after null event value")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire field After Null uses padding {0}")
    void fieldAfterNull(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        @NotNull Wire wire = createWire();
        wire.writeDocument(false, w -> writeUpdateEvent(w, null));

        // Validate the serialized format of the document, especially the null field representation
        assertEquals("--- !!data #binary\n" +
                "data: !!UpdateEvent {\n" +
                "  assetName: /name,\n" +
                "  key: test,\n" +
                "  oldValue: !!null \"\",\n" +
                "  value: world2\n" +
                "}\n", Wires.fromSizePrefixedBlobs(wire.bytes()), "typed marshallable with null field should serialize with explicit null marker in binary wire");

        // Read back the document, especially ensuring the null field is read back correctly
        wire.readDocument(null, w -> assertUpdateEvent(w.read(() -> "data"), true,
                "typed marshallable after null field"));
    }

    // Test the serialization behavior when there's a null field in the context of other metadata and data fields
    @DisplayName("Binary wire should read field after null in document context")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire field After Null Context uses padding {0}")
    void fieldAfterNullContext(boolean usePadding) {
        initBinaryWire2Test(usePadding);
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
            writeUpdateEvent(wire, null);
        }

        // Validate the serialized format of the entire wire content, including metadata and data
        assertEquals("--- !!meta-data #binary\n" +
                        "tid: 1234567890\n" +
                        "# position: 1X, header: 0\n" +
                        "--- !!data #binary\n" +
                        "data: !!UpdateEvent {\n" +
                        "  assetName: /name,\n" +
                        "  key: test,\n" +
                        "  oldValue: !!null \"\",\n" +
                        "  value: world2\n" +
                        "}\n",
                Wires.fromSizePrefixedBlobs(wire).replaceAll("position: 1\\d", "position: 1X"), "multi-document binary wire should serialize metadata and data documents with null field handling");

        // Read back the metadata and assert its content
        try (DocumentContext context = wire.readingDocument()) {
            assertTrue(context.isPresent(), "first document context should be present in binary wire");
            assertTrue(context.isMetaData(), "first document should be identified as metadata in binary wire");
            Assertions.assertEquals(1234567890L, wire.read(() -> "tid").int64(), "tid field should deserialize correctly from metadata document");
        }

        // Read back the main data and assert each field's value
        try (DocumentContext context = wire.readingDocument()) {
            assertTrue(context.isPresent(), "second document context should be present in binary wire");
            assertTrue(context.isData(), "second document should be identified as data in binary wire");
            assertUpdateEvent(wire.read(() -> "data"), true,
                    "typed marshallable after metadata and null field");
        }

        // Ensure no more data is available in the wire
        try (DocumentContext context = wire.readingDocument()) {
            assertFalse(context.isPresent(), "no more documents should be available after reading all content");
        }
    }

    // Test the behavior of reading and writing a demarshallable object
    @DisplayName("Binary wire should read demarshallable values")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire read Demarshallable uses padding {0}")
    void readDemarshallable(boolean usePadding) {
        initBinaryWire2Test(usePadding);
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
                "}\n", Wires.fromSizePrefixedBlobs(wire), "typed marshallable should serialize with fully qualified class name and field names in binary wire");

        // Read back the DemarshallableObject instance and validate its content
        try (DocumentContext $ = wire.readingDocument()) {
            @Nullable DemarshallableObject dobj = wire.getValueIn().typedMarshallable();
            assertEquals("test", dobj.name, "demarshallable object name field should deserialize correctly from binary wire");
            assertEquals(123456, dobj.value, "demarshallable object value field should deserialize correctly from binary wire");
        }
    }

    // Test the behavior of Gzip compression within the Wire system
    @DisplayName("Binary wire should compress with gzip")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Compress With Gzip uses padding {0}")
    void testCompressWithGzip(boolean usePadding) {
        initBinaryWire2Test(usePadding);
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
        assertEquals(s, str2, "gzip compressed string should decompress to original value in binary wire");

        // Convert the compressed content to plain text format and validate
        wire.bytes().readPosition(0);
        Bytes<?> asText = allocateElasticOnHeap();
        wire.copyTo(WireType.TEXT.apply(asText));
        assertEquals("message: # gzip\n" + s +
                "\n", asText.toString(), "gzip compressed binary should convert to text wire with compression marker");
        asText.releaseLast();
        str.releaseLast();
    }

    // Test the behavior when data is compressed using the "binary" scheme (likely no compression)
    @DisplayName("Binary wire should use binary compression")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Binary Compression uses padding {0}")
    void testBinaryCompression(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        assertEquals(COMPRESSION_SAMPLE, testCompression("binary"), "binary compression should round-trip sample text");
    }

    // Test the behavior when data is compressed using the Gzip scheme
    @DisplayName("Binary wire should use gzip compression")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Gzip Compression uses padding {0}")
    void testGzipCompression(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        assertEquals(COMPRESSION_SAMPLE, testCompression("gzip"), "gzip compression should round-trip sample text");
    }

    // Test the behavior when data is compressed using the LZW scheme
    @DisplayName("Binary wire should use LZW compression")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Lzw Compression uses padding {0}")
    void testLzwCompression(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        assertEquals(COMPRESSION_SAMPLE, testCompression("lzw"), "lzw compression should round-trip sample text");
    }

    /**
     * Tests the behavior of different compression schemes within the Wire system.
     *
     * @param comp Compression scheme ("binary", "gzip", or "lzw")
     */
    private String testCompression(String comp) {
        bytes.clear();
        @NotNull Wire wire = new BinaryWire(bytes, false, false, false, 32, comp);

        // Create a repetitive string and convert it to BytesStore
        BytesStore<?, ?> bytesStore = Bytes.from(COMPRESSION_SAMPLE);

        // Write the string to the wire using the specified compression
        wire.write().bytes(bytesStore);

        // If compression is used (i.e., not binary), verify that the compressed size is smaller
        if (!comp.equals("binary"))
            assertTrue(wire.bytes().readRemaining() < COMPRESSION_SAMPLE.length(),
                    "compressed data size (" + wire.bytes().readRemaining() + ") should be smaller than original (" + COMPRESSION_SAMPLE.length() + ") for " + comp + " compression");

        // Read back the compressed string and validate its content
        wire.bytes().readPosition(0);
        String str2 = wire.read().text();
        bytesStore.releaseLast();
        return str2;
    }

    // Test the behavior of storing and retrieving a byte array containing negative values
    @DisplayName("Binary wire Byte Array Value With Real Bytes Negative")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Byte Array Value With Real Bytes Negative uses padding {0}")
    void testByteArrayValueWithRealBytesNegative(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        @NotNull Wire wire = createWire();

        // Create an array of negative bytes
        @NotNull final byte[] expected = {-1, -2, -3, -4, -5, -6, -7};

        // Write the byte array to the wire under a key named "1"
        wire.writeDocument(false, wir -> wir.writeEventName(() -> "put")
                .marshallable(w -> w.write("key").text("1")
                        .write("value")
                        .object(expected)));

        // Read back the stored byte array and validate its content
        AtomicReference<String> actualKey = new AtomicReference<>();
        AtomicReference<Object> actualValue = new AtomicReference<>();
        assertTrue(wire.readDocument(null, wir -> wir.read(() -> "put")
                .marshallable(w -> {
                    actualKey.set(w.read(() -> "key").text());
                    actualValue.set(w.read(() -> "value").object(Object.class));
                })), "document with byte array value should be readable from binary wire");
        assertEquals("1", actualKey.get(), "byte array key field should deserialize correctly");
        assertArrayEquals(expected, (byte[]) actualValue.get(), "negative byte array values should round-trip correctly through binary wire");
    }

    // Test the behavior of writing and reading from a wire using byte arrays of varying sizes
    @DisplayName("Binary wire should round trip bytes array")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Bytes Array uses padding {0}")
    void testBytesArray(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        @NotNull Wire wire = createWire();
        @NotNull SplittableRandom rand = new SplittableRandom(0x9e3779b9);
        for (int i = 0; i < 70000; i += rand.nextInt(i + 1) + 1) {
            wire.clear();
            @NotNull final byte[] fromBytes = new byte[i];
            wire.writeDocument(false, w -> w.write("bytes").bytes(fromBytes));
            Wires.fromSizePrefixedBlobs(wire);
            int finalI = i;
            int[] bytesLength = {-1};
            assertTrue(wire.readDocument(null, w -> bytesLength[0] = w.read("bytes").bytes().length),
                    "document with byte array should be readable from binary wire (i=" + i + ", length=" + finalI + ")");
            assertEquals(finalI, bytesLength[0], "byte array length should round-trip for i=" + i + ", length=" + finalI);
        }
    }

    // Test the writing and reading of a small array on the wire
    @DisplayName("Binary wire should handle small arrays")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Small Array uses padding {0}")
    void testSmallArray(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        @NotNull Wire wire = createWire();
        wire.writeDocument(false, w -> w.write("index")
                .int64array(10));
        assertEquals("--- !!data #binary\n" +
                "index: [\n" +
                "  # length: 10, used: 0\n" +
                "  0, 0, 0, 0, 0, 0, 0, 0, 0, 0\n" +
                "]\n", Wires.fromSizePrefixedBlobs(wire.bytes()), "int64 array should serialize with length and used metadata in binary wire format");
    }

    // Test the writing and reading of different type literals on the wire
    @DisplayName("Binary wire should round trip type literals")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Type Literal uses padding {0}")
    void testTypeLiteral(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        assumeFalse(usePadding, "type literal test requires padding disabled");

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
                "z: !!null \"\"\n", Wires.fromSizePrefixedBlobs(wire.bytes()), "type literals should serialize with !type prefix for classes and arrays, null for null class");
    }

    // Test the behavior of writing and reading byte arrays of specific sizes and values
    @DisplayName("Binary wire should read byte array values")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Byte Array uses padding {0}")
    void testByteArray(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        assumeFalse(usePadding, "byte array test requires padding disabled");
        @NotNull Wire wire = createWire();
        wire.writeDocument(false, w -> w.write("nothing").object(new byte[0]));
        @NotNull byte[] one = {1};
        wire.writeDocument(false, w -> w.write("one").object(one));
        @NotNull byte[] thirtytwo = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32};
        wire.writeDocument(false, w -> w.write("four").object(thirtytwo));

        final String expected = usePadding ?
                "--- !!data #binary\n" +
                        "nothing: !byte[] \"\"\n" +
                        "# position: 24, header: 1\n" +
                        "--- !!data #binary\n" +
                        "one: !byte[] \"\\x01\"\n" +
                        "# position: 44, header: 2\n" +
                        "--- !!data #binary\n" +
                        "four: !byte[] \"\\0\\x01\\x02\\x03\\x04\\x05\\x06\\a\\b\\t\\n\\v\\f\\r\\x0E\\x0F\\x10\\x11\\x12\\x13\\x14\\x15\\x16\\x17\\x18\\x19\\x1A\\e\\x1C\\x1D\\x1E\\x1F \"\n" :
                "--- !!data #binary\n" +
                        "nothing: !byte[] \"\"\n" +
                        "# position: 23, header: 1\n" +
                        "--- !!data #binary\n" +
                        "one: !byte[] \"\\x01\"\n" +
                        "# position: 43, header: 2\n" +
                        "--- !!data #binary\n" +
                        "four: !byte[] \"\\0\\x01\\x02\\x03\\x04\\x05\\x06\\a\\b\\t\\n\\v\\f\\r\\x0E\\x0F\\x10\\x11\\x12\\x13\\x14\\x15\\x16\\x17\\x18\\x19\\x1A\\e\\x1C\\x1D\\x1E\\x1F \"\n";
        assertEquals(expected, Wires.fromSizePrefixedBlobs(wire), "multiple byte arrays of varying sizes should serialize with !byte[] type prefix and escaped content");
        wire.readDocument(null, w -> assertArrayEquals(new byte[0], (byte[]) w.read(() -> "nothing").object(), "empty byte array should round-trip correctly through binary wire"));
        wire.readDocument(null, w -> assertArrayEquals(one, (byte[]) w.read(() -> "one").object(), "single-element byte array should round-trip correctly through binary wire"));
        wire.readDocument(null, w -> assertArrayEquals(thirtytwo, (byte[]) w.read(() -> "four").object(), "33-element byte array should round-trip correctly through binary wire"));
    }

    // Test the behavior of using complex objects (like MyMarshallable) as keys in a map
    // and writing and reading this map from a wire
    @DisplayName("Binary wire should read object keys")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Object Keys uses padding {0}")
    void testObjectKeys(boolean usePadding) {
        initBinaryWire2Test(usePadding);
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
                        "}\n",
                Wires.fromSizePrefixedBlobs(wire.bytes()), "map with marshallable keys should serialize with explicit key markers and type prefixes in binary wire");

        // Read the document from the wire and check the values
        wire.readDocument(null, w -> {
            MyMarshallable mm = w.readEvent(MyMarshallable.class);
            assertEquals(parent.toString(), mm.toString(), "marshallable event key should have matching string representation");
            assertEquals(parent, mm, "marshallable event key should deserialize with correct equality");
            @Nullable final Map map2 = w.getValueIn()
                    .object(Map.class);
            assertEquals(map, map2, "map with marshallable keys should round-trip correctly through binary wire");
        });
    }

    // Test the writing and reading of literal byte sequences in a wire
    @DisplayName("Binary wire should read bytes literal")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Bytes Literal uses padding {0}")
    void testBytesLiteral(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        assumeFalse(usePadding, "bytes literal test requires padding disabled");  // Skip this test if padding is used

        @NotNull Wire wire = new BinaryWire(allocateElasticOnHeap());
        wire.write("test").text("Hello World");

        @NotNull final BinaryWire wire1 = createWire();
        wire1.writeDocument(false, (WireOut w) -> w.write(() -> "nested").bytesLiteral(wire.bytes()));

        // Check that the nested wire's content matches the expected format
        assertEquals("--- !!data #binary\n" +
                "nested: {\n" +
                "  test: Hello World\n" +
                "}\n", Wires.fromSizePrefixedBlobs(wire1), "bytes literal should embed nested binary wire content as inline marshallable structure");

        // Read the nested wire's content and check its value
        wire1.readDocument(null, w -> {
            @Nullable final BytesStore<?, ?> bytesStore = w.read(() -> "nested")
                    .bytesLiteral();
            assertEquals(wire.bytes(), bytesStore, "bytes literal should embed raw binary wire content preserving exact byte sequence");
        });

        wire.bytes().releaseLast();  // Release the resources
    }

    @DisplayName("Binary wire Unicode Read And Write Hex")
    @Disabled("TODO fix: unicode hex dump output differs from expected")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Unicode Read And Write Hex uses padding {0}")
    void testUnicodeReadAndWriteHex(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        bytes.releaseLast();
        bytes = new HexDumpBytes();
        assertEquals(EXPECTED_UNICODE_WIRE, doTestUnicodeReadAndWrite(),
                "unicode wire output should match expected YAML blob in hex dump mode");
    }

    // Test reading and writing Unicode characters directly (not on heap)
    @DisplayName("Binary wire Unicode Read And Write Direct")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Unicode Read And Write Direct uses padding {0}")
    void testUnicodeReadAndWriteDirect(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        assumeFalse(usePadding, "direct unicode test requires padding disabled");  // Skip this test if padding is used

        bytes.releaseLast();
        bytes = allocateElasticDirect();  // Directly allocate memory for the bytes
        assertEquals(EXPECTED_UNICODE_WIRE, doTestUnicodeReadAndWrite(),
                "unicode wire output should match expected YAML blob in direct bytes mode");  // Use the helper method to conduct the test
    }

    // Test reading and writing Unicode characters on heap
    // Note: This test has been marked to be ignored due to some issues
    @DisplayName("Binary wire Unicode Read And Write On Heap")
    @ParameterizedTest(name = "Binary wire Unicode Read And Write On Heap uses padding {0}")
    @Disabled("TODO fix: unicode heap output differs from expected")
    @MethodSource("wireTypes")
    void testUnicodeReadAndWriteOnHeap(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        bytes.releaseLast();
        bytes = allocateElasticOnHeap();  // Allocate memory for the bytes on the heap
        assertEquals(EXPECTED_UNICODE_WIRE, doTestUnicodeReadAndWrite(),
                "unicode wire output should match expected YAML blob in heap bytes mode");  // Use the helper method to conduct the test
    }

    // Helper method for reading and writing Unicode
    private String doTestUnicodeReadAndWrite() {
        @NotNull Wire wire = createWire();
        try {
            wire.writeDocument(false, w -> w.write("data")
                    .typePrefix("!UpdateEvent")
                    .marshallable(
                            v -> v.write("mm").text("你好")  // Write Chinese characters
                                    .write("value").float64(15.0)));
            // assertEquals("29 00 00 00 c4 64 61 74 61 b6 0c 21 55 70 64 61\n" +
            // "74 65 45 76 65 6e 74 82 11 00 00 00 c2 6d 6d e6\n" +
            // "e4 bd a0 e5 a5 bd c5 76 61 6c 75 65 0f\n", bytes.toHexString());
            // Ensure that the wire's content matches the expected format with the Chinese characters

            return Wires.fromSizePrefixedBlobs(wire.bytes());
        } finally {
            wire.bytes().releaseLast();  // Release the resources
        }
    }

    // Test writing a map with diverse types to a wire and then reading it back
    @DisplayName("Binary wire should write map values")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Write Map uses padding {0}")
    void testWriteMap(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        @NotNull Wire wire = new BinaryWire(allocateElasticOnHeap());

        // Create a map with different types of values
        @NotNull Map<String, Object> putMap = new HashMap<>();
        putMap.put("TestKey", "TestValue");
        putMap.put("TestKey2", 1.0);

        wire.writeAllAsMap(String.class, Object.class, putMap);  // Write the map to the wire

        @NotNull Map<String, Object> newMap = new HashMap<>();

        wire.readAllAsMap(String.class, Object.class, newMap); // Read the map from the wire

        Assertions.assertEquals(putMap, newMap, "map with mixed string and numeric values should round-trip correctly using readAllAsMap/writeAllAsMap"); // Ensure that the read map matches the original one

        wire.bytes().releaseLast(); // Release the resources
    }

    // This test is designed to check if the wire correctly reads a Bytes object from a marshallable representation.
    @DisplayName("Binary wire should read bytes values")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Read Bytes uses padding {0}")
    void testreadBytes(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        // Create a new BinaryWire with heap allocated storage
        @NotNull Wire wire = new BinaryWire(allocateElasticOnHeap());

        // Write a marshallable BytesHolder object with a "Hello World" text
        wire.write("a").typePrefix(BytesHolder.class).marshallable(w -> w.write("bytes").text("Hello World"));

        // Read the BytesHolder object from the wire
        BytesHolder bh2 = new BytesHolder();
        wire.read("a").object(bh2, BytesHolder.class);

        // Check if the read BytesHolder contains the expected content
        assertEquals("Hello World", bh2.bytes.toString(), "marshallable bytes field should deserialize text content correctly from binary wire");
    }

    // This test checks the efficiency of writing decimal numbers to the wire.
    // It tries to ensure that decimal numbers are written and read back correctly,
    // and that they use a minimal amount of space.
    @DisplayName("Binary wire should write decimal values")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Writing Decimals uses padding {0}")
    void testWritingDecimals(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        // Create a new BinaryWire with heap allocated storage
        @NotNull Wire wire = new BinaryWire(allocateElasticOnHeap());
        @NotNull final ValueOut out = wire.getValueOut();
        @NotNull final ValueIn in = wire.getValueIn();
        // try all the values of 0.xxxxxx which will fit
        @NotNull SplittableRandom rand = new SplittableRandom(0x243f6a88);
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
            assertEquals(d, v, 0.0, "decimal with 6 places should round-trip for t=" + t + ", value=" + d);

            // Check if the size used by the wire is less than 8 bytes
            final long size = wire.bytes().readPosition();
            assertTrue(size < 8, "decimal with 6 places should use compact encoding for t=" + t + ", i=" + i + ", size=" + size);
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
            assertEquals(d, v, 0.0, "decimal with 4 places should round-trip for t=" + t + ", value=" + d);

            // Check if the size used by the wire is less than 8 bytes
            final long size = wire.bytes().readPosition();
            assertTrue(size < 8, "decimal with 4 places should use compact encoding for t=" + t + ", i=" + i + ", size=" + size);
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
            assertEquals(d, v, 0.0, "decimal with 2 places should round-trip for t=" + t + ", value=" + d);

            // Check if the size used by the wire is less than 8 bytes
            final long size = wire.bytes().readPosition();
            assertTrue(size < 8, "decimal with 2 places should use compact encoding for t=" + t + ", i=" + i + ", size=" + size);
        }
    }

    // This test checks the correctness of writing a series of decimal numbers to the wire.
    // It is aiming to ensure that for numbers in a certain range (here, 0 to 2 with 2 decimal places),
    // the numbers are written and read back correctly.
    @DisplayName("Binary wire should write decimal values with scale")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire Writing Decimals 2 uses padding {0}")
    void testWritingDecimals2(boolean usePadding) {
        initBinaryWire2Test(usePadding);
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
            assertEquals(d, v, 0.0, "sequential decimal values should round-trip for t=" + t + ", value=" + d);
            final long size = wire.bytes().readPosition();
            assertTrue(size > 0, "wire bytes should advance for t=" + t + ", size=" + size);
        }
    }

    // This test checks the capability of the Wire to read a CharSequence correctly.
    @DisplayName("Binary wire should read CharSequence values")
    @MethodSource("wireTypes")
    @ParameterizedTest(name = "Binary wire read Char Sequence uses padding {0}")
    void readCharSequence(boolean usePadding) {
        initBinaryWire2Test(usePadding);
        // Create a wire and write "hello world" as an object
        Wire wire = createWire();
        wire.write().object("hello world");

        // Read back the CharSequence from the wire
        CharSequence s = wire.read()
                .object(CharSequence.class);
        // Asserting that the read value is the same as the written value
        assertEquals("hello world", s, "charsequence should deserialize from string object in binary wire");
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
