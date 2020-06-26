package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.Closeable;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.BitSet;

public class LongValueBitSetTest extends WireTestCommon {

    @Test
    public void testNextSetBit() {

        Bytes<ByteBuffer> b = Bytes.elasticByteBuffer();
        try {
            Wire w = WireType.BINARY.apply(b);
            int size = 1024;
            LongValueBitSet actual = new LongValueBitSet(size, w);

            BitSet expected = new BitSet();
            int maxValue = Integer.MIN_VALUE;
            int minValue = Integer.MAX_VALUE;

            for (int i = 0; i < 100; i++) {
                int bit = (int) (Math.random() * size);
                expected.set(bit);
                actual.set(bit);
                maxValue = Math.max(maxValue, bit);
                minValue = Math.min(minValue, bit);
            }

            int expectBit = expected.nextSetBit(0);
            int actualBit = actual.nextSetBit(0);

            Assert.assertEquals(minValue, actualBit);

            do {
                Assert.assertEquals(expectBit, actualBit);

                expectBit = expected.nextSetBit(expectBit + 1);
                actualBit = actual.nextSetBit(actualBit + 1, maxValue);

                Assert.assertEquals(expectBit, actualBit);

            } while (expectBit != -1);

            Closeable.closeQuietly(actual);

        } finally {
            b.releaseLast();
        }
    }
}