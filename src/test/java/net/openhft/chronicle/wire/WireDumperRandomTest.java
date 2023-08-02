package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.OnHeapBytes;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class WireDumperRandomTest {

    public static final int LEN = 64;

    @Test
    public void dumpBinary() {
        int count = 0;
        OnHeapBytes bytes = Bytes.allocateElasticOnHeap(LEN);
        String starts = "--- !!data #binary\n" +
                "00000000             ";
        for (int i = 0; i < 20000; i++) {
            Random random = new Random(i);
            bytes.clear()
                    .writeInt(LEN - 4)
                    .writeInt(random.nextInt());
            for (int n = 8; n < LEN; n += 8)
                bytes.writeLong(random.nextLong());

            String string = WireDumper.of(new BinaryWire(bytes))
                    .asString();
            if (!string.startsWith(starts)) {
                count++;
//                if (count++ < 10)
//                    System.out.println("i: " + i + " " + string);
//                assertEquals("i: " + i + " " + string, starts, string.substring(0, starts.length()));
            }
        }
        assertEquals(3, count);
    }
}
