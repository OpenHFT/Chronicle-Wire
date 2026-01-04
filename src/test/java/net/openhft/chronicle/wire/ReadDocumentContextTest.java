/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings({"deprecation", "removal"})
class ReadDocumentContextTest extends WireTestCommon {

    // Test for writing a document that's not complete using non-shared memory
    @Test
    @DisplayName("Read not-complete document in non-shared memory")
    void testWritingNotCompleteDocument() {

        // Create an elastic byte buffer
        Bytes<?> b = Bytes.allocateElasticOnHeap();

        // Assert that memory is not shared
        assertFalse(b.sharedMemory(), "elastic on-heap bytes should not use shared memory");

        // Create a TEXT wire
        @NotNull Wire wire = WireType.TEXT.apply(b);
        assertFalse(wire.notCompleteIsNotPresent(), "non-shared memory wire should treat not-complete as present for reading");

        // Reading a document, expecting it to not be present
        try (DocumentContext dc = wire.readingDocument()) {
            assertFalse(dc.isPresent(),
                    "non-shared memory should report no document before any write");
            assertFalse(dc.isNotComplete(),
                    "non-shared memory should not mark not-complete before any document exists");
        }

        // Write an incomplete document to the wire
        wire.writeNotCompleteDocument(false, w -> w.write("key").text("someText"));

        // Read the incomplete document and verify its content
        try (DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent(),
                    "non-shared memory should expose incomplete document as present");
            assertTrue(dc.isNotComplete(),
                    "incomplete document should be marked not-complete in non-shared memory");
            assertFalse(dc.isMetaData(),
                    "non-shared incomplete document should be data when written with meta=false");
            Assertions.assertEquals("someText", wire.read(() -> "key").text(),
                    "incomplete document should contain key=someText");
        }

        // Write a complete document to the wire
        wire.writeDocument(false, w -> w.write("key2").text("someText2"));

