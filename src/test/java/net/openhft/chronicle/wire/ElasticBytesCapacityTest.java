/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

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

        assertTrue(bytes.realCapacity() >= initialCapacity,
                "elastic buffer should grow when payload exceeds initial capacity");

        long writePosition = bytes.writePosition();
        long readPosition = bytes.readPosition();
        long readLimit = bytes.readLimit();
        final long writeLimit = bytes.writeLimit();

        bytes.readPositionRemaining(0, writePosition);
        assertEquals(writePosition, bytes.readLimit(), "read view should span written payload");
        assertEquals(0, bytes.readPosition(), "read view should reset to start");

        bytes.readPosition(readPosition);
        bytes.readLimit(readLimit);
        assertEquals(writeLimit, bytes.writeLimit(), "write limit unchanged by read slices");

        Bytes<?> snapshot = bytes.bytesStore().bytesForRead();
        try {
            snapshot.readPositionRemaining(0, writePosition);
            assertEquals(writePosition, snapshot.readLimit(), "snapshot read limit aligns with payload");
            assertEquals(0, snapshot.readPosition(), "snapshot read position reset to zero");
        } finally {
            snapshot.releaseLast();
        }

        assertEquals(readPosition, bytes.readPosition(), "original read position unaffected by snapshot");
        assertEquals(readLimit, bytes.readLimit(), "original read limit unaffected by snapshot");

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
