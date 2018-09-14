package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import org.junit.Test;

import java.nio.ByteOrder;

import static junit.framework.TestCase.assertEquals;

public class HexDumpTest {
    @Test
    public void testEndian() {
        if (ByteOrder.nativeOrder() != ByteOrder.LITTLE_ENDIAN)
            return;

        Bytes b = new HexDumpBytes();
        b.writeInt(0x0a0b0c0d);
        assertEquals("0d 0c 0b 0a\n", b.toHexString());
        b.release();
    }
}
