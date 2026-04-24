/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.scoped.ScopedResource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional coverage tests for JSONL (JSON Lines) wire format.
 * <p>
 * These tests target specific code paths in {@link JSONWire}'s JSONL support
 * that are not exercised by {@link JSONLTest}, including:
 * <ul>
 *   <li>Empty and non-marshallable document write paths</li>
 *   <li>Nested object depth tracking in {@code findDocumentBounds()}</li>
 *   <li>Structural characters inside string values</li>
 *   <li>CRLF and mixed delimiter handling</li>
 *   <li>Multi-document streaming reads via {@code readingDocument()}</li>
 *   <li>{@code WireType.JSONL.asString()} convenience method</li>
 * </ul>
 */
@DisplayName("JSONL Coverage: write paths, parser edge cases, and delimiter handling")
public class JSONLCoverageTest extends WireTestCommon {

    // =========================================================================
    // DTOs
    // =========================================================================

    static class Flat extends SelfDescribingMarshallable {
        String name;
        int count;

        Flat() {
        }

        Flat(String name, int count) {
            this.name = name;
            this.count = count;
        }
    }

    static class Nested extends SelfDescribingMarshallable {
        String id;
        Flat child;

        Nested() {
        }

        Nested(String id, Flat child) {
            this.id = id;
            this.child = child;
        }
    }

    static class WithList extends SelfDescribingMarshallable {
        String label;
        List<Integer> values;

        WithList() {
        }

        WithList(String label, List<Integer> values) {
            this.label = label;
            this.values = values;
        }
    }

    interface Events {
        void event(String name, int count);
    }

    interface Events2 {
        void other(String msg);
    }

    // =========================================================================
    // Write-path coverage
    // =========================================================================

    @Test
    @DisplayName("empty writingDocument should produce no output")
    public void emptyDocumentProducesNothing() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            // Open and close without writing anything
            try (DocumentContext dc = wire.writingDocument()) {
                assertNotNull(dc);
            }

