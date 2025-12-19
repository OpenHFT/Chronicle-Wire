/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings({"deprecation", "removal"})
public class ReadDocumentContextTest extends WireTestCommon {

    // Test for writing a document that's not complete using non-shared memory
    @Test
    public void testWritingNotCompleteDocument() {

        // Create an elastic byte buffer
        Bytes<?> b = Bytes.allocateElasticOnHeap();

        // Assert that memory is not shared
        assertFalse(b.sharedMemory(), "elastic on-heap bytes should not use shared memory");

        // Create a TEXT wire
        @NotNull Wire wire = WireType.TEXT.apply(b);
        assertFalse(wire.notCompleteIsNotPresent(), "non-shared memory wire should treat not-complete as present for reading");

        // Reading a document, expecting it to not be present
        try (DocumentContext dc = wire.readingDocument()) {
            assertFalse(dc.isPresent(), "document context should not be present when no data has been written");
            assertFalse(dc.isNotComplete(), "document context should not be marked as not-complete when no document exists");
        }

        // Write an incomplete document to the wire
        wire.writeNotCompleteDocument(false, w -> w.write("key").text("someText"));

        // Read the incomplete document and verify its content
        try (DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent(), "document context should be present when incomplete document exists in non-shared memory");
            assertTrue(dc.isNotComplete(), "document context should be marked as not-complete for incomplete document");
            assertFalse(dc.isMetaData(), "document should be data not metadata when written with meta=false");
            Assertions.assertEquals("someText", wire.read(() -> "key").text());
        }

        // Write a complete document to the wire
        wire.writeDocument(false, w -> w.write("key2").text("someText2"));

