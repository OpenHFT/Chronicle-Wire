/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("deprecation")
public abstract class AbstractWireTest extends WireTestCommon {

    protected Bytes<?> bytes;

    protected abstract Wire createWire();

    // Test case for working with ZonedDateTime values
    @Test
    @DisplayName("Serialises and reads ZonedDateTime values correctly")
    void testZonedDateTime() {
        @NotNull Wire wire = createWire();

        // Create several ZonedDateTime instances: now, max, min
        ZonedDateTime now = ZonedDateTime.now();
        ZoneId zone = ZoneId.of("Europe/London");
        final ZonedDateTime max = ZonedDateTime.of(LocalDateTime.MAX, zone);
        final ZonedDateTime min = ZonedDateTime.of(LocalDateTime.MIN, zone);

        // Write the ZonedDateTime values to the wire
        wire.write()
                .zonedDateTime(now)
                .write().zonedDateTime(max)
                .write().zonedDateTime(min);

        // Validate the string representation of the wire content
        assertEquals("\"\": \"" + now + "\"\n" +
                "\"\": \"+999999999-12-31T23:59:59.999999999Z[Europe/London]\"\n" +
                        "\"\": \"-999999999-01-01T00:00-00:01:15[Europe/London]\"\n", wire.toString(),
                "wire should serialize ZonedDateTime values with timezone information");

        // Read back the ZonedDateTime values and validate
        wire.read().zonedDateTime(now, Assertions::assertEquals)
                .read().zonedDateTime(max, Assertions::assertEquals)
                .read().zonedDateTime(min, Assertions::assertEquals);

        // Repeat the process but write as a generic object
        wire.clear();
        wire.write().object(now)
                .write().object(max)
                .write().object(min);
        assertEquals("\"\": !ZonedDateTime \"" + now + "\"\n" +
                "\"\": !ZonedDateTime \"+999999999-12-31T23:59:59.999999999Z[Europe/London]\"\n" +
                        "\"\": !ZonedDateTime \"-999999999-01-01T00:00-00:01:15[Europe/London]\"\n", wire.toString(),
                "wire should serialize ZonedDateTime objects with type tag");
        wire.read().object(Object.class, now, Assertions::assertEquals)
                .read().object(Object.class, max, Assertions::assertEquals)
                .read().object(Object.class, min, Assertions::assertEquals);

        // Write as a ZonedDateTime object
        wire.clear();
        wire.write().object(ZonedDateTime.class, now)
                .write().object(ZonedDateTime.class, max)
                .write().object(ZonedDateTime.class, min);
        assertEquals("\"\": \"" + now + "\"\n" +
                "\"\": \"+999999999-12-31T23:59:59.999999999Z[Europe/London]\"\n" +
                        "\"\": \"-999999999-01-01T00:00-00:01:15[Europe/London]\"\n", wire.toString(),
                "wire should serialize ZonedDateTime class objects without type tag");
        wire.read().object(ZonedDateTime.class, now, Assertions::assertEquals)
                .read().object(ZonedDateTime.class, max, Assertions::assertEquals)
                .read().object(ZonedDateTime.class, min, Assertions::assertEquals);
    }

    // Test to ensure a SortedSet is correctly written to and read from the Wire.
    @Test
    @DisplayName("Serialises and reads SortedSet values correctly")
    void testSortedSet() {
        // Initialize a new Wire instance.
        final Wire wire = createWire();

        // Create a SortedSet (TreeSet) and populate it with strings.
        @NotNull SortedSet<String> set = new TreeSet<>();
        set.add("one");
        set.add("two");
        set.add("three");

        // Write the SortedSet to the Wire with the key "a".
        wire.write("a").object(set);

        // Validate the written content on the Wire.
        if (wire instanceof TextWire || wire instanceof YamlWire) {
            assertEquals("a: !!oset [\n" +
                    "  one,\n" +
                    "  three,\n" +
                    "  two\n" +
                            "]\n", wire.toString(),
                    "wire should serialize SortedSet with YAML ordered set tag");
        }

        // Read back the SortedSet from the Wire and validate its type and contents.
        @Nullable Object o = wire.read().object();
        assertInstanceOf(SortedSet.class, o, "wire should deserialize to SortedSet instance");
        assertEquals(set, o, "wire should preserve SortedSet contents and ordering");
    }

