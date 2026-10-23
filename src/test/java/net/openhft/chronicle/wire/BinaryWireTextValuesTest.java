/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.*;

public class BinaryWireTextValuesTest extends WireTestCommon {

    @Test
    public void readsBooleansFromText() {
        BinaryWire wire = wireWithText("true", "false", "TRUE");
        assertTrue(wire.read().bool());
        assertFalse(wire.read().bool());
        assertTrue(wire.read().bool());
    }

    @Test
    public void readsIntegersFromText() {
        BinaryWire wire = wireWithText("-128", "42", "32767", "65535", "-2147483648", "9223372036854775807");
        assertEquals(Byte.MIN_VALUE, wire.read().int8());
        assertEquals((byte) 42, wire.read().int8());
        assertEquals(Short.MAX_VALUE, wire.read().int16());
        assertEquals(0xFFFF, wire.read().uint16());
        assertEquals(Integer.MIN_VALUE, wire.read().int32());
        assertEquals(Long.MAX_VALUE, wire.read().int64());
    }

    @Test
    public void roundsFractionalTextToInteger() {
        BinaryWire wire = wireWithText("1.5", "-2.5");
        assertEquals(2, wire.read().int32());
        assertEquals(-2L, wire.read().int64());
    }

    @Test
    public void readsFloatingPointFromText() {
        BinaryWire wire = wireWithText("1.25", "-3.5e2", "0.000000000000000000000000000000001", "NaN");
        assertEquals(1.25f, wire.read().float32(), 0.0f);
        assertEquals(-350.0, wire.read().float64(), 0.0);
        assertEquals(1e-33, wire.read().float64(), 0.0);
        assertTrue(Double.isNaN(wire.read().float64()));
    }

    @Test
    public void emptyTextReadsAsDefault() {
        BinaryWire wire = wireWithText("", "", "");
        assertEquals(Integer.MIN_VALUE, wire.read().int32());
        assertEquals(Long.MIN_VALUE, wire.read().int64());
        assertTrue(Double.isNaN(wire.read().float64()));
    }

    private static BinaryWire wireWithText(String... values) {
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        for (String value : values)
            wire.write().text(value);
        return wire;
    }
}
