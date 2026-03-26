/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.util.DecoratedBufferUnderflowException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RawWireEdgeCaseTest extends WireTestCommon {

    @Test
    void writesAndReadsPrimitives() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        RawWire wire = new RawWire(bytes);

        wire.write().int32(10);
        wire.write().text("hello");
        wire.write().int64(20L);

        bytes.readPositionRemaining(0, bytes.writePosition());
        assertEquals(10, wire.read().int32());
        assertEquals("hello", wire.read().text());
        assertEquals(20L, wire.read().int64());

        // reading past end should fail with an underflow
        assertThrows(DecoratedBufferUnderflowException.class, () -> wire.read().int32());
    }
}