    // Test to ensure a SortedMap is correctly written to and read from the Wire.
    @Test
    @DisplayName("Serialises and reads SortedMap values correctly")
    void testSortedMap() {
        // Initialize a new Wire instance.
        final Wire wire = createWire();

        // Create a SortedMap (TreeMap) and populate it with key-value pairs.
        @NotNull SortedMap<String, Long> set = new TreeMap<>();
        set.put("one", 1L);
        set.put("two", 2L);
        set.put("three", 3L);

        // Write the SortedMap to the Wire with the key "a".
        wire.write("a").object(set);

        // Validate the written content on the Wire.
        if (wire instanceof TextWire || wire instanceof YamlWire) {
            assertEquals("a: !!omap {\n" +
                    "  one: 1,\n" +
                    "  three: 3,\n" +
                    "  two: 2\n" +
                            "}\n", wire.toString(),
                    "wire should serialize SortedMap with YAML ordered map tag");
        }

        // Read back the SortedMap from the Wire and validate its type and contents.
        @Nullable Object o = wire.read().object();
        assertInstanceOf(SortedMap.class, o, "wire should deserialize to SortedMap instance");
        assertEquals(set, o, "wire should preserve SortedMap contents and ordering");
    }

    // Test to validate the behavior of writing and reading simple boolean values
    @Test
    @DisplayName("Writes and reads simple boolean fields")
    void testSimpleBool() {
        @NotNull Wire wire = createWire();

        // Write two boolean values with keys "F" and "T"
        wire.write(() -> "F").bool(false);
        wire.write(() -> "T").bool(true);

        // Verify that the written values are correctly represented in string format
        assertEquals("F: false\n" +
                        "T: true\n", wire.toString(),
                "wire should serialize boolean values as 'true' and 'false'");

        // Check the wire content using the SnakeYaml parser
        @NotNull String expected = "{F=false, T=true}";
        expectWithSnakeYaml(expected, wire);

        // Read and validate the written boolean values
        assertFalse(wire.read(() -> "F").bool(), "wire should read false boolean value correctly");
        assertTrue(wire.read(() -> "T").bool(), "wire should read true boolean value correctly");
    }

    // Test to validate the behavior when writing strings that cannot be converted to boolean
    @Test
    @DisplayName("Reads non-boolean text values as false")
    void testFailingBool() {
        @NotNull Wire wire = createWire();

        // Write two non-boolean strings with keys "A" and "B"
        wire.write(() -> "A").text("");
        wire.write(() -> "B").text("other");

        // Verify the written strings
        assertEquals("A: \"\"\n" +
                        "B: other\n", wire.toString(),
                "wire should serialise non-boolean text values as strings");

        // Check the wire content using the SnakeYaml parser
        @NotNull String expected = "{A=, B=other}";
        expectWithSnakeYaml(expected, wire);

        // Ensure the written strings are not mistakenly interpreted as boolean values
        assertFalse(wire.read(() -> "A").bool(), "wire should read empty string as false when read as boolean");
        assertFalse(wire.read(() -> "B").bool(), "wire should read non-boolean string as false when read as boolean");
    }

