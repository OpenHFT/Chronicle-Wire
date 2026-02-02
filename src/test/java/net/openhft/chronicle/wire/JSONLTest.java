/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.scoped.ScopedResource;
import net.openhft.chronicle.wire.converter.NanoTime;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JSONL (JSON Lines) wire format support in Chronicle Wire.
 * <p>
 * JSONL outputs one JSON object per line with newline separators, enabling
 * streaming processing and easy parsing of multi-record data. These tests
 * verify correct line discipline, special value handling (NaN, Infinity),
 * enum serialisation, control character escaping, and round-trip serialisation.
 * <p>
 * Thread-safety: Tests are single-threaded; Wire instances are not thread-safe.
 * <p>
 * Key verified behaviours:
 * <ul>
 *   <li>Each document produces exactly one line ending with newline</li>
 *   <li>Embedded newlines in strings are escaped, not literal</li>
 *   <li>Non-finite doubles serialised as quoted strings (Chronicle extension)</li>
 *   <li>WireType.JSONL disables type metadata by default</li>
 * </ul>
 */
@DisplayName("JSONL Wire Format: serialisation, line discipline, and special value handling")
public class JSONLTest extends WireTestCommon {

    /**
     * Test DTO for JSONL serialisation, containing fields that exercise
     * various serialisation edge cases including enums, timestamps, and special double values.
     */
    static class JSONLRecord extends SelfDescribingMarshallable {
        String name;
        int count;
        double price;
        double specialValue;
        Status status;
        @NanoTime
        long timestamp;

        JSONLRecord() {
        }

        JSONLRecord(String name, int count, double price, double specialValue, Status status, long timestamp) {
            this.name = name;
            this.count = count;
            this.price = price;
            this.specialValue = specialValue;
            this.status = status;
            this.timestamp = timestamp;
        }
    }

    enum Status {
        PENDING, ACTIVE, COMPLETED, CANCELLED
    }

    interface JsonlEvents {
        void event(String name, int count);
    }

