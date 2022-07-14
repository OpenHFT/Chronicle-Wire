package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import org.junit.Test;

import java.io.DataOutput;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class IgnoreHighOrderBitsTest extends WireTestCommon {

    /**
     * Writes to the output stream the eight low-order bits of the argument b. The 24 high-order bits of b are ignored.
     * see https://docs.oracle.com/javase/7/docs/api/java/io/DataOutput.html#write(int)
     */
    @Test
    public void testWriteByte() throws IOException {
        @SuppressWarnings("rawtypes") final Bytes<?> bytes = new HexDumpBytes();
        try {
            final Wire wire = new BinaryWire(bytes);
            @SuppressWarnings("resource")
            DataOutput out = new WireObjectOutput(wire);
            int b = 256;
            out.write(b); // expecting 0 to be written

            assertEquals("" +
                            "a1 00                                           # 0\n",
                    bytes.toHexString());
        } finally {
            bytes.releaseLast();
        }
    }
}
