/*
 * Copyright 2016-2020 chronicle.software
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MappedBytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("rawtypes")
public class ReadDocumentContextTest extends WireTestCommon {

    // Test for writing a document that's not complete using non-shared memory
    @Test
    public void testWritingNotCompleteDocument() {

        // Create an elastic byte buffer
        Bytes<?> b = Bytes.elasticByteBuffer();

        // Assert that memory is not shared
        assertFalse(b.sharedMemory());

        // Create a TEXT wire
        @NotNull Wire wire = WireType.TEXT.apply(b);
        assertFalse(wire.notCompleteIsNotPresent());

        // Reading a document, expecting it to not be present
        try (DocumentContext dc = wire.readingDocument()) {
            assertFalse(dc.isPresent());
            assertFalse(dc.isNotComplete());
        }

        // Write an incomplete document to the wire
        wire.writeNotCompleteDocument(false, w -> w.write("key").text("someText"));

        // Read the incomplete document and verify its content
        try (DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertTrue(dc.isNotComplete());
            assertFalse(dc.isMetaData());
            Assert.assertEquals("someText", wire.read(() -> "key").text());
        }

        // Write a complete document to the wire
        wire.writeDocument(false, w -> w.write("key2").text("someText2"));

        // Read the complete document and verify its content
        try (DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertFalse(dc.isNotComplete());
            assertFalse(dc.isMetaData());
            Assert.assertEquals("someText2", wire.read(() -> "key2").text());
        }

        // Release the byte buffer's resources
        b.releaseLast();
    }

    // Test for writing a document that's not complete using shared memory
    @Test
    public void testWritingNotCompleteDocumentShared() throws IOException {
        // Create a MappedBytes buffer with shared memory from a temp file
        @NotNull MappedBytes b = MappedBytes.mappedBytes(File.createTempFile("delete", "me"), 64 << 10);

        // Assert that memory is shared
        assertTrue(b.sharedMemory());

        // Create a TEXT wire
        @NotNull Wire wire = WireType.TEXT.apply(b);
        assertTrue(wire.notCompleteIsNotPresent());

        // Reading a document, expecting it to not be present
        try (DocumentContext dc = wire.readingDocument()) {
            assertFalse(dc.isPresent());
            assertFalse(dc.isNotComplete());
        }

        // Save the current write position of the wire
        long pos = wire.bytes().writePosition();

        // Write an incomplete document to the wire
        wire.writeNotCompleteDocument(false, w -> w.write("key").text("someText"));

        // Write a complete document to the wire
        wire.writeDocument(false, w -> w.write("key2").text("someText2"));

        // Reading a document, still expecting it to be incomplete
        try (DocumentContext dc = wire.readingDocument()) {
            assertFalse(dc.isPresent());
            assertTrue(dc.isNotComplete());
        }

        // Modify the header of the incomplete document to make it complete
        int header = wire.bytes().readInt(pos);
        assertTrue(wire.bytes().compareAndSwapInt(pos, header, header & ~Wires.NOT_COMPLETE));

        // Read the now completed document and verify its content
        try (DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertFalse(dc.isNotComplete());
            assertFalse(dc.isMetaData());
            Assert.assertEquals("someText", wire.read(() -> "key").text());
        }

        // Read the subsequent complete document and verify its content
        try (DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertFalse(dc.isNotComplete());
            assertFalse(dc.isMetaData());
            Assert.assertEquals("someText2", wire.read(() -> "key2").text());
        }

        // Release the MappedBytes' resources
        b.releaseLast();
    }

    @Test
    public void testEmptyMessage() {
        // Create an elastic byte buffer
        Bytes<?> b = Bytes.elasticByteBuffer();

        // Apply the TEXT wire type to the buffer
        Wire textWire = WireType.TEXT.apply(b);

        // Write an empty meta-data document to the wire
        textWire.writeDocument(true, w -> {});

        // Write a data document with content to the wire
        textWire.writeDocument(false, w -> w.write("key2").text("someText2"));

        // Read the empty meta-data document and verify its properties
        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertFalse(dc.isData());
            assertTrue(dc.wire().bytes().isEmpty());
        }

        // Read the data document and verify its content
        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertFalse(dc.isMetaData());
            Assert.assertEquals("someText2", textWire.read(() -> "key2").text());
        }

        // Release the byte buffer's resources
        b.releaseLast();
    }

    @Test
    public void testReadingADocumentThatHasNotBeenFullyReadFromTheTcpSocketAt2Bytes() throws Exception {
        // Create an elastic byte buffer
        Bytes<?> b = Bytes.elasticByteBuffer();

        // Apply the TEXT wire type to the buffer
        Wire textWire = WireType.TEXT.apply(b);

        // Write two meta-data documents and one data document to the wire
        textWire.writeDocument(true, w -> w.write("key").text("someText"));
        textWire.writeDocument(true, w -> w.write("key").text("someText"));
        textWire.writeDocument(false, w -> w.write("key2").text("someText2"));

        // Read the first meta-data document and verify its content
        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertTrue(dc.isMetaData());
            Assert.assertEquals("someText", textWire.read(() -> "key").text());
        }

        // Store the current read limit of the buffer
        long limit = b.readLimit();

        // Simulate a scenario where data has not been fully read from the socket by moving the read position and limiting the read limit
        long newReadPosition = b.readPosition() + 2;
        b.readLimit(newReadPosition);

        // Try reading the next document, but it should not be present due to the simulated limit
        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertFalse(dc.isPresent());
        }

        // Assert that the new read limit has been applied
        Assert.assertEquals(newReadPosition, b.readLimit());

        // Reset the read limit to its original value
        b.readLimit(limit);

        // Read the next meta-data document (which was previously unreadable due to the limit) and verify its content
        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertTrue(dc.isMetaData());
            Assert.assertEquals("someText", textWire.read(() -> "key").text());
        }

        // Read the data document and verify its content
        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertFalse(dc.isMetaData());
            Assert.assertEquals("someText2", textWire.read(() -> "key2").text());
        }

        // Release the byte buffer's resources
        b.releaseLast();
    }

    @Test
    public void testReadingADocumentThatHasNotBeenFullyReadFromTheTcpSocketAt5Bytes() {
        // Create an elastic byte buffer
        Bytes<?> b = Bytes.elasticByteBuffer();

        // Apply the TEXT wire type to the buffer
        Wire wire = WireType.TEXT.apply(b);

        // Write two meta-data documents and one data document to the wire
        wire.writeDocument(true, w -> w.write("key").text("someText"));
        wire.writeDocument(true, w -> w.write("key").text("someText"));
        wire.writeDocument(false, w -> w.write("key2").text("someText2"));

        // Read the first meta-data document and verify its content
        try (@NotNull DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertTrue(dc.isMetaData());
            Assert.assertEquals("someText", wire.read(() -> "key").text());
        }

        // Store the current read limit of the buffer
        long limit = b.readLimit();

        // Simulate a scenario where data has not been fully read from the socket by moving the read position and setting the read limit 5 bytes further
        long newReadPosition = b.readPosition() + 5;
        b.readLimit(newReadPosition);

        // Try reading the next document; it should not be present due to the simulated limit
        try (@NotNull DocumentContext dc = wire.readingDocument()) {
            assertFalse(dc.isPresent());
        }

        // Assert that the new read limit has been applied
        Assert.assertEquals(newReadPosition, b.readLimit());

        // Reset the read limit to its original value
        b.readLimit(limit);

        // Read the next meta-data document (which was previously unreadable due to the limit) and verify its content
        try (@NotNull DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertTrue(dc.isMetaData());
            Assert.assertEquals("someText", wire.read(() -> "key").text());
        }

        // Read the data document and verify its content
        try (@NotNull DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertFalse(dc.isMetaData());
            Assert.assertEquals("someText2", wire.read(() -> "key2").text());
        }

        // Release the byte buffer's resources
        b.releaseLast();
    }
}
