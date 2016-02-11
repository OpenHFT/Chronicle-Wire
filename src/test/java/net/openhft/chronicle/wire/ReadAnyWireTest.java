package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

import static net.openhft.chronicle.wire.WireType.*;

/**
 * @author Rob Austin.
 */
public class ReadAnyWireTest {

    @Test
    public void testReadAny() throws Exception {
        final Bytes<ByteBuffer> t = Bytes.elasticByteBuffer();
        final Wire wire = TEXT.apply(t);
        wire.write((() -> "hello")).text("world");
        Assert.assertEquals("world", READ_ANY.apply(t).read(() -> "hello").text());
    }

    @Test
    public void testCreateReadAnyFirstTextWire() throws Exception {
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        final String expected = "world";
        TEXT.apply(bytes).write((() -> "hello")).text(expected);
        Assert.assertEquals(expected, READ_ANY.apply(bytes).read((() -> "hello")).text());
    }


    @Test
    public void testCreateReadAnyFirstBinaryWire() throws Exception {
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        final String expected = "world";
        BINARY.apply(bytes).write((() -> "hello")).text(expected);
        Assert.assertEquals(expected, READ_ANY.apply(bytes).read((() -> "hello")).text());
    }


    @Test
    public void testCreateReadAnyFirstFIELDLESS_BINARYWire() throws Exception {
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        final String expected = "world";
        FIELDLESS_BINARY.apply(bytes).write((() -> "hello")).text(expected);
        Assert.assertEquals(expected, READ_ANY.apply(bytes).read((() -> "hello")).text());
    }
}


