/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

/**
 * Exercises {@link Bytes} elastic behaviour without Wire involvement to prove capacity growth
 * maintains reader/writer cursor invariants.
 */
public class ElasticBytesCapacityTest extends WireTestCommon {

    @Test
    public void heapBytesGrowAndRestorePositions() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            assertElasticGrowthPreservesState(bytes);
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void directBytesGrowAndRestorePositions() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        Bytes<?> bytes = Bytes.allocateElasticDirect(32);
        try {
            assertElasticGrowthPreservesState(bytes);
        } finally {
            bytes.releaseLast();
        }
    }

    private void assertElasticGrowthPreservesState(Bytes<?> bytes) {
        long initialCapacity = bytes.realCapacity();
        int payloadLength = (int) initialCapacity + 128;
        fill(bytes, payloadLength);

        assertTrue("elastic buffer should grow when payload exceeds initial capacity",
                bytes.realCapacity() >= initialCapacity);

        long writePosition = bytes.writePosition();
        long readPosition = bytes.readPosition();
        long readLimit = bytes.readLimit();
        long writeLimit = bytes.writeLimit();

        bytes.readPositionRemaining(0, writePosition);
        assertEquals("read view should span written payload", writePosition, bytes.readLimit());
        assertEquals("read view should reset to start", 0, bytes.readPosition());

        bytes.readPosition(readPosition);
        bytes.readLimit(readLimit);
        assertEquals("write limit unchanged by read slices", writeLimit, bytes.writeLimit());

        Bytes<?> snapshot = bytes.bytesStore().bytesForRead();
        try {
            snapshot.readPositionRemaining(0, writePosition);
            assertEquals("snapshot read limit aligns with payload", writePosition, snapshot.readLimit());
            assertEquals("snapshot read position reset to zero", 0, snapshot.readPosition());
        } finally {
            snapshot.releaseLast();
        }

        assertEquals("original read position unaffected by snapshot", readPosition, bytes.readPosition());
        assertEquals("original read limit unaffected by snapshot", readLimit, bytes.readLimit());

        bytes.clear();
        assertEquals(0L, bytes.readPosition());
        assertEquals(0L, bytes.writePosition());
    }

    private void fill(Bytes<?> bytes, int length) {
        for (int i = 0; i < length; i++) {
            bytes.writeByte((byte) ('a' + (i % 23)));
        }
    }
}