    // Test to validate the reading of non-boolean strings as Boolean objects
    @Test
    @DisplayName("Reads non-boolean text as Boolean object result")
    void testFailingBoolean() {
        @NotNull Wire wire = createWire();

        // Write two non-boolean strings
        wire.write(() -> "A").text("");
        wire.write(() -> "B").text("other");

        // Verify the written strings
        assertEquals("A: \"\"\n" +
                        "B: other\n", wire.toString(),
                "wire should serialise non-boolean text values as strings for Boolean reads");

        // Check the wire content using the SnakeYaml parser
        @NotNull String expected = "{A=, B=other}";
        expectWithSnakeYaml(expected, wire);

        // TODO: Handle the potential issue when reading a string as a Boolean object
        // assertEquals(null, wire.read(() -> "A").object(Boolean.class));
        assertEquals(false, wire.read(() -> "B").object(Boolean.class),
                "wire should read non-boolean string as false when read as Boolean object");
    }

    // Test to validate the behavior when writing text with a leading space
    @Test
    @DisplayName("Preserves leading whitespace in text values")
    void testLeadingSpace() {
        @NotNull Wire wire = createWire();

        // Write a string with a leading space
        wire.write().text(" leadingspace");

        // Ensure that the leading space is retained when reading back the string
        assertEquals(" leadingspace", wire.read().text(),
                "wire should preserve leading whitespace in text values");
    }

    // Helper method to validate the wire content using the SnakeYaml parser
    protected void expectWithSnakeYaml(String expected, @NotNull Wire wire) {
        String s = wire.toString();
        @Nullable Object load;
        try {
            @NotNull Yaml yaml = new Yaml();
            load = yaml.load(new StringReader(s));
        } catch (Exception e) {
            throw e;
        }
        assertEquals(expected, load.toString(),
                "wire output should be valid YAML parseable by SnakeYaml");
    }

    @NotNull
    protected Bytes<?> allocateElasticOnHeap() {
        return Bytes.allocateElasticOnHeap();
    }

    // Test writing arrays of objects to a Wire and reading them back.
    @Test
    @DisplayName("Round-trips empty and simple arrays correctly")
    void testArrays() {
        // Create a wire instance
        @NotNull Wire wire = createWire();

        WireArrayTestSupport.assertEmptyArrayRoundTrip(wire, wire instanceof TextWire);

        wire.clear();
        WireArrayTestSupport.assertSimpleStringArrayRoundTrip(this::createWire, wire instanceof TextWire || wire instanceof YamlWire);
    }

    // Test writing arrays with varying lengths and types of elements to a Wire and reading them back.
    @Test
    @DisplayName("Round-trips object arrays of varying size")
    void testArrays2() {
        // Create a wire instance
        @NotNull Wire wire = createWire();

        WireArrayTestSupport.assertMixedArraysRoundTrip(wire);
    }

    // Test GZIP compression of text strings written to a Wire.
    @Test
    @DisplayName("Decompresses gzip text written to wire")
    @SuppressWarnings("deprecation")
    void testGZIPCompressionAsText() {
        // Create a wire instance and a string to compress
        @NotNull Wire wire = createWire();
        @NotNull final String s = "xxxxxxxxxxx1xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        @NotNull String str = s + s + s + s;

        // Get the string as bytes and write it to the wire with gzip compression
        @NotNull byte[] compressedBytes = str.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        wire.write().compress("gzip", Bytes.wrapForRead(compressedBytes));

        // Read the compressed string from the wire, decompress it, and assert its content
        @NotNull Bytes<?> bytes = allocateElasticOnHeap();
        wire.read().bytes(bytes);
        assertEquals(str, bytes.toString(),
                "wire should decompress GZIP compressed text correctly");
    }

    // Test to validate the behavior of writing and reading a long value
    @Test
    @DisplayName("Writes and reads int64 values correctly")
    void testInt64() {
        @NotNull Wire wire = createWire();

        // Write a long value with the key "VALUE"
        long expected = 1234567890123456789L;
        wire.write(() -> "VALUE").int64(expected);

        // Check the wire content using the SnakeYaml parser
        expectWithSnakeYaml("{VALUE=1234567890123456789}", wire);

        // Read and validate the written long value
        assertEquals(expected, wire.read(() -> "VALUE").int64(),
                "wire should preserve long value precision");
    }

