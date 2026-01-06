/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.lang.reflect.Type;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static net.openhft.chronicle.bytes.Bytes.allocateElasticOnHeap;
import static org.junit.jupiter.api.Assertions.*;

// This class tests the functionalities related to the QueryWire's read and write operations.
@SuppressWarnings({"deprecation", "removal"})
class QueryWireTest extends WireTestCommon {

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
    @DisplayName("Reads and writes basic query fields")
    void readWriteQuery() {

        // Create a wire and write various data types to it
        @NotNull QueryWire wire = createWire();
        wire.write(() -> "bool").bool(true)
                .write(() -> "int").int64(12345)
                .write(() -> "text").text("Hello World")
                .write(() -> "float").float64(12.345);

        // Assert that the wire correctly serialized the data
        assertEquals("bool=true&int=12345&text=Hello World&float=12.345", bytes.toString(),
                "query wire should serialize multiple fields as ampersand-separated key-value pairs");

        // Read from the wire and verify each data type
        wire.read(() -> "bool").bool(this, (o, b) -> assertTrue(b, "query wire should read boolean value as true"))
                .read(() -> "int").int64(this, (o, i) -> assertEquals(12345, i, "query wire should read integer value as 12345"))
                .read(() -> "text").text(this, (o, s) -> assertEquals("Hello World", s, "query wire should read text value with spaces preserved"))
                .read(() -> "float").float64(this, (o, f) -> assertEquals(12.345, f, 0.0, "query wire should read floating point value as 12.345"));

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
        assertEquals(new ArrayList<>(Arrays.asList(true, 12345L, "Hello World", 12.345)), results,
                "query wire parser should extract all four field values in correct order and types");
        bytes.releaseLast();
    }

    @Test
    @DisplayName("Writes and reads query fragments with raw bytes")
    void writesAndReadsQueryFragments() {
        Bytes<?> bytes = allocateElasticOnHeap();
        QueryWire writer = new QueryWire(bytes);

        writer.write("flag").bool(true);
        writer.write("count").int64(42);
        writer.write("name").text("alpha beta");
        writer.write("raw").rawBytes("tail".getBytes(ISO_8859_1));
        writer.write("payload").bytes(new byte[]{1, 2, 3});

        String query = bytes.toString();
        assertTrue(query.contains("flag=true"), "query string should contain flag=true, actual: " + query);
        assertTrue(query.contains("count=42"), "query string should contain count=42, actual: " + query);
        assertTrue(query.contains("raw=tail"), "query string should contain raw=tail, actual: " + query);
        assertTrue(query.contains("payload="), "query string should contain payload=, actual: " + query);

        bytes.readPositionRemaining(0, bytes.writePosition());
        QueryWire reader = new QueryWire(bytes);

        assertEquals("true", reader.read("flag").text(), "query wire should read boolean field value as text 'true'");
        assertEquals(42L, reader.read("count").int64(), "query wire should read integer field value as 42");
        assertEquals("alpha beta", reader.read("name").text(), "query wire should read text field with spaces preserved");

        Bytes<?> payload = allocateElasticOnHeap();
        reader.read("payload").textTo(payload);
        assertEquals("AQID", payload.toString(), "query wire should decode base64-encoded byte array payload as 'AQID'");
        payload.releaseLast();

        bytes.releaseLast();
    }

    @Test
    @DisplayName("Percent-encoded characters remain literal in output")
    void percentEncodedCharactersRemainLiteral() {
        @NotNull QueryWire wire = createWire();
        String literal = "value%2Bplus+space";
        wire.write("token").text(literal);

        assertTrue(bytes.toString().contains("token=" + literal),
                "query wire should preserve percent-encoded characters literally without URL decoding");

        bytes.readPositionRemaining(0, bytes.writePosition());
        QueryWire reader = new QueryWire(bytes);
        assertEquals(literal, reader.read("token").text(),
                "query wire should read percent-encoded value with plus signs preserved literally");
        bytes.releaseLast();
    }

    @Test
    @DisplayName("Handles zero bytes and dangling keys")
    void handlesZeroBytesAndDanglingKeys() {
        Bytes<?> storage = allocateElasticOnHeap();
        QueryWire wire = new QueryWire(storage);
        byte[] raw = {'A', 0, 'B'};
        wire.write("raw").rawBytes(raw);
        wire.write("encoded").bytes(raw);

        storage.readPositionRemaining(0, storage.writePosition());
        QueryWire reader = new QueryWire(storage);
        Bytes<?> sink = allocateElasticOnHeap();
        reader.read("raw").textTo(sink);
        assertArrayEquals(raw, sink.toByteArray(), "query wire should handle raw bytes including zero bytes correctly");
        sink.releaseLast();
        assertEquals(Base64.getEncoder().encodeToString(raw), reader.read("encoded").text(),
                "query wire should base64-encode byte array containing zero bytes");
        storage.releaseLast();

        Bytes<?> truncated = Bytes.from("done=true&dangling");
        try {
            QueryWire danglingReader = new QueryWire(truncated);
            assertEquals("true", danglingReader.read("done").text(),
                    "query wire should read complete key-value pair correctly");
            assertEquals("", danglingReader.read("dangling").text(),
                    "query wire should return empty string for dangling key without value");
        } finally {
            truncated.releaseLast();
        }
    }

