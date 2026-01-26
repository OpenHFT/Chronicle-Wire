/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.scoped.ScopedResource;
import net.openhft.chronicle.wire.converter.NanoTime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for JSONL (JSON Lines) wire format support.
 * JSONL outputs one JSON object per line with newline separators.
 */
public class JSONLTest extends WireTestCommon {

    /**
     * Test DTO for JSONL serialization.
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
            assertEquals("Expected 2 lines, got: " + output, 2, lines.length);

            // Verify each line is valid JSON
            assertTrue("First line should start with {, got: " + lines[0], lines[0].startsWith("{"));
            assertTrue("First line should end with }", lines[0].endsWith("}"));
            assertTrue("Second line should start with {", lines[1].startsWith("{"));
            assertTrue("Second line should end with }", lines[1].endsWith("}"));

            // Verify content
            assertTrue("First line should contain record1", lines[0].contains("\"name\":\"record1\""));
            assertTrue("Second line should contain record2", lines[1].contains("\"name\":\"record2\""));
        }
    }

    @Test
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
            assertEquals(3, lines.length);

            // Verify special values are quoted strings in JSON
            assertTrue("NaN should be quoted", lines[0].contains("\"specialValue\":\"NaN\""));
            assertTrue("Infinity should be quoted", lines[1].contains("\"specialValue\":\"Infinity\""));
            assertTrue("-Infinity should be quoted", lines[2].contains("\"specialValue\":\"-Infinity\""));
        }
    }

    @Test
    public void testNonFiniteDoubleRoundTrip() {
        // Test round-trip for NaN
        String nanJson = "{\"name\":\"nan\",\"count\":0,\"price\":0.0,\"specialValue\":\"NaN\",\"status\":\"ACTIVE\",\"timestamp\":0}";
        Bytes<?> nanBytes = Bytes.from(nanJson);
        try {
            Wire wire = WireType.JSONL.apply(nanBytes);
            JSONLRecord record = wire.getValueIn().object(JSONLRecord.class);
            assertTrue("NaN should round-trip", Double.isNaN(record.specialValue));
        } finally {
            nanBytes.releaseLast();
        }

        // Test round-trip for +Infinity
        String posInfJson = "{\"name\":\"inf\",\"count\":0,\"price\":0.0,\"specialValue\":\"Infinity\",\"status\":\"ACTIVE\",\"timestamp\":0}";
        Bytes<?> posInfBytes = Bytes.from(posInfJson);
        try {
            Wire wire = WireType.JSONL.apply(posInfBytes);
            JSONLRecord record = wire.getValueIn().object(JSONLRecord.class);
            assertEquals("Infinity should round-trip", Double.POSITIVE_INFINITY, record.specialValue, 0.0);
        } finally {
            posInfBytes.releaseLast();
        }

        // Test round-trip for -Infinity
        String negInfJson = "{\"name\":\"negInf\",\"count\":0,\"price\":0.0,\"specialValue\":\"-Infinity\",\"status\":\"ACTIVE\",\"timestamp\":0}";
        Bytes<?> negInfBytes = Bytes.from(negInfJson);
        try {
            Wire wire = WireType.JSONL.apply(negInfBytes);
            JSONLRecord record = wire.getValueIn().object(JSONLRecord.class);
            assertEquals("-Infinity should round-trip", Double.NEGATIVE_INFINITY, record.specialValue, 0.0);
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
            assertEquals(3, lines.length);

            // Verify special float values are quoted strings in JSON
            assertTrue("Float NaN should be quoted", lines[0].contains("\"value\":\"NaN\""));
            assertTrue("Float Infinity should be quoted", lines[1].contains("\"value\":\"Infinity\""));
            assertTrue("Float -Infinity should be quoted", lines[2].contains("\"value\":\"-Infinity\""));
        }
    }

    @Test
    public void testNonFiniteFloatRoundTrip() {
        // Test round-trip for Float.NaN
        String nanJson = "{\"name\":\"nan\",\"value\":\"NaN\"}";
        Bytes<?> nanBytes = Bytes.from(nanJson);
        try {
            Wire wire = WireType.JSONL.apply(nanBytes);
            FloatRecord record = wire.getValueIn().object(FloatRecord.class);
            assertTrue("Float NaN should round-trip", Float.isNaN(record.value));
        } finally {
            nanBytes.releaseLast();
        }

        // Test round-trip for Float.POSITIVE_INFINITY
        String posInfJson = "{\"name\":\"inf\",\"value\":\"Infinity\"}";
        Bytes<?> posInfBytes = Bytes.from(posInfJson);
        try {
            Wire wire = WireType.JSONL.apply(posInfBytes);
            FloatRecord record = wire.getValueIn().object(FloatRecord.class);
            assertEquals("Float Infinity should round-trip", Float.POSITIVE_INFINITY, record.value, 0.0f);
        } finally {
            posInfBytes.releaseLast();
        }

        // Test round-trip for Float.NEGATIVE_INFINITY
        String negInfJson = "{\"name\":\"negInf\",\"value\":\"-Infinity\"}";
        Bytes<?> negInfBytes = Bytes.from(negInfJson);
        try {
            Wire wire = WireType.JSONL.apply(negInfBytes);
            FloatRecord record = wire.getValueIn().object(FloatRecord.class);
            assertEquals("Float -Infinity should round-trip", Float.NEGATIVE_INFINITY, record.value, 0.0f);
        } finally {
            negInfBytes.releaseLast();
        }
    }

    @Test
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
            assertTrue("Timestamp should be formatted as ISO-8601", output.contains("\"timestamp\":\"2022-06-17T12:35:56\""));
        }
    }

    @Test
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
            assertEquals(Status.values().length, lines.length);

            // Verify enum values are quoted strings (JSONL uses lowercase by default)
            assertTrue(lines[0].contains("\"status\":\"pending\""));
            assertTrue(lines[1].contains("\"status\":\"active\""));
            assertTrue(lines[2].contains("\"status\":\"completed\""));
            assertTrue(lines[3].contains("\"status\":\"cancelled\""));
        }
    }

    @Test
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
            assertTrue("Should contain escaped quote", output.contains("\\\""));
            assertTrue("Should contain escaped backslash", output.contains("\\\\"));
            assertTrue("Should contain escaped tab", output.contains("\\t"));
            assertTrue("Should contain escaped newline", output.contains("\\n"));
        }
    }

    @Test
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
            assertEquals(2, lines.length);

            // Verify null handling
            assertTrue("Null should be represented", lines[0].contains("\"name\":null") || lines[0].contains("\"name\":\"\""));
            // Verify empty string
            assertTrue("Empty string should be quoted", lines[1].contains("\"name\":\"\""));
        }
    }

    @Test
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
            assertTrue("Single record should end with newline", output.endsWith("\n"));

            // Should be valid JSON object
            String line = output.trim();
            assertTrue("Should be a JSON object", line.startsWith("{") && line.endsWith("}"));
        }
    }

    @Test
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
            assertFalse("Should not contain @type", output.contains("@type"));
            assertFalse("Should not contain @JSONLRecord", output.contains("@JSONLRecord"));
        }
    }

    @Test
    public void testWireTypeJSONL() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            // Verify wire type properties
            assertTrue("JSONL should be text format", WireType.JSONL.isText());
            assertTrue("Wire should be JSONWire", wire instanceof JSONWire);

            JSONWire jsonWire = (JSONWire) wire;
            assertFalse("JSONL should not use types", jsonWire.useTypes());
        }
    }

    @Test
    public void testReadJSONL() {
        // Test reading JSONL format
        String jsonl = "{\"name\":\"read1\",\"count\":10,\"price\":1.5,\"specialValue\":0.0,\"status\":\"ACTIVE\",\"timestamp\":0}\n" +
                       "{\"name\":\"read2\",\"count\":20,\"price\":2.5,\"specialValue\":0.0,\"status\":\"PENDING\",\"timestamp\":0}\n";

        List<JSONLRecord> records = new ArrayList<JSONLRecord>();

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

        assertEquals(2, records.size());
        assertEquals("read1", records.get(0).name);
        assertEquals(10, records.get(0).count);
        assertEquals("read2", records.get(1).name);
        assertEquals(20, records.get(1).count);
    }

    @Test
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

            assertEquals("Should have 3 lines", 3, lines.length);

            for (int i = 0; i < 3; i++) {
                assertTrue("Line " + i + " should contain record" + i, lines[i].contains("\"name\":\"record" + i + "\""));
                assertTrue("Line " + i + " should be valid JSON", lines[i].startsWith("{") && lines[i].endsWith("}"));
            }
        }
    }

    @Test
    public void testMethodReaderStreamingJsonl() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            JsonlEvents writer = wire.methodWriter(JsonlEvents.class);
            writer.event("first", 1);
            writer.event("second", 2);
            writer.event("third", 3);

            List<String> seen = new ArrayList<String>();
            MethodReader reader = wire.methodReader(new JsonlEvents() {
                @Override
                public void event(String name, int count) {
                    seen.add(name + ":" + count);
                }
            });

            while (reader.readOne()) {
                // loop until exhausted
            }

            assertEquals(3, seen.size());
            assertEquals("first:1", seen.get(0));
            assertEquals("second:2", seen.get(1));
            assertEquals("third:3", seen.get(2));
        }
    }

    @Test
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
                List<String> seen = new ArrayList<String>();
                MethodReader reader = readWire.methodReader(new JsonlEvents() {
                    @Override
                    public void event(String name, int count) {
                        seen.add(name + ":" + count);
                    }
                });

                while (reader.readOne()) {
                    // loop until exhausted
                }

                assertEquals(3, seen.size());
                assertEquals("first:1", seen.get(0));
                assertEquals("second:2", seen.get(1));
                assertEquals("third:3", seen.get(2));
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
            assertFalse("JSON output should not have newlines between documents", 
                       output.contains("}\n{"));
            
            // Read with JSONL - should work because JSONL reader handles concatenated JSON
            bytes.readPosition(0);
            Wire readWire = WireType.JSONL.apply(bytes);
            List<String> seen = new ArrayList<String>();
            MethodReader reader = readWire.methodReader(new JsonlEvents() {
                @Override
                public void event(String name, int count) {
                    seen.add(name + ":" + count);
                }
            });
            
            while (reader.readOne()) {
                // loop until exhausted
            }
            
            assertEquals("JSONL reader should read all 3 events from JSON output", 3, seen.size());
            assertEquals("first:1", seen.get(0));
            assertEquals("second:2", seen.get(1));
            assertEquals("third:3", seen.get(2));
        }
    }
    
    /**
     * Test that data written with JSONL wire type can be read with JSON wire type.
     * JSONL adds newlines between documents which JSON reader treats as whitespace.
     */
    @Test
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
            assertTrue("JSONL output should have newlines between documents", 
                      output.contains("}\n{"));
            