        // Read the complete document and verify its content
        try (DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent(), "document context should be present for complete document");
            assertFalse(dc.isNotComplete(), "document context should not be marked as not-complete for complete document");
            assertFalse(dc.isMetaData(), "document should be data not metadata when written with meta=false");
            Assertions.assertEquals("someText2", wire.read(() -> "key2").text());
        }

        // Release the byte buffer's resources
        b.releaseLast();
    }

    // Test for writing a document that's not complete using shared memory
    @Test
    public void testWritingNotCompleteDocumentShared() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        // Create a MappedBytes buffer with shared memory from a temp file
        @NotNull MappedBytes b = MappedBytes.mappedBytes(File.createTempFile("delete", "me"), 64 << 10);

        // Assert that memory is shared
        assertTrue(b.sharedMemory(), "mapped bytes should use shared memory for inter-process communication");

        // Create a TEXT wire
        @NotNull Wire wire = WireType.TEXT.apply(b);
        assertTrue(wire.notCompleteIsNotPresent(), "shared memory wire should treat not-complete documents as not present");

        // Reading a document, expecting it to not be present
        try (DocumentContext dc = wire.readingDocument()) {
            assertFalse(dc.isPresent(), "document context should not be present when no data has been written");
            assertFalse(dc.isNotComplete(), "document context should not be marked as not-complete when no document exists");
        }

        // Save the current write position of the wire
        long pos = wire.bytes().writePosition();

        // Write an incomplete document to the wire
        wire.writeNotCompleteDocument(false, w -> w.write("key").text("someText"));

        // Write a complete document to the wire
        wire.writeDocument(false, w -> w.write("key2").text("someText2"));

        // Reading a document, still expecting it to be incomplete
        try (DocumentContext dc = wire.readingDocument()) {
            assertFalse(dc.isPresent(), "document context should not be present when incomplete document exists in shared memory");
            assertTrue(dc.isNotComplete(), "document context should indicate not-complete status when incomplete document exists");
        }

        // Modify the header of the incomplete document to make it complete
        int header = wire.bytes().readInt(pos);
        assertTrue(wire.bytes().compareAndSwapInt(pos, header, header & ~Wires.NOT_COMPLETE), "compare-and-swap should succeed when marking incomplete document as complete");

        // Read the now completed document and verify its content
        try (DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent(), "document context should be present after marking document as complete");
            assertFalse(dc.isNotComplete(), "document context should not be marked as not-complete after completion flag cleared");
            assertFalse(dc.isMetaData(), "document should be data not metadata when written with meta=false");
            Assertions.assertEquals("someText", wire.read(() -> "key").text());
        }

        // Read the subsequent complete document and verify its content
        try (DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent(), "document context should be present for second complete document");
            assertFalse(dc.isNotComplete(), "document context should not be marked as not-complete for complete document");
            assertFalse(dc.isMetaData(), "document should be data not metadata when written with meta=false");
            Assertions.assertEquals("someText2", wire.read(() -> "key2").text());
        }

        // Release the MappedBytes' resources
        b.releaseLast();
    }

    @Test
    public void testEmptyMessage() {
        // Create an elastic byte buffer
        Bytes<?> b = Bytes.allocateElasticOnHeap();

        // Apply the TEXT wire type to the buffer
        Wire textWire = WireType.TEXT.apply(b);

        // Write an empty meta-data document to the wire
        textWire.writeDocument(true, w -> {});

        // Write a data document with content to the wire
        textWire.writeDocument(false, w -> w.write("key2").text("someText2"));

        // Read the empty meta-data document and verify its properties
        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent(), "document context should be present for empty metadata document");
            assertFalse(dc.isData(), "document context should not be data when written with meta=true");
            assertTrue(dc.wire().bytes().isEmpty(), "wire bytes should be empty for document with no content");
        }

        // Read the data document and verify its content
        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent(), "document context should be present for data document");
            assertFalse(dc.isMetaData(), "document should be data not metadata when written with meta=false");
            Assertions.assertEquals("someText2", textWire.read(() -> "key2").text());
        }

        // Release the byte buffer's resources
        b.releaseLast();
    }

    @Test
    public void testReadingADocumentThatHasNotBeenFullyReadFromTheTcpSocketAt2Bytes() {
        // Create an elastic byte buffer
        Bytes<?> b = Bytes.allocateElasticOnHeap();

        // Apply the TEXT wire type to the buffer
        Wire textWire = WireType.TEXT.apply(b);

        // Write two meta-data documents and one data document to the wire
        textWire.writeDocument(true, w -> w.write("key").text("someText"));
        textWire.writeDocument(true, w -> w.write("key").text("someText"));
        textWire.writeDocument(false, w -> w.write("key2").text("someText2"));

        // Read the first meta-data document and verify its content
        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent(), "document context should be present for first metadata document");
            assertTrue(dc.isMetaData(), "document should be metadata when written with meta=true");
            Assertions.assertEquals("someText", textWire.read(() -> "key").text());
        }

        // Store the current read limit of the buffer
        long limit = b.readLimit();

        // Simulate a scenario where data has not been fully read from the socket by moving the read position and limiting the read limit
        long newReadPosition = b.readPosition() + 2;
        b.readLimit(newReadPosition);

        // Try reading the next document, but it should not be present due to the simulated limit
        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertFalse(dc.isPresent(), "document context should not be present when read limit prevents reading document header");
        }

        // Assert that the new read limit has been applied
        Assertions.assertEquals(newReadPosition, b.readLimit());

        // Reset the read limit to its original value
        b.readLimit(limit);

        // Read the next meta-data document (which was previously unreadable due to the limit) and verify its content
        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent(), "document context should be present for second metadata document after restoring read limit");
            assertTrue(dc.isMetaData(), "document should be metadata when written with meta=true");
            Assertions.assertEquals("someText", textWire.read(() -> "key").text());
        }

        // Read the data document and verify its content
        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent(), "document context should be present for data document");
            assertFalse(dc.isMetaData(), "document should be data not metadata when written with meta=false");
            Assertions.assertEquals("someText2", textWire.read(() -> "key2").text());
        }

        // Release the byte buffer's resources
        b.releaseLast();
    }

    @Test
    public void testReadingADocumentThatHasNotBeenFullyReadFromTheTcpSocketAt5Bytes() {
        // Create an elastic byte buffer
        Bytes<?> b = Bytes.allocateElasticOnHeap();

        // Apply the TEXT wire type to the buffer
        Wire wire = WireType.TEXT.apply(b);

        // Write two meta-data documents and one data document to the wire
        wire.writeDocument(true, w -> w.write("key").text("someText"));
        wire.writeDocument(true, w -> w.write("key").text("someText"));
        wire.writeDocument(false, w -> w.write("key2").text("someText2"));

        // Read the first meta-data document and verify its content
        try (@NotNull DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent(), "document context should be present for first metadata document");
            assertTrue(dc.isMetaData(), "document should be metadata when written with meta=true");
            Assertions.assertEquals("someText", wire.read(() -> "key").text());
        }

        // Store the current read limit of the buffer
        long limit = b.readLimit();

        // Simulate a scenario where data has not been fully read from the socket by moving the read position and setting the read limit 5 bytes further
        long newReadPosition = b.readPosition() + 5;
        b.readLimit(newReadPosition);

        // Try reading the next document; it should not be present due to the simulated limit
        try (@NotNull DocumentContext dc = wire.readingDocument()) {
            assertFalse(dc.isPresent(), "document context should not be present when read limit prevents reading document header");
        }

        // Assert that the new read limit has been applied
        Assertions.assertEquals(newReadPosition, b.readLimit());

        // Reset the read limit to its original value
        b.readLimit(limit);

        // Read the next meta-data document (which was previously unreadable due to the limit) and verify its content
        try (@NotNull DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent(), "document context should be present for second metadata document after restoring read limit");
            assertTrue(dc.isMetaData(), "document should be metadata when written with meta=true");
            Assertions.assertEquals("someText", wire.read(() -> "key").text());
        }

        // Read the data document and verify its content
        try (@NotNull DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent(), "document context should be present for data document");
            assertFalse(dc.isMetaData(), "document should be data not metadata when written with meta=false");
            Assertions.assertEquals("someText2", wire.read(() -> "key2").text());
        }

        // Release the byte buffer's resources
        b.releaseLast();
    }
}