        // Read the complete document and verify its content
        try (DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent(),
                    "complete document should be present in non-shared memory");
            assertFalse(dc.isNotComplete(),
                    "complete document should not be marked not-complete in non-shared memory");
            assertFalse(dc.isMetaData(),
                    "non-shared complete document should be data when written with meta=false");
            Assertions.assertEquals("someText2", wire.read(() -> "key2").text(),
                    "complete document should contain key2=someText2");
        }

        // Release the byte buffer's resources
        b.releaseLast();
    }

    // Test for writing a document that's not complete using shared memory
    @Test
    @DisplayName("Read not-complete document in shared memory")
    void testWritingNotCompleteDocumentShared() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for shared memory document tests");

        // Create a MappedBytes buffer with shared memory from a temp file
        @NotNull MappedBytes b = MappedBytes.mappedBytes(File.createTempFile("delete", "me"), 64 << 10);

        // Assert that memory is shared
        assertTrue(b.sharedMemory(), "mapped bytes should use shared memory for inter-process communication");

        // Create a TEXT wire
        @NotNull Wire wire = WireType.TEXT.apply(b);
        assertTrue(wire.notCompleteIsNotPresent(), "shared memory wire should treat not-complete documents as not present");

        // Reading a document, expecting it to not be present
        try (DocumentContext dc = wire.readingDocument()) {
            assertFalse(dc.isPresent(),
                    "shared memory should report no document before any write");
            assertFalse(dc.isNotComplete(),
                    "shared memory should not mark not-complete before any document exists");
        }

        // Save the current write position of the wire
        long pos = wire.bytes().writePosition();

        writeSharedNotCompleteDocuments(wire);

        // Modify the header of the incomplete document to make it complete
        int header = wire.bytes().readInt(pos);
        assertTrue(wire.bytes().compareAndSwapInt(pos, header, header & ~Wires.NOT_COMPLETE), "compare-and-swap should succeed when marking incomplete document as complete");

        // Read the now completed document and verify its content
        try (DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent(),
                    "shared memory should present document after completion flag cleared");
            assertFalse(dc.isNotComplete(),
                    "shared memory should not mark not-complete after completion flag cleared");
            assertFalse(dc.isMetaData(),
                    "shared memory completed document should be data when meta=false");
            Assertions.assertEquals("someText", wire.read(() -> "key").text(),
                    "completed shared document should contain key=someText");
        }

        // Read the subsequent complete document and verify its content
        try (DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent(),
                    "shared memory should present second complete document");
            assertFalse(dc.isNotComplete(),
                    "shared memory second document should not be marked not-complete");
            assertFalse(dc.isMetaData(),
                    "shared memory second document should be data when meta=false");
            Assertions.assertEquals("someText2", wire.read(() -> "key2").text(),
                    "second shared document should contain key2=someText2");
        }

        // Release the MappedBytes' resources
        b.releaseLast();
    }

    private static void writeSharedNotCompleteDocuments(@NotNull Wire wire) {
        // Write an incomplete document to the wire
        wire.writeNotCompleteDocument(false, w -> w.write("key").text("someText"));

        // Write a complete document to the wire
        wire.writeDocument(false, w -> w.write("key2").text("someText2"));

        // Reading a document, still expecting it to be incomplete
        try (DocumentContext dc = wire.readingDocument()) {
            assertFalse(dc.isPresent(),
                    "shared memory should not present incomplete document when not-complete flag is set");
            assertTrue(dc.isNotComplete(),
                    "shared memory should mark not-complete when incomplete document exists");
        }
    }

    @Test
    @DisplayName("Empty metadata document should remain empty with no payload bytes")
    void testEmptyMessage() {
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
            assertTrue(dc.isPresent(), "empty metadata document should be present");
            assertFalse(dc.isData(), "empty metadata document should not be data");
            assertTrue(dc.wire().bytes().isEmpty(), "empty metadata document should have no bytes");
        }

        // Read the data document and verify its content
        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent(), "data document should be present after empty metadata");
            assertFalse(dc.isMetaData(), "data document after empty metadata should be non-metadata");
            Assertions.assertEquals("someText2", textWire.read(() -> "key2").text(),
                    "data document after empty metadata should contain key2=someText2");
        }

        // Release the byte buffer's resources
        b.releaseLast();
    }

    @Test
    @DisplayName("Read limit at 2 bytes blocks document header")
    void testReadingADocumentThatHasNotBeenFullyReadFromTheTcpSocketAt2Bytes() {
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
            assertTrue(dc.isPresent(),
                    "first metadata document should be present before read-limit change");
            assertTrue(dc.isMetaData(),
                    "first document should be metadata before 2-byte read limit");
            Assertions.assertEquals("someText", textWire.read(() -> "key").text(),
                    "first metadata document should contain key=someText before 2-byte limit change");
        }

        // Store the current read limit of the buffer
        long limit = b.readLimit();
        long newReadPosition = b.readPosition() + 2;
        b.readLimit(newReadPosition);
        try {
            // Try reading the next document, but it should not be present due to the simulated limit
            try (@NotNull DocumentContext dc = textWire.readingDocument()) {
                assertFalse(dc.isPresent(),
                        "read limit at 2 bytes should prevent reading document header");
            }

            // Assert that the new read limit has been applied
            Assertions.assertEquals(newReadPosition, b.readLimit(),
                    "read limit should be set to 2-byte simulated position");
        } finally {
            // Reset the read limit to its original value
            b.readLimit(limit);
        }

        // Read the next meta-data document (which was previously unreadable due to the limit) and verify its content
        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent(),
                    "second metadata document should be present after restoring read limit");
            assertTrue(dc.isMetaData(),
                    "second document should be metadata after 2-byte limit restore");
            Assertions.assertEquals("someText", textWire.read(() -> "key").text(),
                    "second metadata document should contain key=someText after 2-byte limit restore");
        }

        // Read the data document and verify its content
        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent(), "data document should be present after 2-byte limit");
            assertFalse(dc.isMetaData(), "data document after 2-byte limit should be non-metadata");
            Assertions.assertEquals("someText2", textWire.read(() -> "key2").text(),
                    "data document after 2-byte limit should contain key2=someText2");
        }

        // Release the byte buffer's resources
        b.releaseLast();
    }

    @Test
    @DisplayName("Read limit at 5 bytes blocks document header")
    void testReadingADocumentThatHasNotBeenFullyReadFromTheTcpSocketAt5Bytes() {
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
            assertTrue(dc.isPresent(),
                    "first metadata document should be present before 5-byte limit");
            assertTrue(dc.isMetaData(),
                    "first document should be metadata before 5-byte read limit");
            Assertions.assertEquals("someText", wire.read(() -> "key").text(),
                    "first metadata document should contain key=someText before 5-byte limit change");
        }

        // Store the current read limit of the buffer
        long limit = b.readLimit();
        long newReadPosition = b.readPosition() + 5;
        b.readLimit(newReadPosition);
        try {
            // Try reading the next document; it should not be present due to the simulated limit
            try (@NotNull DocumentContext dc = wire.readingDocument()) {
                assertFalse(dc.isPresent(),
                        "read limit at 5 bytes should prevent reading document header");
            }

            // Assert that the new read limit has been applied
            Assertions.assertEquals(newReadPosition, b.readLimit(),
                    "read limit should be set to 5-byte simulated position");
        } finally {
            // Reset the read limit to its original value
            b.readLimit(limit);
        }

        // Read the next meta-data document (which was previously unreadable due to the limit) and verify its content
        try (@NotNull DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent(),
                    "second metadata document should be present after restoring 5-byte limit");
            assertTrue(dc.isMetaData(),
                    "second document should be metadata after 5-byte limit restore");
            Assertions.assertEquals("someText", wire.read(() -> "key").text(),
                    "second metadata document should contain key=someText after 5-byte limit restore");
        }

        // Read the data document and verify its content
        try (@NotNull DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent(), "data document should be present after 5-byte limit");
            assertFalse(dc.isMetaData(), "data document after 5-byte limit should be non-metadata");
            Assertions.assertEquals("someText2", wire.read(() -> "key2").text(),
                    "data document after 5-byte limit should contain key2=someText2");
        }

        // Release the byte buffer's resources
        b.releaseLast();
    }
}