    // Test to validate the behavior of writing and reading a short value
    @Test
    @DisplayName("Writes and reads int16 values correctly")
    void testInt16() {
        @NotNull Wire wire = createWire();

        // Write a short value with the key "VALUE"
        short expected = 12345;
        wire.write(() -> "VALUE").int64(expected);

        // Check the wire content using the SnakeYaml parser
        expectWithSnakeYaml("{VALUE=12345}", wire);

        // Read and validate the written short value
        assertEquals(expected, wire.read(() -> "VALUE").int16(),
                "wire should read short value within range correctly");
    }

    // Test to ensure that reading a value too large for a short throws an exception
    @Test
    @DisplayName("Rejects int16 overflow during read safely")
    void testInt16TooLarge() {
        assertThrows(IllegalStateException.class, () -> {
            @NotNull Wire wire = createWire();

            // Write the maximum long value with the key "VALUE"
            wire.write(() -> "VALUE").int64(Long.MAX_VALUE);

            // Attempt to read the value as a short, which should throw an exception
            wire.read(() -> "VALUE").int16();
        }, "int16 read should reject overflow");
    }

    // Test to validate the behavior of writing and reading an integer value
    @Test
    @DisplayName("Writes and reads int32 values correctly")
    void testInt32() {
        @NotNull Wire wire = createWire();

        // Write an integer value with the keys "VALUE" and "VALUE2"
        int expected = 1;
        wire.write(() -> "VALUE").int64(expected);
        wire.write(() -> "VALUE2").int64(expected);

        // Check the wire content using the SnakeYaml parser
        expectWithSnakeYaml("{VALUE=1, VALUE2=1}", wire);

        // Read and validate the written integer values
        assertEquals(expected, wire.read(() -> "VALUE").int16(),
                "wire should read first int32 value as int16 when within range");
        assertEquals(expected, wire.read(() -> "VALUE2").int16(),
                "wire should read second int32 value as int16 when within range");
    }

    // Test to ensure that reading a value too large for an integer throws an exception
    @Test
    @DisplayName("Rejects int32 overflow during read safely")
    void testInt32TooLarge() {
        assertThrows(IllegalStateException.class, () -> {
            @NotNull Wire wire = createWire();

            // Write the maximum integer value with the key "VALUE"
            wire.write(() -> "VALUE").int64(Integer.MAX_VALUE);

            // Attempt to read the value as a short, which should throw an exception
            wire.read(() -> "VALUE").int16();
        }, "int32 read should reject overflow");
    }

    // Test to validate writing using keys from the BWKey enum
    @Test
    @DisplayName("Writes enum keys without values correctly")
    void testWrite1() {
        @NotNull Wire wire = createWire();

        // Write fields using BWKey enum values
        wire.write(BWKey.field1);
        wire.write(BWKey.field2);
        wire.write(BWKey.field3);

        // Verify the wire content
        assertEquals("field1: field2: field3: ", wire.toString(),
                "wire should write enum keys as field names without values");
    }

    // Test to validate writing with different string lengths
    @Test
    @DisplayName("Writes field names with long strings")
    void testWrite2() {
        @NotNull Wire wire = createWire();

        // Write strings with varying lengths
        wire.write(() -> "Hello");
        wire.write(() -> "World");
        wire.write(() -> "Long field name which is more than 32 characters, Bye");

        // Verify the wire content
        assertEquals("Hello: World: \"Long field name which is more than 32 characters, Bye\": ", wire.toString(),
                "wire should handle field names of varying lengths including those exceeding 32 characters");
    }

    // Test to validate reading from the wire
    @Test
    @DisplayName("Consumes bytes when reading fields from wire")
    void testRead() {
        @NotNull Wire wire = createWire();

        // Write values to the wire
        wire.write();
        wire.write(BWKey.field1);
        wire.write(() -> "Test");

        long remainingBefore = wire.bytes().readRemaining();
        for (int i = 0; i < 3; i++) {
            wire.read();
        }
        assertTrue(wire.bytes().readRemaining() < remainingBefore,
                "wire read operations should consume bytes from the underlying buffer");
    }

