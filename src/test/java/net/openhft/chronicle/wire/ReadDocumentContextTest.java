package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author Rob Austin.
 */
public class ReadDocumentContextTest {

    @Test
    public void testReadingADocumentThatHasNotBeenFullyReadFromTheTcpSocket() throws Exception {

        Bytes b = Bytes.elasticByteBuffer();

        TextWire textWire = new TextWire(b);

        textWire.writeDocument(false, w -> w.write("key").text("someText"));

        b.readLimit(b.readLimit() - 5);

        try (DocumentContext dc = textWire.readingDocument()) {
            // not expected to call this
            Assert.fail();
        } catch (java.nio.BufferUnderflowException e) {
            Assert.assertTrue(true);
        }

        b.readLimit(b.readLimit() + 5);
        try (DocumentContext dc = textWire.readingDocument()) {
            Assert.assertTrue(dc.isPresent());
            Assert.assertEquals("someText", textWire.read(() -> "key").text());
        }

    }

}