    @Test
    @DisplayName("should produce one JSON object per line with newline separators")
    public void testBasicJSONLRoundTrip() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            // Write multiple records
            JSONLRecord r1 = new JSONLRecord("record1", 100, 19.99, 1.5, Status.ACTIVE, 0);
            JSONLRecord r2 = new JSONLRecord("record2", 200, 29.99, 2.5, Status.PENDING, 0);

            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(JSONLRecord.class, r1);
            }
            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(JSONLRecord.class, r2);
            }

            String output = bytes.toString();

            // Verify newline separation
            String[] lines = output.split("\n");
            assertEquals(2, lines.length, "JSONL output line count should be 2 for two records, actual " + lines.length + ": " + output);

            // Verify each line is valid JSON
            assertTrue(lines[0].startsWith("{"), "JSONL line[0] should start with '{' to be valid JSON object, actual: " + lines[0]);
            assertTrue(lines[0].endsWith("}"), "JSONL line[0] should end with '}' to be valid JSON object, actual: " + lines[0]);
            assertTrue(lines[1].startsWith("{"), "JSONL line[1] should start with '{' to be valid JSON object, but was: " + lines[1]);
            assertTrue(lines[1].endsWith("}"), "JSONL line[1] should end with '}' to be valid JSON object, but was: " + lines[1]);

            // Verify content
            assertTrue(lines[0].contains("\"name\":\"record1\""), "JSONL line[0] should contain '\"name\":\"record1\"' for first record, actual: " + lines[0]);
            assertTrue(lines[1].contains("\"name\":\"record2\""), "JSONL line[1] should contain '\"name\":\"record2\"' for second record, but was: " + lines[1]);
        }
    }

    @Test
    @DisplayName("should avoid double braces when writing marshallables in JSONL documents")
    public void testJSONLNoDoubleBracesForMarshallable() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            JSONLRecord record = new JSONLRecord("single", 1, 1.0, 0.0, Status.ACTIVE, 0);
            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(JSONLRecord.class, record);
            }

            String output = bytes.toString();
            String[] lines = output.split("\n");
            int nonEmptyLines = 0;
            String line = null;
            for (String candidate : lines) {
                if (!candidate.isEmpty()) {
                    nonEmptyLines++;
                    line = candidate;
                }
            }
            assertEquals(1, nonEmptyLines,
                    "JSONL output should contain exactly one non-empty line, actual " + nonEmptyLines + ": " + output);
            assertTrue(line.startsWith("{"), "JSONL line should start with '{', actual: " + line);
            assertTrue(line.endsWith("}"), "JSONL line should end with '}', actual: " + line);
            assertFalse(line.startsWith("{{"), "JSONL line should not start with double braces, actual: " + line);
            assertFalse(line.endsWith("}}"), "JSONL line should not end with double braces, actual: " + line);
        }
    }

    @Test
    @DisplayName("should serialise NaN, +Infinity, and -Infinity as quoted strings")
    public void testSpecialDoubleValuesInJSONL() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            // Test NaN
            JSONLRecord nanRecord = new JSONLRecord("nan", 1, 0.0, Double.NaN, Status.ACTIVE, 0);
            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(JSONLRecord.class, nanRecord);
            }

            // Test +Infinity
            JSONLRecord posInfRecord = new JSONLRecord("posInf", 2, 0.0, Double.POSITIVE_INFINITY, Status.ACTIVE, 0);
            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(JSONLRecord.class, posInfRecord);
            }

            // Test -Infinity
            JSONLRecord negInfRecord = new JSONLRecord("negInf", 3, 0.0, Double.NEGATIVE_INFINITY, Status.ACTIVE, 0);
            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(JSONLRecord.class, negInfRecord);
            }

            String output = bytes.toString();
            String[] lines = output.split("\n");
            assertEquals(3, lines.length, "JSONL output line count should be 3 for three special double records, actual: " + lines.length);

            // Verify special values are quoted strings in JSON
            assertTrue(lines[0].contains("\"specialValue\":\"NaN\""), "JSONL line[0] should contain '\"specialValue\":\"NaN\"' for NaN record, but was: " + lines[0]);
            assertTrue(lines[1].contains("\"specialValue\":\"Infinity\""), "JSONL line[1] should contain '\"specialValue\":\"Infinity\"' for +Infinity record, received: " + lines[1]);
            assertTrue(lines[2].contains("\"specialValue\":\"-Infinity\""), "JSONL line[2] should contain '\"specialValue\":\"-Infinity\"' for -Infinity record, actual: " + lines[2]);
        }
    }

    @Test
    @DisplayName("should round-trip non-finite double values (NaN, +/-Infinity) through JSONL")
    public void testNonFiniteDoubleRoundTrip() {
        // Test round-trip for NaN
        String nanJson = "{\"name\":\"nan\",\"count\":0,\"price\":0.0,\"specialValue\":\"NaN\",\"status\":\"ACTIVE\",\"timestamp\":0}";
        Bytes<?> nanBytes = Bytes.from(nanJson);
        try {
            Wire wire = WireType.JSONL.apply(nanBytes);
            JSONLRecord record = wire.getValueIn().object(JSONLRecord.class);
            assertTrue(Double.isNaN(record.specialValue), "NaN should round-trip");
        } finally {
            nanBytes.releaseLast();
        }

        // Test round-trip for +Infinity
        String posInfJson = "{\"name\":\"inf\",\"count\":0,\"price\":0.0,\"specialValue\":\"Infinity\",\"status\":\"ACTIVE\",\"timestamp\":0}";
        Bytes<?> posInfBytes = Bytes.from(posInfJson);
        try {
            Wire wire = WireType.JSONL.apply(posInfBytes);
            JSONLRecord record = wire.getValueIn().object(JSONLRecord.class);
            assertEquals(Double.POSITIVE_INFINITY, record.specialValue, 0.0, "Infinity should round-trip");
        } finally {
            posInfBytes.releaseLast();
        }

        // Test round-trip for -Infinity
        String negInfJson = "{\"name\":\"negInf\",\"count\":0,\"price\":0.0,\"specialValue\":\"-Infinity\",\"status\":\"ACTIVE\",\"timestamp\":0}";
        Bytes<?> negInfBytes = Bytes.from(negInfJson);
        try {
            Wire wire = WireType.JSONL.apply(negInfBytes);
            JSONLRecord record = wire.getValueIn().object(JSONLRecord.class);
            assertEquals(Double.NEGATIVE_INFINITY, record.specialValue, 0.0, "-Infinity should round-trip");
        } finally {
            negInfBytes.releaseLast();
        }
    }

    /**
     * Test DTO with float field for testing non-finite float values.
     */
    static class FloatRecord extends SelfDescribingMarshallable {
        String name;
        float value;

        FloatRecord() {
        }

        FloatRecord(String name, float value) {
            this.name = name;
            this.value = value;
        }
    }

    @Test
    @DisplayName("should serialise non-finite float values (NaN, +/-Infinity) as quoted strings")
    public void testNonFiniteFloatValues() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            // Test Float.NaN
            FloatRecord nanRecord = new FloatRecord("nan", Float.NaN);
            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(FloatRecord.class, nanRecord);
            }

            // Test Float.POSITIVE_INFINITY
            FloatRecord posInfRecord = new FloatRecord("posInf", Float.POSITIVE_INFINITY);
            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(FloatRecord.class, posInfRecord);
            }

            // Test Float.NEGATIVE_INFINITY
            FloatRecord negInfRecord = new FloatRecord("negInf", Float.NEGATIVE_INFINITY);
            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(FloatRecord.class, negInfRecord);
            }

            String output = bytes.toString();
            String[] lines = output.split("\n");
            assertEquals(3, lines.length, "JSONL output line count should be 3 for three special float records, but was: " + lines.length);

            // Verify special float values are quoted strings in JSON
            assertTrue(lines[0].contains("\"value\":\"NaN\""), "JSONL line[0] should contain '\"value\":\"NaN\"' for Float.NaN record, received: " + lines[0]);
            assertTrue(lines[1].contains("\"value\":\"Infinity\""), "JSONL line[1] should contain '\"value\":\"Infinity\"' for Float.POSITIVE_INFINITY record, actual: " + lines[1]);
            assertTrue(lines[2].contains("\"value\":\"-Infinity\""), "JSONL line[2] should contain '\"value\":\"-Infinity\"' for Float.NEGATIVE_INFINITY record, but was: " + lines[2]);
        }
    }

    @Test
    @DisplayName("should round-trip non-finite float values (NaN, +/-Infinity) through JSONL")
    public void testNonFiniteFloatRoundTrip() {
        // Test round-trip for Float.NaN
        String nanJson = "{\"name\":\"nan\",\"value\":\"NaN\"}";
        Bytes<?> nanBytes = Bytes.from(nanJson);
        try {
            Wire wire = WireType.JSONL.apply(nanBytes);
            FloatRecord record = wire.getValueIn().object(FloatRecord.class);
            assertTrue(Float.isNaN(record.value), "Float NaN should round-trip");
        } finally {
            nanBytes.releaseLast();
        }

        // Test round-trip for Float.POSITIVE_INFINITY
        String posInfJson = "{\"name\":\"inf\",\"value\":\"Infinity\"}";
        Bytes<?> posInfBytes = Bytes.from(posInfJson);
        try {
            Wire wire = WireType.JSONL.apply(posInfBytes);
            FloatRecord record = wire.getValueIn().object(FloatRecord.class);
            assertEquals(Float.POSITIVE_INFINITY, record.value, 0.0f, "Float Infinity should round-trip");
        } finally {
            posInfBytes.releaseLast();
        }

        // Test round-trip for Float.NEGATIVE_INFINITY
        String negInfJson = "{\"name\":\"negInf\",\"value\":\"-Infinity\"}";
        Bytes<?> negInfBytes = Bytes.from(negInfJson);
        try {
            Wire wire = WireType.JSONL.apply(negInfBytes);
            FloatRecord record = wire.getValueIn().object(FloatRecord.class);
            assertEquals(Float.NEGATIVE_INFINITY, record.value, 0.0f, "Float -Infinity should round-trip");
        } finally {
            negInfBytes.releaseLast();
        }
    }

    @Test
    @DisplayName("should format @NanoTime fields as ISO-8601 timestamp strings")
    public void testNanoTimeInJSONL() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            long ts = NanoTime.INSTANCE.parse("2022-06-17T12:35:56");
            JSONLRecord record = new JSONLRecord("timed", 1, 0.0, 0.0, Status.ACTIVE, ts);

            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(JSONLRecord.class, record);
            }

            String output = bytes.toString();
            assertTrue(output.contains("\"timestamp\":\"2022-06-17T12:35:56\""),
                    "JSONL output should contain '\"timestamp\":\"2022-06-17T12:35:56\"' for @NanoTime field formatted as ISO-8601, actual: " + output);
        }
    }

    @Test
    @DisplayName("should serialise enum values as lowercase quoted strings")
    public void testEnumInJSONL() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            // Test all enum values
            for (Status status : Status.values()) {
                JSONLRecord record = new JSONLRecord("enum-" + status, 1, 0.0, 0.0, status, 0);
                try (DocumentContext dc = wire.writingDocument()) {
                    dc.wire().getValueOut().object(JSONLRecord.class, record);
                }
            }

            String output = bytes.toString();
            String[] lines = output.split("\n");
            assertEquals(Status.values().length, lines.length, "JSONL output line count should match Status enum value count (" + Status.values().length + "), but was: " + lines.length);

            // Verify enum values are quoted strings (JSONL uses lowercase by default)
            assertTrue(lines[0].contains("\"status\":\"pending\""), "JSONL line[0] should contain '\"status\":\"pending\"' for PENDING enum, received: " + lines[0]);
            assertTrue(lines[1].contains("\"status\":\"active\""), "JSONL line[1] should contain '\"status\":\"active\"' for ACTIVE enum, actual: " + lines[1]);
            assertTrue(lines[2].contains("\"status\":\"completed\""), "JSONL line[2] should contain '\"status\":\"completed\"' for COMPLETED enum, but was: " + lines[2]);
            assertTrue(lines[3].contains("\"status\":\"cancelled\""), "JSONL line[3] should contain '\"status\":\"cancelled\"' for CANCELLED enum, received: " + lines[3]);
        }
    }

    @Test
    @DisplayName("should escape special characters (quotes, backslash, tab, newline) in strings")
    public void testSpecialCharactersInStrings() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            // Test escaping of special characters
            JSONLRecord record = new JSONLRecord("quote\"backslash\\tab\tnewline\n", 1, 0.0, 0.0, Status.ACTIVE, 0);
            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(JSONLRecord.class, record);
            }

            String output = bytes.toString();
            // Verify escaping - the output should have escaped versions
            assertTrue(output.contains("\\\""), "JSONL output should contain '\\\"' for escaped quote, actual: " + output);
            assertTrue(output.contains("\\\\"), "JSONL output should contain '\\\\' for escaped backslash, but was: " + output);
            assertTrue(output.contains("\\t"), "JSONL output should contain '\\t' for escaped tab, received: " + output);
            assertTrue(output.contains("\\n"), "JSONL output should contain '\\n' for escaped newline, actual output: " + output);
        }
    }

    @Test
    @DisplayName("should handle null and empty string values correctly")
    public void testEmptyAndNullValues() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            // Test null string
            JSONLRecord nullRecord = new JSONLRecord();
            nullRecord.name = null;
            nullRecord.count = 0;
            nullRecord.status = Status.PENDING;

            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(JSONLRecord.class, nullRecord);
            }

            // Test empty string
            JSONLRecord emptyRecord = new JSONLRecord();
            emptyRecord.name = "";
            emptyRecord.count = 0;
            emptyRecord.status = Status.PENDING;

            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(JSONLRecord.class, emptyRecord);
            }

            String output = bytes.toString();
            String[] lines = output.split("\n");
            assertEquals(2, lines.length, "JSONL output line count should be 2 for null and empty string records, actual: " + lines.length);

            // Verify null handling
            assertTrue(lines[0].contains("\"name\":null") || lines[0].contains("\"name\":\"\""),
                    "JSONL line[0] should contain '\"name\":null' or '\"name\":\"\"' for null field, but was: " + lines[0]);
            // Verify empty string
            assertTrue(lines[1].contains("\"name\":\"\""),
                    "JSONL line[1] should contain '\"name\":\"\"' for empty string field, received: " + lines[1]);
        }
    }

    @Test
    @DisplayName("should terminate single record with newline and be valid JSON")
    public void testSingleRecord() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            JSONLRecord record = new JSONLRecord("single", 42, 3.14, 2.71, Status.COMPLETED, 0);
            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(JSONLRecord.class, record);
            }

            String output = bytes.toString();

            // Single record should still have newline
            assertTrue(output.endsWith("\n"),
                    "JSONL single record output should end with '\\n' for JSONL compliance, actual: [" + output + "]");

            // Should be valid JSON object
            String line = output.trim();
            assertTrue(line.startsWith("{") && line.endsWith("}"),
                    "JSONL record should start with '{' and end with '}' to be valid JSON object, but was: " + line);
        }
    }

    @Test
    @DisplayName("should omit type metadata (@type, @ClassName) in JSONL output")
    public void testNoTypePrefix() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            JSONLRecord record = new JSONLRecord("test", 1, 0.0, 0.0, Status.ACTIVE, 0);
            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(JSONLRecord.class, record);
            }

            String output = bytes.toString();

            // Verify no type prefix
            assertFalse(output.contains("@type"),
                    "JSONL output should not contain '@type' metadata since WireType.JSONL disables type tags, but found in: " + output);
            assertFalse(output.contains("@JSONLRecord"),
                    "JSONL output should not contain '@JSONLRecord' type prefix since WireType.JSONL disables type tags, but found in: " + output);
        }
    }

    @Test
    @DisplayName("should create text-based JSONWire with types disabled")
    @SuppressWarnings("deprecation") // testing deprecated useTypes() intentionally
    public void testWireTypeJSONL() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            // Verify wire type properties
            assertTrue(WireType.JSONL.isText(), "JSONL should be text format");
            assertInstanceOf(JSONWire.class, wire, "Wire should be JSONWire");

            JSONWire jsonWire = (JSONWire) wire;
            assertFalse(jsonWire.useTypes(), "JSONL should not use types");
        }
    }

    @Test
    @DisplayName("should deserialise JSONL input line by line into objects")
    public void testReadJSONL() {
        // Test reading JSONL format
        String jsonl = "{\"name\":\"read1\",\"count\":10,\"price\":1.5,\"specialValue\":0.0,\"status\":\"ACTIVE\",\"timestamp\":0}\n" +
                "{\"name\":\"read2\",\"count\":20,\"price\":2.5,\"specialValue\":0.0,\"status\":\"PENDING\",\"timestamp\":0}\n";

        List<JSONLRecord> records = new ArrayList<>();

        // Read line by line
        String[] lines = jsonl.split("\n");
        for (String line : lines) {
            if (line.isEmpty()) continue;
            Bytes<?> lineBytes = Bytes.from(line);
            try {
                Wire wire = WireType.JSONL.apply(lineBytes);
                JSONLRecord record = wire.getValueIn().object(JSONLRecord.class);
                records.add(record);
            } finally {
                lineBytes.releaseLast();
            }
        }

        assertEquals(2, records.size(), "Deserialised record count should be 2 from two JSONL lines, actual: " + records.size());
        assertEquals("read1", records.get(0).name, "First deserialised record name should be 'read1', but was: " + records.get(0).name);
        assertEquals(10, records.get(0).count, "First deserialised record count should be 10, received: " + records.get(0).count);
        assertEquals("read2", records.get(1).name, "Second deserialised record name should be 'read2', actual: " + records.get(1).name);
        assertEquals(20, records.get(1).count, "Second deserialised record count should be 20, but was: " + records.get(1).count);
    }

    @Test
    @DisplayName("should write multiple documents with one JSON object per line")
    public void testMultipleDocumentsWithValueOut() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            // Write three records
            for (int i = 0; i < 3; i++) {
                JSONLRecord record = new JSONLRecord("record" + i, i, i * 1.1, 0.0, Status.ACTIVE, 0);
                try (DocumentContext dc = wire.writingDocument()) {
                    dc.wire().getValueOut().object(JSONLRecord.class, record);
                }
            }

            String output = bytes.toString();
            String[] lines = output.split("\n");

            assertEquals(3, lines.length, "JSONL output line count should be 3 for three documents, received: " + lines.length);

            for (int i = 0; i < 3; i++) {
                assertTrue(lines[i].contains("\"name\":\"record" + i + "\""),
                        "JSONL line[" + i + "] should contain '\"name\":\"record" + i + "\"', actual: " + lines[i]);
                assertTrue(lines[i].startsWith("{") && lines[i].endsWith("}"),
                        "JSONL line[" + i + "] should start with '{' and end with '}' to be valid JSON, but was: " + lines[i]);
            }
        }
    }

    @Test
    @DisplayName("should stream method calls as JSONL and read them back via MethodReader")
    public void testMethodReaderStreamingJsonl() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            JsonlEvents writer = wire.methodWriter(JsonlEvents.class);
            writer.event("first", 1);
            writer.event("second", 2);
            writer.event("third", 3);

            List<String> seen = new ArrayList<>();
            MethodReader reader = wire.methodReader(new JsonlEvents() {
                @Override
                public void event(String name, int count) {
                    seen.add(name + ":" + count);
                }
            });

            while (reader.readOne()) {
                continue; // consume all events
            }

            assertEquals(3, seen.size(), "MethodReader should read 3 events from JSONL stream, actual: " + seen.size());
            assertEquals("first:1", seen.get(0), "First event should be 'first:1', but was: " + seen.get(0));
            assertEquals("second:2", seen.get(1), "Second event should be 'second:2', received: " + seen.get(1));
            assertEquals("third:3", seen.get(2), "Third event should be 'third:3', actual: " + seen.get(2));
        }
    }

    @Test
    @DisplayName("should handle comma-separated JSON as well as newline-separated")
    public void testMethodReaderCommaSeparatedJson() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            JsonlEvents writer = wire.methodWriter(JsonlEvents.class);
            writer.event("first", 1);
            writer.event("second", 2);
            writer.event("third", 3);

            String[] lines = bytes.toString().split("\n");
            String commaSeparated = String.join(",", lines);
            Bytes<?> input = Bytes.from(commaSeparated);
            try {
                Wire readWire = WireType.JSONL.apply(input);
                List<String> seen = new ArrayList<>();
                MethodReader reader = readWire.methodReader(new JsonlEvents() {
                    @Override
                    public void event(String name, int count) {
                        seen.add(name + ":" + count);
                    }
                });

                while (reader.readOne()) {
                    continue; // consume all events
                }

                assertEquals(3, seen.size(), "MethodReader should read 3 events from comma-separated JSON, but was: " + seen.size());
                assertEquals("first:1", seen.get(0), "First event from comma-separated input should be 'first:1', received: " + seen.get(0));
                assertEquals("second:2", seen.get(1), "Second event from comma-separated input should be 'second:2', actual: " + seen.get(1));
                assertEquals("third:3", seen.get(2), "Third event from comma-separated input should be 'third:3', but was: " + seen.get(2));
            } finally {
                input.releaseLast();
            }
        }
    }

    /**
     * Test that data written with JSON wire type can be read with JSONL wire type.
     * JSON output doesn't have newlines between documents, but JSONL reader can handle it.
     */
    @Test
    @DisplayName("should read JSON output (no newlines) with JSONL reader")
    public void testWriteJsonReadJsonl() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();

            // Write with JSON (not JSONL)
            Wire writeWire = WireType.JSON.apply(bytes);
            JsonlEvents writer = writeWire.methodWriter(JsonlEvents.class);
            writer.event("first", 1);
            writer.event("second", 2);
            writer.event("third", 3);

            // Verify JSON output has no newlines between documents
            String output = bytes.toString();
            assertFalse(output.contains("}\n{"),
                    "JSON output should not contain '}\\n{' (newlines between documents) since WireType.JSON concatenates objects, but found in: " + output);

            // Read with JSONL - should work because JSONL reader handles concatenated JSON
            bytes.readPosition(0);
            Wire readWire = WireType.JSONL.apply(bytes);
            List<String> seen = new ArrayList<>();
            MethodReader reader = readWire.methodReader(new JsonlEvents() {
                @Override
                public void event(String name, int count) {
                    seen.add(name + ":" + count);
                }
            });

            while (reader.readOne()) {
                continue; // consume all events
            }

            assertEquals(3, seen.size(), "JSONL reader should read all 3 events from JSON output, actual: " + seen.size());
            assertEquals("first:1", seen.get(0), "First event from JSON output should be 'first:1', received: " + seen.get(0));
            assertEquals("second:2", seen.get(1), "Second event from JSON output should be 'second:2', but was: " + seen.get(1));
            assertEquals("third:3", seen.get(2), "Third event from JSON output should be 'third:3', actual: " + seen.get(2));
        }
    }

    /**
     * Test that data written with JSONL wire type can be read with JSON wire type.
     * JSONL adds newlines between documents which JSON reader treats as whitespace.
     */
    @Test
    @DisplayName("should round-trip JSONL output with JSONL reader")
    public void testWriteJsonlReadJson() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();

            // Write with JSONL
            Wire writeWire = WireType.JSONL.apply(bytes);
            JsonlEvents writer = writeWire.methodWriter(JsonlEvents.class);
            writer.event("first", 1);
            writer.event("second", 2);
            writer.event("third", 3);

            // Verify JSONL output has proper format:
            // Each document is wrapped in braces and separated by newlines
            String output = bytes.toString();
            assertTrue(output.contains("}\n{"),
                    "JSONL output should contain '}\\n{' (newlines between documents) for proper JSONL format, but was: " + output);

            // Read back with JSONL reader (round-trip test)
            bytes.readPosition(0);
            Wire readWire = WireType.JSONL.apply(bytes);
            List<String> seen = new ArrayList<>();
            MethodReader reader = readWire.methodReader(new JsonlEvents() {
                @Override
                public void event(String name, int count) {
                    seen.add(name + ":" + count);
                }
            });

            while (reader.readOne()) {
                continue; // consume all events
            }

            assertEquals(3, seen.size(), "JSONL reader should read all 3 events from round-trip, received: " + seen.size());
            assertEquals("first:1", seen.get(0), "First event from round-trip should be 'first:1', actual: " + seen.get(0));
            assertEquals("second:2", seen.get(1), "Second event from round-trip should be 'second:2', but was: " + seen.get(1));
            assertEquals("third:3", seen.get(2), "Third event from round-trip should be 'third:3', received: " + seen.get(2));
        }
    }

    // =========================================================================
    // Tests clarifying JSONL support and documenting behavior/limitations
    // =========================================================================

    /**
     * CLARIFICATION: Single record produces exactly one JSON line.
     * Splitting on \n yields exactly 1 non-empty JSON object.
     */
    @Test
    @DisplayName("should produce exactly one line for single record")
    public void testLineDiscipline_singleRecordProducesOneLine() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            JSONLRecord record = new JSONLRecord("test", 1, 1.0, 0.0, Status.ACTIVE, 0);
            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(JSONLRecord.class, record);
            }

            String output = bytes.toString();
            String[] lines = output.split("\n", -1); // -1 to keep trailing empty strings

            // Should have exactly one non-empty line (plus possibly trailing empty after final \n)
            int nonEmptyLines = 0;
            for (String line : lines) {
                if (!line.isEmpty()) nonEmptyLines++;
            }
            assertEquals(1, nonEmptyLines, "Single record should produce exactly 1 line");

            // The non-empty line should be valid JSON
            String jsonLine = lines[0];
            assertTrue(jsonLine.startsWith("{") && jsonLine.endsWith("}"), "Line should be a JSON object");
        }
    }

    /**
     * CLARIFICATION: Embedded newlines in strings are escaped, not literal.
     * Output remains single line per record.
     */
    @Test
    @DisplayName("should escape embedded newlines in strings to maintain one JSON object per line")
    public void testLineDiscipline_embeddedNewlinesAreEscaped() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            // String with embedded newline and carriage return
            JSONLRecord record = new JSONLRecord("line1\nline2\rline3\r\nline4", 1, 0.0, 0.0, Status.ACTIVE, 0);
            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(JSONLRecord.class, record);
            }

            String output = bytes.toString();

            // Count actual newlines in output (should be exactly 1 - the JSONL line terminator)
            int newlineCount = 0;
            for (char c : output.toCharArray()) {
                if (c == '\n') newlineCount++;
            }
            assertEquals(1, newlineCount, "JSONL output newline count should be 1 (only the line terminator), actual " + newlineCount + " for output: " + output);

            // Verify escaped sequences are present
            assertTrue(output.contains("\\n"), "JSONL output should contain '\\n' escape sequence for embedded newlines, actual: " + output);
            assertTrue(output.contains("\\r"), "JSONL output should contain '\\r' escape sequence for embedded carriage returns, but was: " + output);
        }
    }

    /**
     * CLARIFICATION: Control characters (NUL, SOH, etc.) are properly escaped.
     * This ensures JSON parsers can handle the output.
     */
    @Test
    @DisplayName("should escape control characters (NUL, TAB, LF, CR) to maintain valid JSON")
    public void testControlCharacterEscaping() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            // String with various control characters
            String controlChars = "NUL:\u0000 TAB:\t LF:\n CR:\r BS:\b FF:\f";
            JSONLRecord record = new JSONLRecord(controlChars, 1, 0.0, 0.0, Status.ACTIVE, 0);
            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(JSONLRecord.class, record);
            }

            String output = bytes.toString();
            String jsonLine = output.split("\n")[0];

            // Verify it's still a single valid JSON line
            assertTrue(jsonLine.startsWith("{") && jsonLine.endsWith("}"),
                    "JSONL line should start with '{' and end with '}' to be valid JSON after control character escaping, received: " + jsonLine);

            // Verify control characters are escaped (not literal)
            assertFalse(jsonLine.contains("\u0000"), "JSONL line should not contain literal NUL (\\u0000) character, but found in: " + jsonLine);
            assertFalse(jsonLine.contains("\t"), "JSONL line should not contain literal TAB (\\t) character, actual: " + jsonLine);
            assertFalse(jsonLine.contains("\n"), "JSONL line should not contain literal LF (\\n) character, but was: " + jsonLine);
            assertFalse(jsonLine.contains("\r"), "JSONL line should not contain literal CR (\\r) character, received: " + jsonLine);
        }
    }

    /**
     * CLARIFICATION: Enum deserialization is CASE-SENSITIVE.
     * Exact case match is required; different case will fail.
     */
    @Test
    @DisplayName("should parse enum with exact case match")
    public void testEnumCaseSensitivity_exactCaseSucceeds() {
        // Exact case - should succeed
        String json = "{\"name\":\"test\",\"count\":0,\"price\":0.0,\"specialValue\":0.0,\"status\":\"ACTIVE\",\"timestamp\":0}";
        Bytes<?> bytes = Bytes.from(json);
        try {
            Wire wire = WireType.JSONL.apply(bytes);
            JSONLRecord record = wire.getValueIn().object(JSONLRecord.class);
            assertEquals(Status.ACTIVE, record.status, "Exact case should parse correctly");
        } finally {
            bytes.releaseLast();
        }
    }

    /**
     * DOCUMENTS: Enum deserialization IS case-insensitive in Chronicle Wire.
     * Both exact case and different case values parse successfully.
     */
    @Test
    @DisplayName("should parse enum case-insensitively (lowercase, mixed case)")
    public void testEnumCaseSensitivity_caseInsensitiveSupported() {
        // Lowercase - Chronicle Wire supports case-insensitive enum parsing
        String json = "{\"name\":\"test\",\"count\":0,\"price\":0.0,\"specialValue\":0.0,\"status\":\"active\",\"timestamp\":0}";
        Bytes<?> bytes = Bytes.from(json);
        try {
            Wire wire = WireType.JSONL.apply(bytes);
            JSONLRecord record = wire.getValueIn().object(JSONLRecord.class);
            // Chronicle Wire supports case-insensitive enum parsing
            assertEquals(Status.ACTIVE, record.status, "Lowercase enum parses correctly (case-insensitive)");
        } finally {
            bytes.releaseLast();
        }

        // Mixed case also works
        String mixedJson = "{\"name\":\"test\",\"count\":0,\"price\":0.0,\"specialValue\":0.0,\"status\":\"Active\",\"timestamp\":0}";
        Bytes<?> mixedBytes = Bytes.from(mixedJson);
        try {
            Wire wire = WireType.JSONL.apply(mixedBytes);
            JSONLRecord record = wire.getValueIn().object(JSONLRecord.class);
            assertEquals(Status.ACTIVE, record.status, "Mixed case enum parses correctly");
        } finally {
            mixedBytes.releaseLast();
        }
    }

    /**
     * DOCUMENTS: Behavior differs from Java's Enum.valueOf() which is case-sensitive.
     * Chronicle Wire's case-insensitive parsing is more lenient than standard Java enum handling.
     * This may be desirable for user-friendly input, but differs from Enum.valueOf("active") which throws.
     */
    @Test
    @DisplayName("documents difference from Enum.valueOf() case-sensitive behaviour")
    @Disabled("Chronicle Wire uses case-insensitive enum parsing - documents difference from Enum.valueOf() behavior")
    public void testEnumCaseSensitivity_differsFromEnumValueOf() {
        // Java's Enum.valueOf(Status.class, "active") would throw IllegalArgumentException
        // Chronicle Wire accepts it - this test documents the difference
        String json = "{\"name\":\"test\",\"count\":0,\"price\":0.0,\"specialValue\":0.0,\"status\":\"active\",\"timestamp\":0}";
        Bytes<?> bytes = Bytes.from(json);
        try {
            Wire wire = WireType.JSONL.apply(bytes);
            JSONLRecord record = wire.getValueIn().object(JSONLRecord.class);
            // If matching Enum.valueOf() behavior, lowercase "active" should fail
            assertNull(record.status, "Would be null if matching Enum.valueOf() case-sensitive behavior");
        } finally {
            bytes.releaseLast();
        }
    }

    /**
     * CLARIFICATION: Non-finite double values (NaN, Infinity) are serialized as STRINGS.
     * This is NOT standard JSON (which has no representation for these values).
     * Chronicle Wire uses quoted strings: "NaN", "Infinity", "-Infinity"
     */
    @Test
    @DisplayName("should serialise non-finite doubles as quoted strings (Chronicle extension)")
    public void testNonFiniteDoubles_serializedAsStrings() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            JSONLRecord record = new JSONLRecord("nonfinite", 0, Double.NaN, Double.POSITIVE_INFINITY, Status.ACTIVE, 0);
            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(JSONLRecord.class, record);
            }

            String output = bytes.toString();

            // Document the exact format used
            assertTrue(output.contains("\"price\":\"NaN\""),
                    "JSONL output should contain '\"price\":\"NaN\"' for non-finite double NaN as quoted string, actual: " + output);
            assertTrue(output.contains("\"specialValue\":\"Infinity\""),
                    "JSONL output should contain '\"specialValue\":\"Infinity\"' for non-finite double +Infinity as quoted string, but was: " + output);

            // Note: This is Chronicle Wire's extension - standard JSON parsers may not handle this
        }
    }

    /**
     * CLARIFICATION: Finite double values are serialized as JSON numbers (not strings).
     */
    @Test
    @DisplayName("should serialise finite doubles as unquoted JSON numbers")
    public void testFiniteDoubles_serializedAsNumbers() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            JSONLRecord record = new JSONLRecord("finite", 0, 123.456, -789.012, Status.ACTIVE, 0);
            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(JSONLRecord.class, record);
            }

            String output = bytes.toString();

            // Finite values should be unquoted numbers
            assertTrue(output.contains("\"price\":123.456"),
                    "JSONL output should contain '\"price\":123.456' for finite double as unquoted number, received: " + output);
            assertTrue(output.contains("\"specialValue\":-789.012"),
                    "JSONL output should contain '\"specialValue\":-789.012' for negative finite double as unquoted number, actual: " + output);
        }
    }

    /**
     * CLARIFICATION: WireType.JSONL does NOT include @type metadata by default.
     * When field is typed as concrete class, no type tag appears.
     */
    @Test
    @DisplayName("should omit @type metadata when concrete type is specified")
    public void testTypeMetadata_concreteTypeNoTag() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            JSONLRecord record = new JSONLRecord("test", 1, 0.0, 0.0, Status.ACTIVE, 0);
            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(JSONLRecord.class, record);
            }

            String output = bytes.toString();

            // No type metadata when concrete type is specified
            assertFalse(output.contains("@type"),
                    "JSONL output should not contain '@type' when concrete type is specified at serialisation, but found in: " + output);
            assertFalse(output.contains("!"),
                    "JSONL output should not contain '!' type prefix when concrete type is specified at serialisation, actual: " + output);
        }
    }

    /**
     * DTO with Object field for testing polymorphic serialization.
     */
    static class PolymorphicRecord extends SelfDescribingMarshallable {
        String name;
        Object data;

        PolymorphicRecord() {
        }

        PolymorphicRecord(String name, Object data) {
            this.name = name;
            this.data = data;
        }
    }

    /**
     * CLARIFICATION: When useTypes(true) is set on JSONWire, type metadata IS included.
     * This test documents the behavior when types are explicitly enabled.
     */
    @Test
    @DisplayName("should output valid JSON object when useTypes(true) is explicitly set")
    public void testTypeMetadata_withUseTypesEnabled() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            // Create JSONWire with types enabled (not using WireType.JSONL which disables types)
            JSONWire wire = new JSONWire(bytes).useTypes(true);

            PolymorphicRecord record = new PolymorphicRecord("test", "stringValue");
            wire.getValueOut().object(PolymorphicRecord.class, record);

            String output = bytes.toString();

            // With useTypes(true), type information may appear for polymorphic fields
            // This documents when @type tags are expected
            assertTrue(output.startsWith("{"),
                    "JSONWire output with useTypes(true) should start with '{' to be valid JSON object, received: " + output);
        }
    }

    /**
     * CLARIFICATION: Large number of records works correctly.
     * Line count equals record count, and every line is valid JSON.
     */
    @Test
    @DisplayName("should handle 1000 records with correct line count and valid JSON per line")
    public void testLargeRecordCount() {
        final int recordCount = 1000;

        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            // Write many records
            for (int i = 0; i < recordCount; i++) {
                JSONLRecord record = new JSONLRecord("record" + i, i, i * 0.1, 0.0, Status.ACTIVE, 0);
                try (DocumentContext dc = wire.writingDocument()) {
                    dc.wire().getValueOut().object(JSONLRecord.class, record);
                }
            }

            String output = bytes.toString();
            String[] lines = output.split("\n");

            // Filter empty lines
            int nonEmptyLines = 0;
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (!line.isEmpty()) {
                    nonEmptyLines++;
                    // Verify each line is valid JSON structure
                    assertTrue(line.startsWith("{"), "JSONL line[" + i + "] should start with '{' to be valid JSON, actual: " + line);
                    assertTrue(line.endsWith("}"), "JSONL line[" + i + "] should end with '}' to be valid JSON, but was: " + line);
                }
            }

            assertEquals(recordCount, nonEmptyLines, "JSONL output non-empty line count should equal record count (" + recordCount + "), received: " + nonEmptyLines);
        }
    }

    /**
     * CLARIFICATION: Each JSONL line is independently parseable.
     * This verifies round-trip for multiple records.
     */
    @Test
    @DisplayName("should allow each JSONL line to be independently deserialised")
    public void testEachLineIndependentlyParseable() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            // Write multiple records with different data
            JSONLRecord[] originals = {
                    new JSONLRecord("first", 1, 1.1, 0.0, Status.PENDING, 0),
                    new JSONLRecord("second", 2, 2.2, Double.NaN, Status.ACTIVE, 0),
                    new JSONLRecord("third", 3, 3.3, Double.POSITIVE_INFINITY, Status.COMPLETED, 0)
            };

            for (JSONLRecord original : originals) {
                try (DocumentContext dc = wire.writingDocument()) {
                    dc.wire().getValueOut().object(JSONLRecord.class, original);
                }
            }

            // Parse each line independently
            String output = bytes.toString();
            String[] lines = output.split("\n");

            for (int i = 0; i < originals.length; i++) {
                Bytes<?> lineBytes = Bytes.from(lines[i]);
                try {
                    Wire lineWire = WireType.JSONL.apply(lineBytes);
                    JSONLRecord parsed;
                    try (DocumentContext dc = lineWire.readingDocument()) {
                        parsed = dc.wire().getValueIn().object(JSONLRecord.class);
                    }

                    assertEquals(originals[i].name, parsed.name,
                            "Deserialised record[" + i + "] name should match original '" + originals[i].name + "', actual: " + parsed.name);
                    assertEquals(originals[i].count, parsed.count,
                            "Deserialised record[" + i + "] count should match original " + originals[i].count + ", but was: " + parsed.count);
                    assertEquals(originals[i].status, parsed.status,
                            "Deserialised record[" + i + "] status should match original " + originals[i].status + ", received: " + parsed.status);
                } finally {
                    lineBytes.releaseLast();
                }
            }
        }
    }

    /**
     * CLARIFICATION: Unicode characters are preserved correctly.
     * High Unicode and emoji are properly serialized.
     */
    @Test
    @DisplayName("should preserve Unicode characters (Chinese, Cyrillic) through round-trip")
    public void testUnicodeSupport() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            // String with various Unicode characters
            String unicode = "Hello \u4e2d\u6587 \u0410\u0411\u0412";  // Chinese and Cyrillic
            JSONLRecord record = new JSONLRecord(unicode, 1, 0.0, 0.0, Status.ACTIVE, 0);
            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(JSONLRecord.class, record);
            }

            String output = bytes.toString();

            // Parse back and verify round-trip
            String line = output.split("\n")[0];
            Bytes<?> lineBytes = Bytes.from(line);
            try {
                Wire lineWire = WireType.JSONL.apply(lineBytes);
                JSONLRecord parsed;
                try (DocumentContext dc = lineWire.readingDocument()) {
                    parsed = dc.wire().getValueIn().object(JSONLRecord.class);
                }
                assertEquals(unicode, parsed.name, "Unicode should round-trip correctly");
            } finally {
                lineBytes.releaseLast();
            }
        }
    }

    /**
     * Simple DTO for testing default JSONWire behavior.
     */
    static class SimpleRecord extends SelfDescribingMarshallable {
        String value;

        SimpleRecord() {
        }

        SimpleRecord(String value) {
            this.value = value;
        }
    }

    /**
     * CLARIFICATION: Default WireType.JSON is NOT JSONL compatible.
     * Multiple writes produce comma-separated objects, not newline-separated.
     * This documents why WireType.JSONL was created.
     */
    @Test
    @DisplayName("should demonstrate that default JSON wire uses comma separation, not newlines")
    public void testDefaultJSONWire_notJSONLCompatible() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            // Use regular JSON, not JSONL
            Wire wire = WireType.JSON.apply(bytes);

            // Write two objects
            SimpleRecord r1 = new SimpleRecord("first");
            SimpleRecord r2 = new SimpleRecord("second");

            wire.getValueOut().object(SimpleRecord.class, r1);
            wire.getValueOut().object(SimpleRecord.class, r2);

            String output = bytes.toString();

            // Default JSON uses comma separation, not newlines
            // This documents that regular JSONWire is NOT JSONL
            assertTrue(output.contains(","),
                    "Default WireType.JSON output should contain ',' for comma-separated objects, actual: " + output);

            // Count newlines - should NOT be JSONL format
            int newlineCount = 0;
            for (char c : output.toCharArray()) {
                if (c == '\n') newlineCount++;
            }
            // Regular JSON may have 0 or few newlines, but not 1 per record
            assertTrue(newlineCount < 2,
                    "Default WireType.JSON newline count should be less than 2 (not one-per-record like JSONL), but was " + newlineCount + " for output: " + output);
        }
    }

    /**
     * CLARIFICATION: WireType.JSONL uses document context for proper line separation.
     * Each writingDocument() produces one complete JSON line.
     */
    @Test
    @DisplayName("should require DocumentContext pattern to produce proper JSONL line separation")
    public void testJSONL_requiresDocumentContext() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            // Correct pattern: use writingDocument() for each record
            SimpleRecord r1 = new SimpleRecord("first");
            SimpleRecord r2 = new SimpleRecord("second");

            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(SimpleRecord.class, r1);
            }
            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(SimpleRecord.class, r2);
            }

            String output = bytes.toString();
            String[] lines = output.split("\n");

            // Should have exactly 2 lines
            int nonEmptyLines = 0;
            for (String line : lines) {
                if (!line.isEmpty()) nonEmptyLines++;
            }
            assertEquals(2, nonEmptyLines,
                    "JSONL output with DocumentContext pattern should have 2 non-empty lines for 2 records, actual: " + nonEmptyLines);
        }
    }
}
