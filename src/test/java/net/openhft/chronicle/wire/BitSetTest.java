package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.Test;

import java.util.BitSet;

@SuppressWarnings("rawtypes")
public class BitSetTest extends WireTestCommon {

    @Test
    public void testBitSetEquals() {
        Bytes<?> b = Bytes.elasticByteBuffer();
        try {
            Wire wire = WireType.TEXT.apply(b);

            BitSet original = new BitSet(64);
            original.set(10);
            wire.getValueOut().object(original);

            BitSet read = wire.getValueIn().object(BitSet.class);
            Assert.assertEquals(original, read);
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void testBitSetEquals2() {
        Bytes<?> b = Bytes.elasticByteBuffer();
        try {
            Wire wire = WireType.TEXT.apply(b);

            BitSet original = new BitSet(64);
            original.set(10);
            original.set(89);
            wire.getValueOut().object(original);

            BitSet read = wire.getValueIn().object(BitSet.class);
            Assert.assertEquals(original, read);
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void testBitSetToText() {
        Bytes<?> b = Bytes.elasticByteBuffer();
        try {
            Wire wire = WireType.TEXT.apply(b);

            BitSet bs = new BitSet(64);
            bs.set(10);

            wire.getValueOut().object(bs);
            Assert.assertEquals("!!bitset [\n" +
                    "  1024,\n" +
                    "  # 0000000000000000000000000000000000000000000000000000010000000000\n" +
                    "]", wire.toString());
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void testBitSet2ToText() {
        Bytes<?> b = Bytes.elasticByteBuffer();
        try {
            Wire wire = WireType.TEXT.apply(b);

            BitSet bs = new BitSet(64);
            bs.set(10);
            bs.set(89);
            wire.getValueOut().object(bs);
            Assert.assertEquals("!!bitset [\n" +
                    "  1024,\n" +
                    "  # 0000000000000000000000000000000000000000000000000000010000000000\n" +
                    "  33554432,\n" +
                    "  # 0000000000000000000000000000000000000010000000000000000000000000\n" +
                    "]", wire.toString());
        } finally {
            b.releaseLast();
        }
    }

    @Test
    public void testBitSetUsing() {

        BitSet using = new BitSet(4);
        using.set(1);

        Bytes<?> b = Bytes.elasticByteBuffer();
        try {
            Wire wire = WireType.TEXT.apply(b);

            BitSet original = new BitSet(64);
            original.set(10);
            original.set(89);
            wire.getValueOut().object(original);

            BitSet read = wire.getValueIn().object(using, BitSet.class);
            Assert.assertEquals(original, read);
        } finally {
            b.releaseLast();
        }
    }
}