    // Test the write behavior of custom Marshallable objects with Wire.
    @Test
    @DisplayName("Serialises and reads Marshallable objects correctly")
    void testWriteMarshallable() {
        // Create wire instance
        final Wire wire = createWire();
        @NotNull MyTypesCustom mtA = MyTypesCustomTestSupport.createA();

        // Write the first Marshallable instance (mtA) to wire
        wire.write(() -> "A").marshallable(mtA);

        @NotNull MyTypesCustom mtB = MyTypesCustomTestSupport.createB();

        // Write the second Marshallable instance (mtB) to wire
        wire.write(() -> "B").marshallable(mtB);

        // Assert the string format of wire after writing
        if (wire instanceof TextWire) {
            assertEquals("A: {\n" +
                    "  B_FLAG: true,\n" +
                    "  S_NUM: 12345,\n" +
                    "  D_NUM: 123.456,\n" +
                    "  L_NUM: 0,\n" +
                    "  I_NUM: -12345789,\n" +
                    "  TEXT: Hello World\n" +
                    "}\n" +
                    "B: {\n" +
                    "  B_FLAG: false,\n" +
                    "  S_NUM: 1234,\n" +
                    "  D_NUM: 123.4567,\n" +
                    "  L_NUM: 0,\n" +
                    "  I_NUM: -123457890,\n" +
                    "  TEXT: Bye now\n" +
                            "}\n", wire.bytes().toString(),
                    "wire should serialize Marshallable objects with nested field structure");
        }
        expectWithSnakeYaml("{A={B_FLAG=true, S_NUM=12345, D_NUM=123.456, L_NUM=0, I_NUM=-12345789, TEXT=Hello World}, " +
                "B={B_FLAG=false, S_NUM=1234, D_NUM=123.4567, L_NUM=0, I_NUM=-123457890, TEXT=Bye now}}", wire);

        @NotNull MyTypesCustom mt2 = new MyTypesCustom();

        // Read the Marshallable instances from wire and assert equality
        wire.read(() -> "A").marshallable(mt2);
        assertEquals(mt2, mtA, "wire should deserialize first Marshallable object correctly");

        wire.read(() -> "B").marshallable(mt2);
        assertEquals(mt2, mtB, "wire should deserialize second Marshallable object correctly");
    }

    // Test the write behavior of custom Marshallable objects with Wire,
    // and verify the length of written fields.
    @Test
    @DisplayName("Reports field length for marshallable writes")
    void testWriteMarshallableAndFieldLength() {
        // Create wire instance
        final Wire wire = createWire();
        @NotNull MyTypesCustom mtA = new MyTypesCustom();
        mtA.flag = true;
        mtA.d = 123.456;
        mtA.i = -12345789;
        mtA.s = (short) 12345;

        @NotNull ValueOut write = wire.write(() -> "A");

        // Determine the start position for field length calculation
        final long start = wire.bytes().writePosition();

        // Write the Marshallable instance to wire
        write.marshallable(mtA);

        // Calculate the length of written field
        final long fieldLen = wire.bytes().lengthWritten(start);

        // Assert the string format of wire after writing
        expectWithSnakeYaml("{A={B_FLAG=true, S_NUM=12345, D_NUM=123.456, L_NUM=0, I_NUM=-12345789, TEXT=}}", wire);

        @NotNull ValueIn read = wire.read(() -> "A");
        assertTrue(fieldLen > 0, "Field length should be positive but was " + fieldLen);
        long readLength = read.readLength();
        assertTrue(Math.abs(fieldLen - readLength) <= 1,
                "wire field length should match read length within one byte fieldLen=" + fieldLen + ", readLength=" + readLength);
    }


}
