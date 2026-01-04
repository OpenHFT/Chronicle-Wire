/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case tests for VanillaMethodReader to improve branch coverage.
 * Targets the 83 missed branches identified in coverage analysis.
 */
@SuppressWarnings({"deprecation", "removal"})
class VanillaMethodReaderEdgeCaseTest extends WireTestCommon {

    // ========== Wire Document Context Tests ==========

    @Test
    @DisplayName("Wire should write and read document with string field")
    void testWriteReadDocumentString() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.TEXT.apply(bytes);

        wire.writeDocument(false, w -> w.write("field").text("value"));

        bytes.readPosition(0);

        AtomicReference<String> received = new AtomicReference<>();
        wire.readDocument(null, w -> received.set(w.read("field").text()));

        assertEquals("value", received.get(), "Document should read string field");
    }

    @Test
    @DisplayName("Wire should write and read document with int field")
    void testWriteReadDocumentInt() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.TEXT.apply(bytes);

        wire.writeDocument(false, w -> w.write("num").int32(42));

        bytes.readPosition(0);

        AtomicInteger received = new AtomicInteger();
        wire.readDocument(null, w -> received.set(w.read("num").int32()));

        assertEquals(42, received.get(), "Document should read int field");
    }

    @Test
    @DisplayName("Wire should write and read document with long field")
    void testWriteReadDocumentLong() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.TEXT.apply(bytes);

        wire.writeDocument(false, w -> w.write("num").int64(9876543210L));

        bytes.readPosition(0);

        AtomicReference<Long> received = new AtomicReference<>();
        wire.readDocument(null, w -> received.set(w.read("num").int64()));

        assertEquals(9876543210L, received.get(), "Document should read long field");
    }

    @Test
    @DisplayName("Wire should write and read document with double field")
    void testWriteReadDocumentDouble() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.TEXT.apply(bytes);

        wire.writeDocument(false, w -> w.write("val").float64(3.14159));

        bytes.readPosition(0);

        AtomicReference<Double> received = new AtomicReference<>();
        wire.readDocument(null, w -> received.set(w.read("val").float64()));

        assertEquals(3.14159, received.get(), 0.0001, "Document should read double field");
    }

    @Test
    @DisplayName("Wire should write and read document with boolean field")
    void testWriteReadDocumentBoolean() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.TEXT.apply(bytes);

        wire.writeDocument(false, w -> w.write("flag").bool(true));

        bytes.readPosition(0);

        AtomicBoolean received = new AtomicBoolean();
        wire.readDocument(null, w -> received.set(w.read("flag").bool()));

        assertTrue(received.get(), "Document should read boolean field");
    }

    // ========== Multiple Documents Tests ==========

    @Test
    @DisplayName("Wire should write and read multiple documents")
    void testMultipleDocuments() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.TEXT.apply(bytes);

        wire.writeDocument(false, w -> w.write("doc").int32(1));
        wire.writeDocument(false, w -> w.write("doc").int32(2));
        wire.writeDocument(false, w -> w.write("doc").int32(3));

        bytes.readPosition(0);

        List<Integer> received = new ArrayList<>();
        while (true) {
            boolean read = wire.readDocument(null, w -> received.add(w.read("doc").int32()));
            if (!read) {
                break;
            }
        }

        assertEquals(3, received.size(), "document count should be three after read loop");
        assertEquals(1, received.get(0), "First document should be 1");
        assertEquals(2, received.get(1), "Second document should be 2");
        assertEquals(3, received.get(2), "Third document should be 3");
    }

    // ========== Wire Type Tests ==========

    @Test
    @DisplayName("BinaryWire should write and read text document field")
    void testBinaryWireDocument() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.BINARY.apply(bytes);

        wire.writeDocument(false, w -> w.write("data").text("binary"));

        bytes.readPosition(0);

        AtomicReference<String> received = new AtomicReference<>();
        wire.readDocument(null, w -> received.set(w.read("data").text()));

        assertEquals("binary", received.get(), "BinaryWire document should read string");
    }

    @Test
    @DisplayName("YamlWire should write and read text document field")
    void testYamlWireDocument() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.YAML.apply(bytes);

        wire.writeDocument(false, w -> w.write("data").text("yaml"));

        bytes.readPosition(0);

        AtomicReference<String> received = new AtomicReference<>();
        wire.readDocument(null, w -> received.set(w.read("data").text()));

        assertEquals("yaml", received.get(), "YamlWire document should read string");
    }

    // ========== Null Value Tests ==========

    @Test
    @DisplayName("Wire should write and read null string field")
    void testNullStringValue() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.TEXT.apply(bytes);

        wire.writeDocument(false, w -> w.write("val").text((String) null));

        bytes.readPosition(0);

        AtomicReference<String> received = new AtomicReference<>("not-null");
        wire.readDocument(null, w -> received.set(w.read("val").text()));

        assertNull(received.get(), "null string field should read back as null");
    }

    // ========== Boundary Value Tests ==========

    @Test
    @DisplayName("Wire should handle max int value")
    void testMaxIntValue() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.TEXT.apply(bytes);

        wire.writeDocument(false, w -> w.write("max").int32(Integer.MAX_VALUE));

        bytes.readPosition(0);

        AtomicInteger received = new AtomicInteger();
        wire.readDocument(null, w -> received.set(w.read("max").int32()));

        assertEquals(Integer.MAX_VALUE, received.get(), "Max int should round-trip");
    }

    @Test
    @DisplayName("Wire should handle min int value")
    void testMinIntValue() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.TEXT.apply(bytes);

        wire.writeDocument(false, w -> w.write("min").int32(Integer.MIN_VALUE));

        bytes.readPosition(0);

        AtomicInteger received = new AtomicInteger();
        wire.readDocument(null, w -> received.set(w.read("min").int32()));

        assertEquals(Integer.MIN_VALUE, received.get(), "Min int should round-trip");
    }

    @Test
    @DisplayName("Wire should handle max long value")
    void testMaxLongValue() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.TEXT.apply(bytes);

        wire.writeDocument(false, w -> w.write("max").int64(Long.MAX_VALUE));

        bytes.readPosition(0);

        AtomicReference<Long> received = new AtomicReference<>();
        wire.readDocument(null, w -> received.set(w.read("max").int64()));

        assertEquals(Long.MAX_VALUE, received.get(), "Max long should round-trip");
    }

    @Test
    @DisplayName("Wire should handle min+1 long value")
    void testMinPlusOneLongValue() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.TEXT.apply(bytes);

        wire.writeDocument(false, w -> w.write("min").int64(Long.MIN_VALUE + 1));

        bytes.readPosition(0);

        AtomicReference<Long> received = new AtomicReference<>();
        wire.readDocument(null, w -> received.set(w.read("min").int64()));

        assertEquals(Long.MIN_VALUE + 1, received.get(), "Min+1 long should round-trip");
    }

    // ========== Empty String Tests ==========

    @Test
    @DisplayName("Wire should round-trip empty string field")
    void testEmptyStringValue() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.TEXT.apply(bytes);

        wire.writeDocument(false, w -> w.write("empty").text(""));

        bytes.readPosition(0);

        AtomicReference<String> received = new AtomicReference<>("not-empty");
        wire.readDocument(null, w -> received.set(w.read("empty").text()));

        assertEquals("", received.get(), "Empty string should round-trip");
    }

    // ========== Long String Tests ==========

    @Test
    @DisplayName("Wire should handle long string value")
    void testLongStringValue() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.TEXT.apply(bytes);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("x");
        }
        String longString = sb.toString();

        wire.writeDocument(false, w -> w.write("long").text(longString));

        bytes.readPosition(0);

        AtomicReference<String> received = new AtomicReference<>();
        wire.readDocument(null, w -> received.set(w.read("long").text()));

        assertEquals(longString, received.get(), "Long string should round-trip");
    }

    // ========== Metadata Document Tests ==========

    @Test
    @DisplayName("Wire should write and read metadata document")
    void testMetadataDocument() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.TEXT.apply(bytes);

        wire.writeDocument(true, w -> w.write("meta").text("metadata"));

        bytes.readPosition(0);

        AtomicReference<String> received = new AtomicReference<>();
        wire.readDocument(w -> received.set(w.read("meta").text()), null);

        assertEquals("metadata", received.get(), "metadata document value should be read");
    }

    // ========== Mixed Document Types Tests ==========

    @Test
    @DisplayName("Wire should handle mixed data and metadata documents")
    void testMixedDocumentTypes() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.TEXT.apply(bytes);

        wire.writeDocument(true, w -> w.write("meta").text("header"));
        wire.writeDocument(false, w -> w.write("data").text("content"));

        bytes.readPosition(0);

        AtomicReference<String> metaReceived = new AtomicReference<>();
        AtomicReference<String> dataReceived = new AtomicReference<>();

        wire.readDocument(w -> metaReceived.set(w.read("meta").text()), null);
        wire.readDocument(null, w -> dataReceived.set(w.read("data").text()));

        assertEquals("header", metaReceived.get(), "metadata header field should be read");
        assertEquals("content", dataReceived.get(), "data field payload should be read");
    }

    // ========== Nested Object Tests ==========

    @Test
    @DisplayName("Wire should handle nested marshallable object")
    void testNestedMarshallable() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.TEXT.apply(bytes);

        wire.write("outer").marshallable(inner -> inner.write("innerVal").int32(42));

        bytes.readPosition(0);

        AtomicInteger received = new AtomicInteger();
        wire.read("outer").marshallable(inner -> received.set(inner.read("innerVal").int32()));

        assertEquals(42, received.get(), "nested innerVal field should read 42");
    }

    // ========== Sequence Tests ==========

    @Test
    @DisplayName("Wire should handle sequence in document")
    void testSequenceInDocument() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.TEXT.apply(bytes);

        wire.writeDocument(false, w -> w.write("list").sequence(seq -> {
            seq.int32(1);
            seq.int32(2);
            seq.int32(3);
        }));

        bytes.readPosition(0);

        List<Integer> received = new ArrayList<>();
        wire.readDocument(null, w -> w.read("list").sequence(received, (list, seq) -> {
            while (seq.hasNextSequenceItem()) {
                list.add(seq.int32());
            }
        }));

        assertEquals(3, received.size(), "Sequence should have 3 items");
        assertEquals(1, received.get(0), "First item should be 1");
        assertEquals(2, received.get(1), "Second item should be 2");
        assertEquals(3, received.get(2), "Third item should be 3");
    }

    // ========== Event Name Tests ==========

    @Test
    @DisplayName("Wire should write and read event name")
    void testEventName() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.TEXT.apply(bytes);

        wire.writeEventName("myEvent").text("eventData");

        bytes.readPosition(0);

        StringBuilder sb = new StringBuilder();
        wire.readEventName(sb);
        String data = wire.getValueIn().text();

        assertEquals("myEvent", sb.toString(), "Event name should be read");
        assertEquals("eventData", data, "Event data should be read");
    }

    // ========== Clear and Reset Tests ==========

    @Test
    @DisplayName("Wire clear should reset write position to zero")
    void testWireClear() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.TEXT.apply(bytes);

        wire.write("key").text("value");
        assertTrue(bytes.writePosition() > 0, "Wire should have written data");

        wire.clear();
        assertEquals(0, bytes.writePosition(), "clear should reset bytes write position to zero");
    }
}
