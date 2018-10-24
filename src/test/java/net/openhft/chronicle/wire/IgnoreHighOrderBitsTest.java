package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Created by Rob Austin
 */
public class IgnoreHighOrderBitsTest {

    /**
     * Writes to the output stream the eight low-order bits of the argument b. The 24 high-order bits of b are ignored.
     * see https://docs.oracle.com/javase/7/docs/api/java/io/DataOutput.html#write(int)
     * @throws IOException
     */
    @Test
    public void testWriteByte() throws IOException {
        final Bytes bytes = Bytes.elasticByteBuffer();
        try {
            final Wire wire = new BinaryWire(bytes);
            DataOutput out = new WireObjectOutput(wire);
            int b = 256;
            out.write(b); // expecting 0 to be written

        } finally {
            bytes.release();
        }
    }

}
