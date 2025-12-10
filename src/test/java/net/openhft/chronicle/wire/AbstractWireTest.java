/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.Assert.*;

@SuppressWarnings("deprecation")
public abstract class AbstractWireTest extends WireTestCommon {

    protected Bytes<?> bytes;

    protected abstract Wire createWire();

    // Test case for working with ZonedDateTime values
    @Test
    public void testZonedDateTime() {
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
                "\"\": \"-999999999-01-01T00:00-00:01:15[Europe/London]\"\n", wire.toString());

        // Read back the ZonedDateTime values and validate
        wire.read().zonedDateTime(now, org.junit.Assert::assertEquals)
                .read().zonedDateTime(max, org.junit.Assert::assertEquals)
                .read().zonedDateTime(min, org.junit.Assert::assertEquals);

        // Repeat the process but write as a generic object
        wire.clear();
        wire.write().object(now)
                .write().object(max)
                .write().object(min);
        assertEquals("\"\": !ZonedDateTime \"" + now + "\"\n" +
                "\"\": !ZonedDateTime \"+999999999-12-31T23:59:59.999999999Z[Europe/London]\"\n" +
                "\"\": !ZonedDateTime \"-999999999-01-01T00:00-00:01:15[Europe/London]\"\n", wire.toString());
        wire.read().object(Object.class, now, org.junit.Assert::assertEquals)
                .read().object(Object.class, max, org.junit.Assert::assertEquals)
                .read().object(Object.class, min, org.junit.Assert::assertEquals);