            String output = bytes.toString();
            assertEquals("", output, "Empty document should produce no output");
        }
    }

    @Test
    @DisplayName("non-marshallable value should be wrapped in braces with newline")
    public void nonMarshallableValueWrappedInBraces() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().write("key").text("value");
            }

            String output = bytes.toString();
            assertTrue(output.endsWith("\n"), "Output should end with newline");
            String line = output.trim();
            assertTrue(line.startsWith("{"), "Non-marshallable output should start with '{'");
            assertTrue(line.endsWith("}"), "Non-marshallable output should end with '}'");
            assertTrue(line.contains("\"key\":\"value\""), "Output should contain the key-value pair");
        }
    }

    @Test
    @DisplayName("scalar int value in document should be wrapped in braces")
    public void scalarIntValueWrappedInBraces() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().write("count").int32(42);
            }

            String output = bytes.toString();
            String line = output.trim();
            assertTrue(line.startsWith("{") && line.endsWith("}"),
                    "Scalar value should be wrapped in braces: " + line);
            assertTrue(line.contains("\"count\":42"), "Output should contain count: " + line);
        }
    }

    // =========================================================================
    // Nested object and depth tracking
    // =========================================================================

    @Test
    @DisplayName("nested marshallable should serialise and round-trip correctly")
    public void nestedMarshallableRoundTrip() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            Flat child = new Flat("inner", 99);
            Nested original = new Nested("outer", child);

            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(Nested.class, original);
            }

            String output = bytes.toString();
            String[] lines = output.split("\n");
            assertEquals(1, lines.length, "Nested object should still be one line: " + output);
            assertTrue(lines[0].startsWith("{") && lines[0].endsWith("}"),
                    "Line should be a valid JSON object: " + lines[0]);

            // Round-trip
            Bytes<?> readBytes = Bytes.from(lines[0]);
            try {
                Wire readWire = WireType.JSONL.apply(readBytes);
                Nested parsed = readWire.getValueIn().object(Nested.class);
                assertEquals("outer", parsed.id);
                assertNotNull(parsed.child);
                assertEquals("inner", parsed.child.name);
                assertEquals(99, parsed.child.count);
            } finally {
                readBytes.releaseLast();
            }
        }
    }

    @Test
    @DisplayName("list field should serialise with array brackets and round-trip")
    public void listFieldRoundTrip() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            List<Integer> vals = new ArrayList<>();
            vals.add(10);
            vals.add(20);
            vals.add(30);
            WithList original = new WithList("nums", vals);

            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(WithList.class, original);
            }

            String output = bytes.toString();
            String line = output.trim();
            assertTrue(line.contains("["), "Output should contain array bracket: " + line);

            // Round-trip
            Bytes<?> readBytes = Bytes.from(line);
            try {
                Wire readWire = WireType.JSONL.apply(readBytes);
                WithList parsed = readWire.getValueIn().object(WithList.class);
                assertEquals("nums", parsed.label);
                assertEquals(3, parsed.values.size());
                assertEquals(10, (int) parsed.values.get(0));
                assertEquals(20, (int) parsed.values.get(1));
                assertEquals(30, (int) parsed.values.get(2));
            } finally {
                readBytes.releaseLast();
            }
        }
    }

    // =========================================================================
    // Structural characters inside string values
    // =========================================================================

    @Test
    @DisplayName("braces and brackets inside string values should not break parsing")
    public void structuralCharsInStringValues() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            Flat record = new Flat("{key: [1,2]}", 1);
            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(Flat.class, record);
            }

            String output = bytes.toString();
            String[] lines = output.split("\n");
            assertEquals(1, lines.length, "Should be one line despite structural chars in value: " + output);

            // Round-trip
            Bytes<?> readBytes = Bytes.from(lines[0]);
            try {
                Wire readWire = WireType.JSONL.apply(readBytes);
                Flat parsed = readWire.getValueIn().object(Flat.class);
                assertEquals("{key: [1,2]}", parsed.name);
                assertEquals(1, parsed.count);
            } finally {
                readBytes.releaseLast();
            }
        }
    }

    @Test
    @DisplayName("commas and newlines inside string values should not split documents")
    public void commasAndNewlinesInStringValues() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            // String with comma and escaped newline
            Flat record = new Flat("a,b\nc", 2);
            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(Flat.class, record);
            }

            String output = bytes.toString();
            // Count non-empty lines - should be 1 because \n in string is escaped
            int nonEmpty = 0;
            for (String line : output.split("\n")) {
                if (!line.isEmpty()) nonEmpty++;
            }
            assertEquals(1, nonEmpty, "Embedded comma/newline in string should not split the document: " + output);
        }
    }

    @Test
    @DisplayName("escaped quotes inside string values should round-trip correctly")
    public void escapedQuotesInStringRoundTrip() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            String value = "he said \"hello\" and \"goodbye\"";
            Flat record = new Flat(value, 1);
            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().getValueOut().object(Flat.class, record);
            }

            String output = bytes.toString();
            String line = output.trim();
            assertTrue(line.startsWith("{") && line.endsWith("}"),
                    "Should be valid JSON despite escaped quotes: " + line);

            // Round-trip
            Bytes<?> readBytes = Bytes.from(line);
            try {
                Wire readWire = WireType.JSONL.apply(readBytes);
                Flat parsed = readWire.getValueIn().object(Flat.class);
                assertEquals(value, parsed.name);
            } finally {
                readBytes.releaseLast();
            }
        }
    }

    // =========================================================================
    // Delimiter handling (CRLF, leading whitespace, mixed)
    // =========================================================================

    @Test
    @DisplayName("should parse CRLF-separated JSONL input")
    public void crlfSeparatedInput() {
        String jsonl = "{\"name\":\"first\",\"count\":1}\r\n{\"name\":\"second\",\"count\":2}\r\n";
        Bytes<?> bytes = Bytes.from(jsonl);
        try {
            Wire wire = WireType.JSONL.apply(bytes);

            List<Flat> records = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                try (DocumentContext dc = wire.readingDocument()) {
                    assertTrue(dc.isPresent(), "Document " + i + " should be present");
                    Flat record = new Flat();
                    record.readMarshallable(dc.wire());
                    records.add(record);
                }
            }

            assertEquals(2, records.size());
            assertEquals("first", records.get(0).name);
            assertEquals("second", records.get(1).name);
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("should parse top-level scalar documents delimited by comma and CRLF")
    public void topLevelScalarDocumentsWithMixedDelimiters() {
        String jsonl = "123,\r\n456,\r789";
        Bytes<?> bytes = Bytes.from(jsonl);
        try {
            Wire wire = WireType.JSONL.apply(bytes);

            int[] expected = {123, 456, 789};
            for (int i = 0; i < expected.length; i++) {
                try (DocumentContext dc = wire.readingDocument()) {
                    assertTrue(dc.isPresent(), "Scalar document " + i + " should be present");
                    assertEquals(expected[i], dc.wire().getValueIn().int32(),
                            "Scalar document " + i + " should match");
                }
            }
            try (DocumentContext dc = wire.readingDocument()) {
                assertFalse(dc.isPresent(), "No additional scalar documents should remain");
            }
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("should parse document followed by terminal CR delimiter")
    public void documentWithTerminalCarriageReturnDelimiter() {
        String jsonl = "{\"name\":\"tail\",\"count\":5}\r";
        Bytes<?> bytes = Bytes.from(jsonl);
        try {
            Wire wire = WireType.JSONL.apply(bytes);

            try (DocumentContext dc = wire.readingDocument()) {
                assertTrue(dc.isPresent(), "Document should be present before terminal CR");
                Flat record = new Flat();
                record.readMarshallable(dc.wire());
                assertEquals("tail", record.name);
                assertEquals(5, record.count);
            }
            try (DocumentContext dc = wire.readingDocument()) {
                assertFalse(dc.isPresent(), "Terminal CR should be consumed as delimiter");
            }
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("should parse CR-only separated JSONL input")
    public void crOnlySeparatedInput() {
        String jsonl = "{\"name\":\"a\",\"count\":1}\r{\"name\":\"b\",\"count\":2}";
        Bytes<?> bytes = Bytes.from(jsonl);
        try {
            Wire wire = WireType.JSONL.apply(bytes);

            List<Flat> records = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                try (DocumentContext dc = wire.readingDocument()) {
                    assertTrue(dc.isPresent(), "Document " + i + " should be present");
                    Flat record = new Flat();
                    record.readMarshallable(dc.wire());
                    records.add(record);
                }
            }

            assertEquals(2, records.size());
            assertEquals("a", records.get(0).name);
            assertEquals("b", records.get(1).name);
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("should skip leading whitespace before first document")
    public void leadingWhitespaceBeforeFirstDocument() {
        String jsonl = "  \t\n{\"name\":\"spaced\",\"count\":7}";
        Bytes<?> bytes = Bytes.from(jsonl);
        try {
            Wire wire = WireType.JSONL.apply(bytes);

            try (DocumentContext dc = wire.readingDocument()) {
                assertTrue(dc.isPresent(), "Document should be present after leading whitespace");
                Flat record = new Flat();
                record.readMarshallable(dc.wire());
                assertEquals("spaced", record.name);
                assertEquals(7, record.count);
            }
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("should handle mixed delimiters between documents")
    public void mixedDelimitersBetweenDocuments() {
        // comma + spaces + newline between documents
        String jsonl = "{\"name\":\"a\",\"count\":1} , \n {\"name\":\"b\",\"count\":2}";
        Bytes<?> bytes = Bytes.from(jsonl);
        try {
            Wire wire = WireType.JSONL.apply(bytes);

            List<Flat> records = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                try (DocumentContext dc = wire.readingDocument()) {
                    assertTrue(dc.isPresent(), "Document " + i + " should be present");
                    Flat record = new Flat();
                    record.readMarshallable(dc.wire());
                    records.add(record);
                }
            }

            assertEquals(2, records.size());
            assertEquals("a", records.get(0).name);
            assertEquals("b", records.get(1).name);
        } finally {
            bytes.releaseLast();
        }
    }

    // =========================================================================
    // Multi-document streaming read via readingDocument() loop
    // =========================================================================

    @Test
    @DisplayName("should stream multiple documents via readingDocument() loop")
    public void streamingReadViaReadingDocument() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            // Write 5 records
            for (int i = 0; i < 5; i++) {
                Flat record = new Flat("r" + i, i);
                try (DocumentContext dc = wire.writingDocument()) {
                    dc.wire().getValueOut().object(Flat.class, record);
                }
            }

            // Read back via readingDocument() loop
            bytes.readPosition(0);
            Wire readWire = WireType.JSONL.apply(bytes);

            List<Flat> records = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                try (DocumentContext dc = readWire.readingDocument()) {
                    assertTrue(dc.isPresent(), "Document " + i + " should be present");
                    Flat record = new Flat();
                    record.readMarshallable(dc.wire());
                    records.add(record);
                }
            }

            // Verify exhaustion
            try (DocumentContext dc = readWire.readingDocument()) {
                assertFalse(dc.isPresent(), "No more documents should be present");
            }

            assertEquals(5, records.size());
            for (int i = 0; i < 5; i++) {
                assertEquals("r" + i, records.get(i).name);
                assertEquals(i, records.get(i).count);
            }
        }
    }

    @Test
    @DisplayName("readingDocument on empty bytes should return not present")
    public void readingDocumentOnEmptyBytes() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            try (DocumentContext dc = wire.readingDocument()) {
                assertFalse(dc.isPresent(), "Empty bytes should yield non-present document");
            }
        }
    }

    @Test
    @DisplayName("readingDocument on whitespace-only bytes should return not present")
    public void readingDocumentOnWhitespaceOnlyBytes() {
        Bytes<?> bytes = Bytes.from("  \n\t\r\n  ");
        try {
            Wire wire = WireType.JSONL.apply(bytes);

            try (DocumentContext dc = wire.readingDocument()) {
                assertFalse(dc.isPresent(), "Whitespace-only bytes should yield non-present document");
            }
        } finally {
            bytes.releaseLast();
        }
    }

    /**
     * Documents a known limitation: {@code readingDocument()} strips the outer
     * {@code &#123;&#125;} as document delimiters, so calling
     * {@code getValueIn().object(Class)} inside the context fails because
     * it expects another opening brace for the marshallable.
     * Use {@code readMarshallable(wire)} instead.
     */
    @Test
    @Disabled("readingDocument() strips outer braces; object() then fails expecting '{'")
    @DisplayName("readingDocument + object() fails because braces are consumed as document delimiters")
    public void readingDocumentPlusObjectFails() {
        String jsonl = "{\"name\":\"test\",\"count\":1}";
        Bytes<?> bytes = Bytes.from(jsonl);
        try {
            Wire wire = WireType.JSONL.apply(bytes);

            try (DocumentContext dc = wire.readingDocument()) {
                assertTrue(dc.isPresent());
                // This pattern fails: readingDocument() already consumed the '{' and '}'
                // so object() cannot find the opening brace it expects
                Flat record = dc.wire().getValueIn().object(Flat.class);
                assertEquals("test", record.name);
                assertEquals(1, record.count);
            }
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("incomplete object without closing brace should still produce one document boundary")
    public void incompleteObjectStillFormsDocumentBoundary() {
        String jsonl = "{\"name\":\"partial\"";
        Bytes<?> bytes = Bytes.from(jsonl);
        try {
            Wire wire = WireType.JSONL.apply(bytes);

            try (DocumentContext dc = wire.readingDocument()) {
                assertTrue(dc.isPresent(), "Incomplete trailing object should still be exposed as a document");
                Flat record = new Flat();
                record.readMarshallable(dc.wire());
                assertEquals("partial", record.name, "Parsed value should include available fields");
                assertEquals(0, record.count, "Missing fields should keep default values");
            }
            try (DocumentContext dc = wire.readingDocument()) {
                assertFalse(dc.isPresent(), "Incomplete object should still consume the remaining input");
            }
        } finally {
            bytes.releaseLast();
        }
    }

    // =========================================================================
    // findDocumentBounds edge cases
    // =========================================================================

    @Test
    @DisplayName("multiple nested objects should be parsed as separate documents")
    public void multipleNestedObjectDocuments() {
        String jsonl = "{\"id\":\"a\",\"child\":{\"name\":\"x\",\"count\":1}}\n" +
                "{\"id\":\"b\",\"child\":{\"name\":\"y\",\"count\":2}}\n";
        Bytes<?> bytes = Bytes.from(jsonl);
        try {
            Wire wire = WireType.JSONL.apply(bytes);

            List<Nested> records = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                try (DocumentContext dc = wire.readingDocument()) {
                    assertTrue(dc.isPresent(), "Document " + i + " should be present");
                    Nested record = new Nested();
                    record.readMarshallable(dc.wire());
                    records.add(record);
                }
            }

            assertEquals(2, records.size());
            assertEquals("a", records.get(0).id);
            assertEquals("x", records.get(0).child.name);
            assertEquals("b", records.get(1).id);
            assertEquals("y", records.get(1).child.name);
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("strings containing braces should not confuse document boundary detection")
    public void bracesInsideStringsDoNotSplitDocuments() {
        // Manually crafted input where string values contain { } [ ] characters
        String jsonl = "{\"name\":\"{nested[0]}\",\"count\":1}\n{\"name\":\"plain\",\"count\":2}\n";
        Bytes<?> bytes = Bytes.from(jsonl);
        try {
            Wire wire = WireType.JSONL.apply(bytes);

            try (DocumentContext dc = wire.readingDocument()) {
                assertTrue(dc.isPresent());
                Flat r1 = new Flat();
                r1.readMarshallable(dc.wire());
                assertEquals("{nested[0]}", r1.name);
                assertEquals(1, r1.count);
            }
            try (DocumentContext dc = wire.readingDocument()) {
                assertTrue(dc.isPresent());
                Flat r2 = new Flat();
                r2.readMarshallable(dc.wire());
                assertEquals("plain", r2.name);
                assertEquals(2, r2.count);
            }
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("escaped quotes inside strings should not break document boundary detection")
    public void escapedQuotesDoNotBreakBoundaryDetection() {
        // String value contains escaped quotes
        String jsonl = "{\"name\":\"say \\\"hi\\\"\",\"count\":1}\n{\"name\":\"ok\",\"count\":2}\n";
        Bytes<?> bytes = Bytes.from(jsonl);
        try {
            Wire wire = WireType.JSONL.apply(bytes);

            try (DocumentContext dc = wire.readingDocument()) {
                assertTrue(dc.isPresent());
                Flat r1 = new Flat();
                r1.readMarshallable(dc.wire());
                assertEquals("say \"hi\"", r1.name);
            }
            try (DocumentContext dc = wire.readingDocument()) {
                assertTrue(dc.isPresent());
                Flat r2 = new Flat();
                r2.readMarshallable(dc.wire());
                assertEquals("ok", r2.name);
            }
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("document without trailing delimiter should still parse")
    public void documentWithoutTrailingDelimiter() {
        // No trailing newline
        String jsonl = "{\"name\":\"last\",\"count\":99}";
        Bytes<?> bytes = Bytes.from(jsonl);
        try {
            Wire wire = WireType.JSONL.apply(bytes);

            try (DocumentContext dc = wire.readingDocument()) {
                assertTrue(dc.isPresent());
                Flat record = new Flat();
                record.readMarshallable(dc.wire());
                assertEquals("last", record.name);
                assertEquals(99, record.count);
            }

            // Should be exhausted
            try (DocumentContext dc = wire.readingDocument()) {
                assertFalse(dc.isPresent());
            }
        } finally {
            bytes.releaseLast();
        }
    }

    // =========================================================================
    // WireType.JSONL.asString()
    // =========================================================================

    @Test
    @DisplayName("WireType.JSONL.asString should produce valid single-line JSON")
    public void wireTypeAsStringProducesValidJson() {
        Flat record = new Flat("hello", 42);
        String output = WireType.JSONL.asString(record);

        assertNotNull(output, "asString should not return null");
        assertFalse(output.isEmpty(), "asString should not return empty string");

        // Should be a single JSON object (may have trailing newline)
        String trimmed = output.trim();
        assertTrue(trimmed.startsWith("{"), "asString should produce JSON starting with '{': " + trimmed);
        assertTrue(trimmed.endsWith("}"), "asString should produce JSON ending with '}': " + trimmed);
        assertTrue(trimmed.contains("\"name\":\"hello\""), "asString should contain field data: " + trimmed);
        assertTrue(trimmed.contains("\"count\":42"), "asString should contain field data: " + trimmed);
    }

    // =========================================================================
    // methodWriter with additional interfaces
    // =========================================================================

    @Test
    @DisplayName("manual object text with leading and trailing spaces should unwrap and keep one line")
    public void manualObjectTextWithPaddingUnwraps() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            try (DocumentContext dc = wire.writingDocument()) {
                dc.wire().bytes().append(" {\"manual\":1} ");
            }

            String output = bytes.toString();
            assertTrue(output.endsWith("\n"), "Output should end with newline");
            assertEquals(" {\"manual\":1} ", output.substring(0, output.length() - 1),
                    "Manual object text should be preserved and terminated by one newline");
        }
    }

    @Test
    @DisplayName("re-entrant writingDocument should still emit a single JSONL line")
    public void reentrantWritingDocumentEmitsSingleLine() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            try (DocumentContext outer = wire.writingDocument()) {
                assertTrue(outer.isPresent() || !outer.isMetaData(), "Outer document context should be active");
                try (DocumentContext inner = wire.writingDocument()) {
                    inner.wire().write("count").int32(1);
                }
            }

            String output = bytes.toString();
            String[] lines = output.split("\n");
            int nonEmpty = 0;
            for (String line : lines) {
                if (!line.isEmpty()) {
                    nonEmpty++;
                    assertTrue(line.startsWith("{") && line.endsWith("}"),
                            "Re-entrant write should still produce a JSON object per line");
                }
            }
            assertEquals(1, nonEmpty, "Re-entrant write should produce exactly one non-empty line");
            assertTrue(output.contains("\"count\":1"), "Output should contain the written field");
        }
    }

    @Test
    @DisplayName("methodWriter with additional interfaces should work in JSONL mode")
    public void methodWriterWithAdditionalInterfaces() {
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> bytes = stlBytes.get();
            Wire wire = WireType.JSONL.apply(bytes);

            // Create method writer with two interfaces
            Events writer = wire.methodWriter(Events.class, Events2.class);
            assertNotNull(writer, "methodWriter should return non-null proxy");

            // Should also implement the additional interface
            assertInstanceOf(Events2.class, writer, "Proxy should implement additional interface Events2");

            // Write via primary interface
            writer.event("test", 1);

            // Write via secondary interface
            ((Events2) writer).other("message");

            String output = bytes.toString();
            assertTrue(output.contains("\"test\""), "Output should contain event data: " + output);
            assertTrue(output.contains("\"message\""), "Output should contain other data: " + output);

            // Read back via MethodReader
            bytes.readPosition(0);
            Wire readWire = WireType.JSONL.apply(bytes);

            List<String> seen = new ArrayList<>();
            MethodReader reader = readWire.methodReader(
                    new Events() {
                        @Override
                        public void event(String name, int count) {
                            seen.add("event:" + name + ":" + count);
                        }
                    },
                    new Events2() {
                        @Override
                        public void other(String msg) {
                            seen.add("other:" + msg);
                        }
                    });

            while (reader.readOne()) {
                // consume
            }

            assertEquals(2, seen.size(), "Should read 2 events: " + seen);
            assertEquals("event:test:1", seen.get(0));
            assertEquals("other:message", seen.get(1));
        }
    }
}
