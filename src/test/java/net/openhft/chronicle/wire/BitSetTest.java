/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

class BitSetTest extends WireTestCommon {

    @BeforeEach
    void hasDirect() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory disabled; skip BitSet tests");
    }

    // Test the equality of a BitSet after being written and read from a wire
    @Test
    @DisplayName("Round-trips BitSet with single bit set")
    void testBitSetEquals() {
        Bytes<?> b = Bytes.allocateElasticOnHeap();
        try {
            Wire wire = WireType.TEXT.apply(b);

            BitSet original = new BitSet(64);
            original.set(10);
            wire.getValueOut().object(original);

            BitSet read = wire.getValueIn().object(BitSet.class);
            Assertions.assertEquals(original, read, "Expected BitSet to round-trip with single bit");
        } finally {
            b.releaseLast();
        }
    }

    // Test the equality of a BitSet with multiple bits set after being written and read from a wire
    @Test
    @DisplayName("Round-trips BitSet with multiple bits set")
    void testBitSetEquals2() {
        Bytes<?> b = Bytes.allocateElasticOnHeap();
        try {
            Wire wire = WireType.TEXT.apply(b);

            BitSet original = new BitSet(64);
            original.set(10);
            original.set(89);
            wire.getValueOut().object(original);

            BitSet read = wire.getValueIn().object(BitSet.class);
            Assertions.assertEquals(original, read, "Expected BitSet to round-trip with multiple bits");
        } finally {
            b.releaseLast();
        }
    }

    // Test the conversion of a BitSet to text format after being written to a wire
    @Test
    @DisplayName("Serialises BitSet to text format with expected output")
    void testBitSetToText() {
        Bytes<?> b = Bytes.allocateElasticOnHeap();
        try {
            Wire wire = WireType.TEXT.apply(b);

            BitSet bs = new BitSet(64);
            bs.set(10);

            wire.getValueOut().object(bs);
            Assertions.assertEquals("!!bitset [\n" +
                    "  1024,\n" +
                    "  # 0000000000000000000000000000000000000000000000000000010000000000\n" +
                    "]\n", wire.toString(),
                    "Expected BitSet text output for single bit");
        } finally {
            b.releaseLast();
        }
    }

    // Test the conversion of a BitSet with multiple bits set to text format after being written to a wire
    @Test
    @DisplayName("Serialises BitSet with multiple bits to text format")
    void testBitSet2ToText() {
        Bytes<?> b = Bytes.allocateElasticOnHeap();
        try {
            Wire wire = WireType.TEXT.apply(b);

            BitSet bs = new BitSet(64);
            bs.set(10);
            bs.set(89);
            wire.getValueOut().object(bs);
            Assertions.assertEquals("!!bitset [\n" +
                    "  1024,\n" +
                    "  # 0000000000000000000000000000000000000000000000000000010000000000\n" +
                    "  33554432,\n" +
                    "  # 0000000000000000000000000000000000000010000000000000000000000000\n" +
                    "]\n", wire.toString(),
                    "Expected BitSet text output for multiple bits");
        } finally {
            b.releaseLast();
        }
    }

    // Test reading a BitSet into an existing BitSet instance using 'using' from a wire
    @Test
    @DisplayName("Reuses target BitSet instance when reading values")
    void testBitSetUsing() {

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
            Assertions.assertEquals(original, read, "Expected BitSet to round-trip using provided instance");
        } finally {
            b.releaseLast();
        }
    }
}