        // Write as a ZonedDateTime object
        wire.clear();
        wire.write().object(ZonedDateTime.class, now)
                .write().object(ZonedDateTime.class, max)
                .write().object(ZonedDateTime.class, min);
        assertEquals("\"\": \"" + now + "\"\n" +
                "\"\": \"+999999999-12-31T23:59:59.999999999Z[Europe/London]\"\n" +
                "\"\": \"-999999999-01-01T00:00-00:01:15[Europe/London]\"\n", wire.toString());
        wire.read().object(ZonedDateTime.class, now, org.junit.Assert::assertEquals)
                .read().object(ZonedDateTime.class, max, org.junit.Assert::assertEquals)
                .read().object(ZonedDateTime.class, min, org.junit.Assert::assertEquals);
    }

    // Test to ensure a SortedSet is correctly written to and read from the Wire.
    @Test
    public void testSortedSet() {
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
                    "]\n", wire.toString());
        }

        // Read back the SortedSet from the Wire and validate its type and contents.
        @Nullable Object o = wire.read().object();
        assertTrue(o instanceof SortedSet);
        assertEquals(set, o);
    }

    // Test to ensure a SortedMap is correctly written to and read from the Wire.
    @Test
    public void testSortedMap() {
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
                    "}\n", wire.toString());
        }

        // Read back the SortedMap from the Wire and validate its type and contents.
        @Nullable Object o = wire.read().object();
        assertTrue(o instanceof SortedMap);
        assertEquals(set, o);
    }

    // Test to validate the behavior of writing and reading simple boolean values
    @Test
    public void testSimpleBool() {
        @NotNull Wire wire = createWire();

        // Write two boolean values with keys "F" and "T"
        wire.write(() -> "F").bool(false);
        wire.write(() -> "T").bool(true);

        // Verify that the written values are correctly represented in string format
        assertEquals("F: false\n" +
                "T: true\n", wire.toString());

        // Check the wire content using the SnakeYaml parser
        @NotNull String expected = "{F=false, T=true}";
        expectWithSnakeYaml(expected, wire);

        // Read and validate the written boolean values
        assertFalse(wire.read(() -> "F").bool());
        assertTrue(wire.read(() -> "T").bool());
    }

    // Test to validate the behavior when writing strings that cannot be converted to boolean
    @Test
    public void testFailingBool() {
        @NotNull Wire wire = createWire();

        // Write two non-boolean strings with keys "A" and "B"
        wire.write(() -> "A").text("");
        wire.write(() -> "B").text("other");

        // Verify the written strings
        assertEquals("A: \"\"\n" +
                "B: other\n", wire.toString());

        // Check the wire content using the SnakeYaml parser
        @NotNull String expected = "{A=, B=other}";
        expectWithSnakeYaml(expected, wire);

        // Ensure the written strings are not mistakenly interpreted as boolean values
        assertFalse(wire.read(() -> "A").bool());
        assertFalse(wire.read(() -> "B").bool());
    }

    // Test to validate the reading of non-boolean strings as Boolean objects
    @Test
    public void testFailingBoolean() {
        @NotNull Wire wire = createWire();

        // Write two non-boolean strings
        wire.write(() -> "A").text("");
        wire.write(() -> "B").text("other");

        // Verify the written strings
        assertEquals("A: \"\"\n" +
                "B: other\n", wire.toString());

        // Check the wire content using the SnakeYaml parser
        @NotNull String expected = "{A=, B=other}";
        expectWithSnakeYaml(expected, wire);

        // TODO: Handle the potential issue when reading a string as a Boolean object
        // assertEquals(null, wire.read(() -> "A").object(Boolean.class));
        assertEquals(false, wire.read(() -> "B").object(Boolean.class));
    }

    // Test to validate the behavior when writing text with a leading space
    @Test
    public void testLeadingSpace() {
        @NotNull Wire wire = createWire();

        // Write a string with a leading space
        wire.write().text(" leadingspace");

        // Ensure that the leading space is retained when reading back the string
        assertEquals(" leadingspace", wire.read().text());
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
        assertEquals(expected, load.toString());
    }

    @NotNull
    protected Bytes<?> allocateElasticOnHeap() {
        return Bytes.allocateElasticOnHeap();
    }

    // Test writing arrays of objects to a Wire and reading them back.
    @Test
    public void testArrays() {
        // Create a wire instance
        @NotNull Wire wire = createWire();

        WireArrayTestSupport.assertEmptyArrayRoundTrip(wire, wire instanceof TextWire);

        wire.clear();
        WireArrayTestSupport.assertSimpleStringArrayRoundTrip(this::createWire, wire instanceof TextWire || wire instanceof YamlWire);
    }

    // Test writing arrays with varying lengths and types of elements to a Wire and reading them back.
    @Test
    public void testArrays2() {
        // Create a wire instance
        @NotNull Wire wire = createWire();

        WireArrayTestSupport.writeAndAssertMixedArrays(wire);
    }

    // Test GZIP compression of text strings written to a Wire.
    @Test
    @SuppressWarnings("deprecation")
    public void testGZIPCompressionAsText() {
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
        assertEquals(str, bytes.toString());
    }

    // Test to validate the behavior of writing and reading a long value
    @Test
    public void testInt64() {
        @NotNull Wire wire = createWire();

        // Write a long value with the key "VALUE"
        long expected = 1234567890123456789L;
        wire.write(() -> "VALUE").int64(expected);

        // Check the wire content using the SnakeYaml parser
        expectWithSnakeYaml("{VALUE=1234567890123456789}", wire);

        // Read and validate the written long value
        assertEquals(expected, wire.read(() -> "VALUE").int64());
    }

    // Test to validate the behavior of writing and reading a short value
    @Test
    public void testInt16() {
        @NotNull Wire wire = createWire();

        // Write a short value with the key "VALUE"
        short expected = 12345;
        wire.write(() -> "VALUE").int64(expected);

        // Check the wire content using the SnakeYaml parser
        expectWithSnakeYaml("{VALUE=12345}", wire);

        // Read and validate the written short value
        assertEquals(expected, wire.read(() -> "VALUE").int16());
    }

    // Test to ensure that reading a value too large for a short throws an exception
    @Test(expected = IllegalStateException.class)
    public void testInt16TooLarge() {
        @NotNull Wire wire = createWire();

        // Write the maximum long value with the key "VALUE"
        wire.write(() -> "VALUE").int64(Long.MAX_VALUE);

        // Attempt to read the value as a short, which should throw an exception
        wire.read(() -> "VALUE").int16();
    }

    // Test to validate the behavior of writing and reading an integer value
    @Test
    public void testInt32() {
        @NotNull Wire wire = createWire();

        // Write an integer value with the keys "VALUE" and "VALUE2"
        int expected = 1;
        wire.write(() -> "VALUE").int64(expected);
        wire.write(() -> "VALUE2").int64(expected);

        // Check the wire content using the SnakeYaml parser
        expectWithSnakeYaml("{VALUE=1, VALUE2=1}", wire);

        // Read and validate the written integer values
        assertEquals(expected, wire.read(() -> "VALUE").int16());
        assertEquals(expected, wire.read(() -> "VALUE2").int16());
    }

    // Test to ensure that reading a value too large for an integer throws an exception
    @Test(expected = IllegalStateException.class)
    public void testInt32TooLarge() {
        @NotNull Wire wire = createWire();

        // Write the maximum integer value with the key "VALUE"
        wire.write(() -> "VALUE").int64(Integer.MAX_VALUE);

        // Attempt to read the value as a short, which should throw an exception
        wire.read(() -> "VALUE").int16();
    }

    // Test to validate writing using keys from the BWKey enum
    @Test
    public void testWrite1() {
        @NotNull Wire wire = createWire();

        // Write fields using BWKey enum values
        wire.write(BWKey.field1);
        wire.write(BWKey.field2);
        wire.write(BWKey.field3);

        // Verify the wire content
        assertEquals("field1: field2: field3: ", wire.toString());
    }

    // Test to validate writing with different string lengths
    @Test
    public void testWrite2() {
        @NotNull Wire wire = createWire();

        // Write strings with varying lengths
        wire.write(() -> "Hello");
        wire.write(() -> "World");
        wire.write(() -> "Long field name which is more than 32 characters, Bye");

        // Verify the wire content
        assertEquals("Hello: World: \"Long field name which is more than 32 characters, Bye\": ", wire.toString());
    }

    // Test to validate reading from the wire
    @Test
    public void testRead() {
        @NotNull Wire wire = createWire();

        // Write values to the wire
        wire.write();
        wire.write(BWKey.field1);
        wire.write(() -> "Test");
    }

    // Test the write behavior of custom Marshallable objects with Wire.
    @Test
    public void testWriteMarshallable() {
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
                    "}\n", wire.bytes().toString());
        }
        expectWithSnakeYaml("{A={B_FLAG=true, S_NUM=12345, D_NUM=123.456, L_NUM=0, I_NUM=-12345789, TEXT=Hello World}, " +
                "B={B_FLAG=false, S_NUM=1234, D_NUM=123.4567, L_NUM=0, I_NUM=-123457890, TEXT=Bye now}}", wire);

        @NotNull MyTypesCustom mt2 = new MyTypesCustom();

        // Read the Marshallable instances from wire and assert equality
        wire.read(() -> "A").marshallable(mt2);
        assertEquals(mt2, mtA);

        wire.read(() -> "B").marshallable(mt2);
        assertEquals(mt2, mtB);
    }

    // Test the write behavior of custom Marshallable objects with Wire,
    // and verify the length of written fields.
    @Test
    public void testWriteMarshallableAndFieldLength() {
        // Create wire instance
        final Wire wire = createWire();
        @NotNull MyTypesCustom mtA = new MyTypesCustom();
        mtA.flag = true;
        mtA.d = 123.456;
        mtA.i = -12345789;
        mtA.s = (short) 12345;

        @NotNull ValueOut write = wire.write(() -> "A");

        // Determine the start position for field length calculation
        final long start = wire.bytes().writePosition() + 1; // including one space for "sep".

        // Write the Marshallable instance to wire
        write.marshallable(mtA);

        // Calculate the length of written field
        final long fieldLen = wire.bytes().lengthWritten(start);

        // Assert the string format of wire after writing
        expectWithSnakeYaml("{A={B_FLAG=true, S_NUM=12345, D_NUM=123.456, L_NUM=0, I_NUM=-12345789, TEXT=}}", wire);

        @NotNull ValueIn read = wire.read(() -> "A");
    }


}
