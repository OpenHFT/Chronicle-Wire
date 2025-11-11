/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.BitSet;

import static org.junit.Assume.assumeFalse;

public class BitSetTest extends WireTestCommon {

    @Before
    public void hasDirect() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
    }

    // Test the equality of a BitSet after being written and read from a wire
    @Test
    public void testBitSetEquals() {
        Bytes<?> b = Bytes.allocateElasticOnHeap();
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

    // Test the equality of a BitSet with multiple bits set after being written and read from a wire
    @Test
    public void testBitSetEquals2() {
        Bytes<?> b = Bytes.allocateElasticOnHeap();
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

    // Test the conversion of a BitSet to text format after being written to a wire
    @Test
    public void testBitSetToText() {
        Bytes<?> b = Bytes.allocateElasticOnHeap();
        try {
            Wire wire = WireType.TEXT.apply(b);

            BitSet bs = new BitSet(64);
            bs.set(10);

            wire.getValueOut().object(bs);
            Assert.assertEquals("!!bitset [\n" +
                    "  1024,\n" +
                    "  # 0000000000000000000000000000000000000000000000000000010000000000\n" +
                    "]\n", wire.toString());
        } finally {
            b.releaseLast();
        }
    }

    // Test the conversion of a BitSet with multiple bits set to text format after being written to a wire
    @Test
    public void testBitSet2ToText() {
        Bytes<?> b = Bytes.allocateElasticOnHeap();
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
                    "]\n", wire.toString());
        } finally {
            b.releaseLast();
        }
    }

    // Test reading a BitSet into an existing BitSet instance using 'using' from a wire
    @Test
    public void testBitSetUsing() {

        BitSet using = new BitSet(4);
        using.set(1);

        Bytes<?> b = Bytes.allocateElasticOnHeap();
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
