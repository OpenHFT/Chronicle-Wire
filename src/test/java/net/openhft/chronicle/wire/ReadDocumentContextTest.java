package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MappedBytes;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Rob Austin.
 */
public class ReadDocumentContextTest {
    @Test
    public void testWritingNotCompleteDocument() {

        Bytes b = Bytes.elasticByteBuffer();
        assertFalse(b.sharedMemory());
        Wire wire = new TextWire(b);
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
    }

    @Test
    public void testWritingNotCompleteDocumentShared() throws IOException {

        MappedBytes b = MappedBytes.mappedBytes(File.createTempFile("delete", "me"), 64 << 10,
                this);
        assertTrue(b.sharedMemory());
        Wire wire = new TextWire(b);
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

        TextWire textWire = new TextWire(b);

        textWire.writeDocument(true, w -> {
        });

        textWire.writeDocument(false, w -> w.write("key2").text("someText2"));

        try (DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertFalse(dc.isData());
            assertTrue(dc.wire().bytes().isEmpty());
        }

        try (DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertFalse(dc.isMetaData());
            Assert.assertEquals("someText2", textWire.read(() -> "key2").text());
        }
    }

    @Test
    public void testReadingADocumentThatHasNotBeenFullyReadFromTheTcpSocketAt2Bytes() throws
            Exception {

        Bytes b = Bytes.elasticByteBuffer();

        TextWire textWire = new TextWire(b);

        textWire.writeDocument(true, w -> w.write("key").text("someText"));
        textWire.writeDocument(true, w -> w.write("key").text("someText"));
        textWire.writeDocument(false, w -> w.write("key2").text("someText2"));

        try (DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertTrue(dc.isMetaData());
            Assert.assertEquals("someText", textWire.read(() -> "key").text());
        }

        long limit = b.readLimit();

        // simulate the the data has not been fully read off the socket
        long newReadPosition = b.readPosition() + 2;
        b.readLimit(newReadPosition);

        try (DocumentContext dc = textWire.readingDocument()) {
            assertFalse(dc.isPresent());
        }

        Assert.assertEquals(newReadPosition, b.readLimit());

        b.readLimit(limit);
        try (DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertTrue(dc.isMetaData());
            Assert.assertEquals("someText", textWire.read(() -> "key").text());
        }

        try (DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertFalse(dc.isMetaData());
            Assert.assertEquals("someText2", textWire.read(() -> "key2").text());
        }
    }

    @Test
    public void testReadingADocumentThatHasNotBeenFullyReadFromTheTcpSocketAt5Bytes() throws
            Exception {

        Bytes b = Bytes.elasticByteBuffer();

        TextWire textWire = new TextWire(b);

        textWire.writeDocument(true, w -> w.write("key").text("someText"));
        textWire.writeDocument(true, w -> w.write("key").text("someText"));
        textWire.writeDocument(false, w -> w.write("key2").text("someText2"));

        try (DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertTrue(dc.isMetaData());
            Assert.assertEquals("someText", textWire.read(() -> "key").text());
        }

        long limit = b.readLimit();

        // simulate the the data has not been fully read off the socket
        long newReadPosition = b.readPosition() + 5;
        b.readLimit(newReadPosition);

        try (DocumentContext dc = textWire.readingDocument()) {
            assertFalse(dc.isPresent());
        }

        Assert.assertEquals(newReadPosition, b.readLimit());

        b.readLimit(limit);
        try (DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertTrue(dc.isMetaData());
            Assert.assertEquals("someText", textWire.read(() -> "key").text());
        }

        try (DocumentContext dc = textWire.readingDocument()) {
            assertTrue(dc.isPresent());
            assertFalse(dc.isMetaData());
            Assert.assertEquals("someText2", textWire.read(() -> "key2").text());
        }
    }
}