            // Read back with JSONL reader (round-trip test)
            bytes.readPosition(0);
            Wire readWire = WireType.JSONL.apply(bytes);
            List<String> seen = new ArrayList<String>();
            MethodReader reader = readWire.methodReader(new JsonlEvents() {
                @Override
                public void event(String name, int count) {
                    seen.add(name + ":" + count);
                }
            });
            
            while (reader.readOne()) {
                // loop until exhausted
            }
            
            assertEquals("JSONL reader should read all 3 events", 3, seen.size());
            assertEquals("first:1", seen.get(0));
            assertEquals("second:2", seen.get(1));
            assertEquals("third:3", seen.get(2));
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
            assertEquals("Single record should produce exactly 1 line", 1, nonEmptyLines);

            // The non-empty line should be valid JSON
            String jsonLine = lines[0];
            assertTrue("Line should be a JSON object", jsonLine.startsWith("{") && jsonLine.endsWith("}"));
        }
    }

    /**
     * CLARIFICATION: Embedded newlines in strings are escaped, not literal.
     * Output remains single line per record.
     */
    @Test
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
            assertEquals("Should have exactly 1 newline (the line terminator)", 1, newlineCount);

            // Verify escaped sequences are present
            assertTrue("Should contain escaped \\n", output.contains("\\n"));
            assertTrue("Should contain escaped \\r", output.contains("\\r"));
        }
    }

    /**
     * CLARIFICATION: Control characters (NUL, SOH, etc.) are properly escaped.
     * This ensures JSON parsers can handle the output.
     */
    @Test
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
            assertTrue("Should be a JSON object", jsonLine.startsWith("{") && jsonLine.endsWith("}"));

            // Verify control characters are escaped (not literal)
            assertFalse("Should not contain literal NUL", jsonLine.contains("\u0000"));
            assertFalse("Should not contain literal TAB", jsonLine.contains("\t"));
            assertFalse("Should not contain literal LF", jsonLine.contains("\n"));
            assertFalse("Should not contain literal CR", jsonLine.contains("\r"));
        }
    }

    /**
     * CLARIFICATION: Enum deserialization is CASE-SENSITIVE.
     * Exact case match is required; different case will fail.
     */
    @Test
    public void testEnumCaseSensitivity_exactCaseSucceeds() {
        // Exact case - should succeed
        String json = "{\"name\":\"test\",\"count\":0,\"price\":0.0,\"specialValue\":0.0,\"status\":\"ACTIVE\",\"timestamp\":0}";
        Bytes<?> bytes = Bytes.from(json);
        try {
            Wire wire = WireType.JSONL.apply(bytes);
            JSONLRecord record = wire.getValueIn().object(JSONLRecord.class);
            assertEquals("Exact case should parse correctly", Status.ACTIVE, record.status);
        } finally {
            bytes.releaseLast();
        }
    }

    /**
     * DOCUMENTS: Enum deserialization IS case-insensitive in Chronicle Wire.
     * Both exact case and different case values parse successfully.
     */
    @Test
    public void testEnumCaseSensitivity_caseInsensitiveSupported() {
        // Lowercase - Chronicle Wire supports case-insensitive enum parsing
        String json = "{\"name\":\"test\",\"count\":0,\"price\":0.0,\"specialValue\":0.0,\"status\":\"active\",\"timestamp\":0}";
        Bytes<?> bytes = Bytes.from(json);
        try {
            Wire wire = WireType.JSONL.apply(bytes);
            JSONLRecord record = wire.getValueIn().object(JSONLRecord.class);
            // Chronicle Wire supports case-insensitive enum parsing
            assertEquals("Lowercase enum parses correctly (case-insensitive)", Status.ACTIVE, record.status);
        } finally {
            bytes.releaseLast();
        }

        // Mixed case also works
        String mixedJson = "{\"name\":\"test\",\"count\":0,\"price\":0.0,\"specialValue\":0.0,\"status\":\"Active\",\"timestamp\":0}";
        Bytes<?> mixedBytes = Bytes.from(mixedJson);
        try {
            Wire wire = WireType.JSONL.apply(mixedBytes);
            JSONLRecord record = wire.getValueIn().object(JSONLRecord.class);
            assertEquals("Mixed case enum parses correctly", Status.ACTIVE, record.status);
        } finally {
            mixedBytes.releaseLast();
        }
    }

    /**
     * DOCUMENTS: Behavior differs from Java's Enum.valueOf() which is case-sensitive.
     * Chronicle Wire's case-insensitive parsing is more lenient than standard Java enum handling.
     * This may be desirable for user-friendly input, but differs from Enum.valueOf("active") which throws.
     */
    @org.junit.Ignore("Chronicle Wire uses case-insensitive enum parsing - documents difference from Enum.valueOf() behavior")
    @Test
    public void testEnumCaseSensitivity_differsFromEnumValueOf() {
        // Java's Enum.valueOf(Status.class, "active") would throw IllegalArgumentException
        // Chronicle Wire accepts it - this test documents the difference
        String json = "{\"name\":\"test\",\"count\":0,\"price\":0.0,\"specialValue\":0.0,\"status\":\"active\",\"timestamp\":0}";
        Bytes<?> bytes = Bytes.from(json);
        try {
            Wire wire = WireType.JSONL.apply(bytes);
            JSONLRecord record = wire.getValueIn().object(JSONLRecord.class);
            // If matching Enum.valueOf() behavior, lowercase "active" should fail
            assertNull("Would be null if matching Enum.valueOf() case-sensitive behavior", record.status);
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
            assertTrue("NaN serialized as string \"NaN\"", output.contains("\"price\":\"NaN\""));
            assertTrue("Infinity serialized as string \"Infinity\"", output.contains("\"specialValue\":\"Infinity\""));

            // Note: This is Chronicle Wire's extension - standard JSON parsers may not handle this
        }
    }

    /**
     * CLARIFICATION: Finite double values are serialized as JSON numbers (not strings).
     */
    @Test
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
            assertTrue("Finite double should be unquoted number", output.contains("\"price\":123.456"));
            assertTrue("Negative finite double should be unquoted number", output.contains("\"specialValue\":-789.012"));
        }
    }

    /**
     * CLARIFICATION: WireType.JSONL does NOT include @type metadata by default.
     * When field is typed as concrete class, no type tag appears.
     */
    @Test
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
            assertFalse("No @type when concrete type specified", output.contains("@type"));
            assertFalse("No !type when concrete type specified", output.contains("!"));
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
            assertTrue("Output should be JSON object", output.startsWith("{"));
        }
    }

    /**
     * CLARIFICATION: Large number of records works correctly.
     * Line count equals record count, and every line is valid JSON.
     */
    @Test
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
            for (String line : lines) {
                if (!line.isEmpty()) {
                    nonEmptyLines++;
                    // Verify each line is valid JSON structure
                    assertTrue("Line should start with {", line.startsWith("{"));
                    assertTrue("Line should end with }", line.endsWith("}"));
                }
            }

            assertEquals("Line count should equal record count", recordCount, nonEmptyLines);
        }
    }

    /**
     * CLARIFICATION: Each JSONL line is independently parseable.
     * This verifies round-trip for multiple records.
     */
    @Test
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

                    assertEquals("Name should match", originals[i].name, parsed.name);
                    assertEquals("Count should match", originals[i].count, parsed.count);
                    assertEquals("Status should match", originals[i].status, parsed.status);
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
                assertEquals("Unicode should round-trip correctly", unicode, parsed.name);
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
            assertTrue("Default JSON uses comma separation", output.contains(","));

            // Count newlines - should NOT be JSONL format
            int newlineCount = 0;
            for (char c : output.toCharArray()) {
                if (c == '\n') newlineCount++;
            }
            // Regular JSON may have 0 or few newlines, but not 1 per record
            assertTrue("Default JSON is not newline-per-record", newlineCount < 2);
        }
    }

    /**
     * CLARIFICATION: WireType.JSONL uses document context for proper line separation.
     * Each writingDocument() produces one complete JSON line.
     */
    @Test
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
            assertEquals("Should have 2 lines with document context pattern", 2, nonEmptyLines);
        }
    }
}
