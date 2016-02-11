package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * @author Rob Austin.
 */
public class ReadAnyWireTest {

    @Test
    public void testReadAny() throws Exception {
        final Bytes<ByteBuffer> t = Bytes.elasticByteBuffer();
        final Wire wire = WireType.TEXT.apply(t);
        wire.write((() -> "hello")).text("world");
        WireType.READ_ANY.apply(t);
    }


    @Test
    public void testCreateReadAnyFirst() throws Exception {
        WireType.READ_ANY.apply(Bytes.allocateDirect(10));
        final Bytes<ByteBuffer> t = Bytes.elasticByteBuffer();
        final Wire wire = WireType.TEXT.apply(t);
        final String expected = "world";
        wire.write((() -> "hello")).text(expected);
        final String actual = wire.read((() -> "hello")).text();
        Assert.assertEquals(expected, actual);
    }
}
