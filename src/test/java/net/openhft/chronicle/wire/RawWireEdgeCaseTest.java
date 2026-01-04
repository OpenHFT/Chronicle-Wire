/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.util.DecoratedBufferUnderflowException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RawWireEdgeCaseTest extends WireTestCommon {

    @Test
    @DisplayName("RawWire reads primitives in order and underflows at end")
    public void writesAndReadsPrimitives() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        RawWire wire = new RawWire(bytes);

        wire.write().int32(10);
        wire.write().text("hello");
        wire.write().int64(20L);

        bytes.readPositionRemaining(0, bytes.writePosition());
        assertEquals(10, wire.read().int32(), "int32 should round-trip");
        assertEquals("hello", wire.read().text(), "text should round-trip");
        assertEquals(20L, wire.read().int64(), "int64 should round-trip");

        // reading past end should fail with an underflow
        assertThrows(DecoratedBufferUnderflowException.class, () -> wire.read().int32(),
                "Reading beyond end should underflow");
    }
}
