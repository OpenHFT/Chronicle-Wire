import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import net.openhft.chronicle.wire.Wires;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * @author Rob Austin.
 */
public class ElasticByteBufferTest {

    @Test
    public void testElasticByteBufferWithWire() throws Exception {

        Bytes<ByteBuffer> byteBufferBytes = Bytes.elasticByteBuffer(10);

        Wire apply = WireType.BINARY.apply(byteBufferBytes);

        try (DocumentContext documentContext = apply.writingDocument(false)) {
            documentContext.wire().write("some key").text("some value of more than ten characters");
        }

        ByteBuffer byteBuffer = byteBufferBytes.underlyingObject();
        StringBuilder stringBuilder = Wires.acquireStringBuilder();
        while (byteBuffer.remaining() > 0) {
            stringBuilder.append((char) byteBuffer.get());
        }

        String s = stringBuilder.toString();
        Assert.assertTrue(s.contains("some value of more than ten characters"));
    }
}
