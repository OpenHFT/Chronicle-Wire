/*
 * Copyright 2016 higherfrequencytrading.com
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
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.bytes.MappedBytes;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Rob Austin.
 */
public class ReadDocumentContextTest {
    @Ignore("The ability to write incomplete documents is deprecated, and will be removed in a future release")
    @Test
    public void testWritingNotCompleteDocument() {

        Bytes b = Bytes.elasticByteBuffer();
        assertFalse(b.sharedMemory());
        @NotNull Wire wire = new TextWire(b).useBinaryDocuments();
        assertFalse(wire.notCompleteIsNotPresent());

        try (DocumentContext dc = wire.readingDocument()) {
            assertFalse(dc.isPresent());
            assertFalse(dc.isNotComplete());
        }

        wire.writeNotCompleteDocument(false, w -> w.write("key").text("someText"));

        try (DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertTrue(dc.isNotComplete());
            assertFalse(dc.isMetaData());
            Assert.assertEquals("someText", wire.read(() -> "key").text());
        }

        wire.writeDocument(false, w -> w.write("key2").text("someText2"));

        try (DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertFalse(dc.isNotComplete());
            assertFalse(dc.isMetaData());
            Assert.assertEquals("someText2", wire.read(() -> "key2").text());
        }

        b.release();
    }

    @Test
    public void testWritingNotCompleteDocumentShared() throws IOException {

        @NotNull MappedBytes b = MappedBytes.mappedBytes(File.createTempFile("delete", "me"), 64 << 10);
        assertTrue(b.sharedMemory());
        @NotNull Wire wire = new TextWire(b).useBinaryDocuments();
        assertTrue(wire.notCompleteIsNotPresent());

        try (DocumentContext dc = wire.readingDocument()) {
            assertFalse(dc.isPresent());
            assertFalse(dc.isNotComplete());
        }

        long pos = wire.bytes().writePosition();
        wire.writeNotCompleteDocument(false, w -> w.write("key").text("someText"));

        wire.writeDocument(false, w -> w.write("key2").text("someText2"));

        try (DocumentContext dc = wire.readingDocument()) {
            assertFalse(dc.isPresent());
            assertTrue(dc.isNotComplete());
        }

        // go back and make the document complete.
        int header = wire.bytes().readInt(pos);
        assertTrue(wire.bytes().compareAndSwapInt(pos, header, header & ~Wires.NOT_COMPLETE));

        // now we can read it.
        try (DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertFalse(dc.isNotComplete());
            assertFalse(dc.isMetaData());
            Assert.assertEquals("someText", wire.read(() -> "key").text());
        }

        // and the message after it.
        try (DocumentContext dc = wire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertFalse(dc.isNotComplete());
            assertFalse(dc.isMetaData());
            Assert.assertEquals("someText2", wire.read(() -> "key2").text());
        }
        b.release();
    }

    @Test
    public void testEmptyMessage() {

        Bytes b = Bytes.elasticByteBuffer();

        @NotNull TextWire textWire = new TextWire(b).useBinaryDocuments();

        textWire.writeDocument(true, w -> {
        });

        textWire.writeDocument(false, w -> w.write("key2").text("someText2"));

        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertFalse(dc.isData());
            assertTrue(dc.wire().bytes().isEmpty());
        }

        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertFalse(dc.isMetaData());
            Assert.assertEquals("someText2", textWire.read(() -> "key2").text());
        }

        b.release();
    }

    @Test
    public void testReadingADocumentThatHasNotBeenFullyReadFromTheTcpSocketAt2Bytes() throws
            Exception {

        Bytes b = Bytes.elasticByteBuffer();

        @NotNull TextWire textWire = new TextWire(b).useBinaryDocuments();

        textWire.writeDocument(true, w -> w.write("key").text("someText"));
        textWire.writeDocument(true, w -> w.write("key").text("someText"));
        textWire.writeDocument(false, w -> w.write("key2").text("someText2"));

        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertTrue(dc.isMetaData());
            Assert.assertEquals("someText", textWire.read(() -> "key").text());
        }

        long limit = b.readLimit();

        // simulate the the data has not been fully read off the socket
        long newReadPosition = b.readPosition() + 2;
        b.readLimit(newReadPosition);

        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertFalse(dc.isPresent());
        }

        Assert.assertEquals(newReadPosition, b.readLimit());

        b.readLimit(limit);
        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertTrue(dc.isMetaData());
            Assert.assertEquals("someText", textWire.read(() -> "key").text());
        }

        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertFalse(dc.isMetaData());
            Assert.assertEquals("someText2", textWire.read(() -> "key2").text());
        }

        b.release();
    }

    @Test
    public void testReadingADocumentThatHasNotBeenFullyReadFromTheTcpSocketAt5Bytes() throws
            Exception {

        Bytes b = Bytes.elasticByteBuffer();

        @NotNull TextWire textWire = new TextWire(b).useBinaryDocuments();

        textWire.writeDocument(true, w -> w.write("key").text("someText"));
        textWire.writeDocument(true, w -> w.write("key").text("someText"));
        textWire.writeDocument(false, w -> w.write("key2").text("someText2"));

        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertTrue(dc.isMetaData());
            Assert.assertEquals("someText", textWire.read(() -> "key").text());
        }

        long limit = b.readLimit();

        // simulate the the data has not been fully read off the socket
        long newReadPosition = b.readPosition() + 5;
        b.readLimit(newReadPosition);

        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertFalse(dc.isPresent());
        }

        Assert.assertEquals(newReadPosition, b.readLimit());

        b.readLimit(limit);
        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertTrue(dc.isMetaData());
            Assert.assertEquals("someText", textWire.read(() -> "key").text());
        }

        try (@NotNull DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertFalse(dc.isMetaData());
            Assert.assertEquals("someText2", textWire.read(() -> "key2").text());
        }

        b.release();
    }

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }
}