package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Rob Austin.
 */
public class ReadDocumentContextTest {


    @Test
    public void testWritingNotReadDocument() throws Exception {

        Bytes b = Bytes.elasticByteBuffer();

        TextWire textWire = new TextWire(b);

        textWire.writeNotReadyDocument(false, w -> w.write("key").text("someText"));

        try (DocumentContext dc = textWire.readingDocument()) {
            Assert.assertTrue(dc.isPresent());
            Assert.assertFalse(dc.isMetaData());
            Assert.assertEquals("someText", textWire.read(() -> "key").text());
        }
    }


    @Test
    public void testEmptyMessage() throws Exception {

        Bytes b = Bytes.elasticByteBuffer();

        TextWire textWire = new TextWire(b);

        textWire.writeDocument(true, w -> {
        });

        textWire.writeDocument(false, w -> w.write("key2").text("someText2"));

        try (DocumentContext dc = textWire.readingDocument()) {
            Assert.assertTrue(dc.isPresent());
            Assert.assertFalse(dc.isData());
            Assert.assertTrue(dc.wire().bytes().isEmpty());
        }

        try (DocumentContext dc = textWire.readingDocument()) {
            Assert.assertTrue(dc.isPresent());
            Assert.assertFalse(dc.isMetaData());
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
            Assert.assertTrue(dc.isPresent());
            Assert.assertTrue(dc.isMetaData());
            Assert.assertEquals("someText", textWire.read(() -> "key").text());
        }

        long limit = b.readLimit();

        // simulate the the data has not been fully read off the socket
        long newReadPosition = b.readPosition() + 2;
        b.readLimit(newReadPosition);

        try (DocumentContext dc = textWire.readingDocument()) {
            Assert.assertFalse(dc.isPresent());
        }

        Assert.assertEquals(newReadPosition, b.readLimit());

        b.readLimit(limit);
        try (DocumentContext dc = textWire.readingDocument()) {
            Assert.assertTrue(dc.isPresent());
            Assert.assertTrue(dc.isMetaData());
            Assert.assertEquals("someText", textWire.read(() -> "key").text());
        }

        try (DocumentContext dc = textWire.readingDocument()) {
            Assert.assertTrue(dc.isPresent());
            Assert.assertFalse(dc.isMetaData());
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
            Assert.assertTrue(dc.isPresent());
            Assert.assertTrue(dc.isMetaData());
            Assert.assertEquals("someText", textWire.read(() -> "key").text());
        }

        long limit = b.readLimit();

        // simulate the the data has not been fully read off the socket
        long newReadPosition = b.readPosition() + 5;
        b.readLimit(newReadPosition);

        try (DocumentContext dc = textWire.readingDocument()) {
            Assert.assertFalse(dc.isPresent());
        }

        Assert.assertEquals(newReadPosition, b.readLimit());

        b.readLimit(limit);
        try (DocumentContext dc = textWire.readingDocument()) {
            Assert.assertTrue(dc.isPresent());
            Assert.assertTrue(dc.isMetaData());
            Assert.assertEquals("someText", textWire.read(() -> "key").text());
        }

        try (DocumentContext dc = textWire.readingDocument()) {
            Assert.assertTrue(dc.isPresent());
            Assert.assertFalse(dc.isMetaData());
            Assert.assertEquals("someText2", textWire.read(() -> "key2").text());
        }

    }
}