//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static net.openhft.chronicle.bytes.Bytes.allocateElasticOnHeap;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// This class tests the functionalities related to the QueryWire's read and write operations.
public class QueryWireTest extends WireTestCommon {

    // Byte storage to hold serialized data
    private Bytes<?> bytes;

    // Factory method to create and return a QueryWire instance.
    // Initializes the byte storage and associates it with the wire.
    @NotNull
    private QueryWire createWire() {
        bytes = allocateElasticOnHeap();
        return new QueryWire(bytes);
    }

    // Test case to verify both write and read operations of the QueryWire
    @Test
    public void readWriteQuery() {

        // Create a wire and write various data types to it
        @NotNull QueryWire wire = createWire();
        wire.write(() -> "bool").bool(true)
                .write(() -> "int").int64(12345)
                .write(() -> "text").text("Hello World")
                .write(() -> "float").float64(12.345);

        // Assert that the wire correctly serialized the data
        assertEquals("bool=true&int=12345&text=Hello World&float=12.345", bytes.toString());

        // Read from the wire and verify each data type
        wire.read(() -> "bool").bool(this, (o, b) -> assertTrue(b))
                .read(() -> "int").int64(this, (o, i) -> assertEquals(12345, i))
                .read(() -> "text").text(this, (o, s) -> assertEquals("Hello World", s))
                .read(() -> "float").float64(this, (o, f) -> assertEquals(12.345, f, 0.0));

        // Set up a WireParser to process each data type and add the values to a results list
        @NotNull WireParser wp = WireParser.wireParser((s, v) -> System.err.println(s + " " + v.text()));
        @NotNull List<Object> results = new ArrayList<>();
        wp.register(() -> "bool", (s, v) -> v.bool(results, List::add));
        wp.register(() -> "int", (s, v) -> v.int64(results, List::add));
        wp.register(() -> "text", (s, v) -> v.text(results, List::add));
        wp.register(() -> "float", (s, v) -> v.float64(results, List::add));

        // Reset the read position and use the WireParser to extract the data
        bytes.readPosition(0);
        while (bytes.readRemaining() > 0)
            wp.parseOne(wire);

        // Verify that the results list contains the correct values
        assertEquals(new ArrayList<>(Arrays.asList(true, 12345L, "Hello World", 12.345)), results);

        bytes.releaseLast();
    }

    @Test
    public void writesAndReadsQueryFragments() {
        Bytes<?> bytes = allocateElasticOnHeap();
        QueryWire writer = new QueryWire(bytes);

        writer.write("flag").bool(true);
        writer.write("count").int64(42);
        writer.write("name").text("alpha beta");
        writer.write("raw").rawBytes("tail".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        writer.write("payload").bytes(new byte[]{1, 2, 3});

        String query = bytes.toString();
        assertTrue(query.contains("flag=true"));
        assertTrue(query.contains("count=42"));
        assertTrue(query.contains("raw=tail"));
        assertTrue(query.contains("payload="));

        bytes.readPositionRemaining(0, bytes.writePosition());
        QueryWire reader = new QueryWire(bytes);

        assertEquals("true", reader.read("flag").text());
        assertEquals(42L, reader.read("count").int64());
        assertEquals("alpha beta", reader.read("name").text());

        Bytes<?> payload = allocateElasticOnHeap();
        reader.read("payload").textTo(payload);
        assertEquals("AQID", payload.toString());
        payload.releaseLast();

        bytes.releaseLast();
    }

    @Test
    public void percentEncodedCharactersRemainLiteral() {
        @NotNull QueryWire wire = createWire();
        String literal = "value%2Bplus+space";
        wire.write("token").text(literal);

        assertTrue(bytes.toString().contains("token=" + literal));

        bytes.readPositionRemaining(0, bytes.writePosition());
        QueryWire reader = new QueryWire(bytes);
        assertEquals(literal, reader.read("token").text());
        bytes.releaseLast();
    }

    @Test
    public void handlesZeroBytesAndDanglingKeys() {
        Bytes<?> storage = allocateElasticOnHeap();
        QueryWire wire = new QueryWire(storage);
        byte[] raw = new byte[]{'A', 0, 'B'};
        wire.write("raw").rawBytes(raw);
        wire.write("encoded").bytes(raw);

        storage.readPositionRemaining(0, storage.writePosition());
        QueryWire reader = new QueryWire(storage);
        Bytes<?> sink = allocateElasticOnHeap();
        reader.read("raw").textTo(sink);
        assertArrayEquals(raw, sink.toByteArray());
        sink.releaseLast();
        assertEquals(Base64.getEncoder().encodeToString(raw), reader.read("encoded").text());
        storage.releaseLast();

        Bytes<?> truncated = Bytes.from("done=true&dangling");
        try {
            QueryWire danglingReader = new QueryWire(truncated);
            assertEquals("true", danglingReader.read("done").text());
            assertEquals("", danglingReader.read("dangling").text());
        } finally {
            truncated.releaseLast();
        }
    }

    @Test
    public void queryWireOutputCanFeedTextWireAfterFormatting() {
        @NotNull QueryWire wire = createWire();
        wire.write("name").text("alpha beta");
        wire.write("count").int64(7);

        String yamlLike = bytes.toString()
                .replace("&", "\n")
                .replace("=", ": ");

        Bytes<?> yamlBytes = Bytes.from(yamlLike);
        try {
            Wire textWire = WireType.TEXT.apply(yamlBytes);
            assertEquals("alpha beta", textWire.read("name").text());
            assertEquals(7L, textWire.read("count").int64());
        } finally {
            yamlBytes.releaseLast();
            bytes.releaseLast();
        }
    }
}