    @Test
    @DisplayName("Query output feeds text wire after formatting")
    void queryWireOutputCanFeedTextWireAfterFormatting() {
        @NotNull QueryWire wire = createWire();
        wire.write("name").text("alpha beta");
        wire.write("count").int64(7);

        String yamlLike = bytes.toString()
                .replace("&", "\n")
                .replace("=", ": ");

        Bytes<?> yamlBytes = Bytes.from(yamlLike);
        try {
            Wire textWire = WireType.TEXT.apply(yamlBytes);
            assertEquals("alpha beta", textWire.read("name").text(),
                    "text wire should read reformatted query wire output correctly for text field");
            assertEquals(7L, textWire.read("count").int64(),
                    "text wire should read reformatted query wire output correctly for integer field");
        } finally {
            yamlBytes.releaseLast();
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("write without a field name is unsupported")
    void writeWithoutNameIsUnsupported() {
        @NotNull QueryWire wire = createWire();
        assertThrows(UnsupportedOperationException.class, wire::write,
                "Query wire does not support unnamed writes");
        bytes.releaseLast();
    }

    @Test
    @DisplayName("Field without equals sign stops parsing at ampersand")
    void fieldWithoutEqualsStopsAtAmpersand() {
        Bytes<?> storage = Bytes.from("flag&next=ok");
        try {
            QueryWire wire = new QueryWire(storage);
            assertEquals("", wire.read("flag").text(),
                    "Query wire returns empty text for a field without an equals sign");
            assertEquals("ok", wire.read("next").text(),
                    "Query wire continues parsing after ampersand delimiters");
        } finally {
            storage.releaseLast();
        }
    }

    @Test
    @DisplayName("typeLiteralAsText forwards the parsed type literal value")
    void typeLiteralAsTextForwardsParsedValue() {
        Bytes<?> storage = Bytes.from("type=java.lang.String");
        try {
            QueryWire wire = new QueryWire(storage);
            StringBuilder captured = new StringBuilder();
            wire.read("type").typeLiteralAsText(captured, StringBuilder::append);
            assertEquals("java.lang.String", captured.toString(),
                    "Query wire should forward the parsed type literal text");
        } finally {
            storage.releaseLast();
        }
    }

    @Test
    @DisplayName("typeLiteral resolves known class names from input")
    void typeLiteralResolvesClassNames() {
        Bytes<?> storage = Bytes.from("type=java.lang.String");
        try {
            QueryWire wire = new QueryWire(storage);
            Type resolved = wire.read("type").typeLiteral((name, ex) -> {
                throw new AssertionError("Unexpected resolution failure for " + name);
            });
            assertEquals(String.class, resolved, "Query wire should resolve known class names");
        } finally {
            storage.releaseLast();
        }
    }

    @Test
    @DisplayName("Null boolean values do not emit fields")
    void nullBooleanValuesDoNotEmitFields() {
        @NotNull QueryWire wire = createWire();
        wire.write("flag").bool(null);
        wire.write("next").text("ok");

        assertEquals("next=ok", bytes.toString(), "Query wire skips null boolean values");
        bytes.releaseLast();
    }

    @Test
    @DisplayName("Sequence omits trailing comma when empty")
    void sequenceOmitsTrailingCommaWhenEmpty() {
        @NotNull QueryWire wire = createWire();
        wire.write("list").sequence("unused", (t, out) -> {
        });

        assertEquals("list=[]", bytes.toString(), "Query wire writes empty sequences without trailing commas");
        bytes.releaseLast();
    }

    @Test
    @DisplayName("Sequence appends comma when writer adds content")
    void sequenceAppendsCommaWhenWriterAddsContent() {
        @NotNull QueryWire wire = createWire();
        wire.write("list").sequence(wire, (w, out) -> w.bytes().appendUtf8("a"));

        assertEquals("list=[a,]", bytes.toString(), "Query wire appends a comma when sequence content is present");
        bytes.releaseLast();
    }

    @Test
    @DisplayName("hasNextSequenceItem consumes comma and stops at closing bracket")
    void hasNextSequenceItemConsumesCommaAndStopsAtClosingBracket() {
        Bytes<?> bytes = Bytes.from(",]");
        try {
            QueryWire wire = new QueryWire(bytes);
            ValueIn in = wire.getValueIn();

            assertTrue(in.hasNextSequenceItem(), "Query wire detects comma separated sequence entries");
            assertFalse(in.hasNextSequenceItem(), "Query wire stops at closing bracket");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("Query stop char testers recognise delimiters")
    void queryStopCharTestersRecogniseDelimiters() {
        assertTrue(QueryWire.QueryStopCharTesters.QUERY_FIELD_NAME.isStopChar('&'),
                "Field name stop tester stops on ampersand");
        assertTrue(QueryWire.QueryStopCharTesters.QUERY_FIELD_NAME.isStopChar('='),
                "Field name stop tester stops on equals");
        assertTrue(QueryWire.QueryStopCharTesters.QUERY_FIELD_NAME.isStopChar(-1),
                "Field name stop tester stops on end of input");
        assertFalse(QueryWire.QueryStopCharTesters.QUERY_FIELD_NAME.isStopChar('a'),
                "Field name stop tester allows normal characters");

        assertTrue(QueryWire.QueryStopCharTesters.QUERY_VALUE.isStopChar('&'),
                "Value stop tester stops on ampersand");
        assertTrue(QueryWire.QueryStopCharTesters.QUERY_VALUE.isStopChar(-1),
                "Value stop tester stops on end of input");
        assertFalse(QueryWire.QueryStopCharTesters.QUERY_VALUE.isStopChar('='),
                "Value stop tester allows equals inside values");
    }
}
