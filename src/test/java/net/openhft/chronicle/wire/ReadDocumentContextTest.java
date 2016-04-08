package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author Rob Austin.
 */
public class ReadDocumentContextTest {

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
    public void testReadingADocumentThatHasNotBeenFullyReadFromTheTcpSocket() throws Exception {

        Bytes b = Bytes.elasticByteBuffer();

        TextWire textWire = new TextWire(b);

        textWire.writeDocument(true, w -> w.write("key").text("someText"));
        textWire.writeDocument(false, w -> w.write("key2").text("someText2"));

        long limit = b.readLimit();
        b.readLimit(2);

        try (DocumentContext dc = textWire.readingDocument()) {
            Assert.assertFalse(dc.isPresent());
        }

        Assert.assertEquals(2, b.readLimit());

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