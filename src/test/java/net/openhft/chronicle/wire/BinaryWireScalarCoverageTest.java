/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.*;

public class BinaryWireScalarCoverageTest extends WireTestCommon {

    @Test
    public void roundTripsCommonScalarTypes() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        wire.write("i32").int32(123);
        wire.write("i64").int64(Long.MIN_VALUE + 7);
        wire.write("boolTrue").bool(true);
        wire.write("boolFalse").bool(false);
        wire.write("text").text("hello");
        wire.write("float").float64(3.14159);
        wire.write("seq").sequence(v -> {
            v.int16((short) 1);
            v.text("two");
            v.int64(3L);
        });

        bytes.readPositionRemaining(0, bytes.writePosition());

        assertEquals(123, wire.read("i32").int32());
        assertEquals(Long.MIN_VALUE + 7, wire.read("i64").int64());
        assertTrue(wire.read("boolTrue").bool());
        assertFalse(wire.read("boolFalse").bool());
        assertEquals("hello", wire.read("text").text());
        assertEquals(3.14159, wire.read("float").float64(), 0.0);

        Object[] holder = new Object[3];
        wire.read("seq").sequence(holder, (arr, in) -> {
            arr[0] = in.int16();
            arr[1] = in.text();
            arr[2] = in.int64();
        });
        assertEquals((short) 1, holder[0]);
        assertEquals("two", holder[1]);
        assertEquals(3L, holder[2]);
    }
}
