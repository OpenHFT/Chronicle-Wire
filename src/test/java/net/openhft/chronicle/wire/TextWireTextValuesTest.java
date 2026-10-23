/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.*;

public class TextWireTextValuesTest extends WireTestCommon {

    @Test
    public void readsBooleansFromText() {
        TextWire wire = wireWithText("true", "false", "TRUE");
        assertTrue(wire.read().bool());
        assertFalse(wire.read().bool());
        assertTrue(wire.read().bool());
    }

    @Test
    public void readsIntegersFromText() {
        TextWire wire = wireWithText("-128", "42", "32767", "65535", "-2147483648", "9223372036854775807");
        assertEquals(Byte.MIN_VALUE, wire.read().int8());
        assertEquals((byte) 42, wire.read().int8());
        assertEquals(Short.MAX_VALUE, wire.read().int16());
        assertEquals(0xFFFF, wire.read().uint16());
        assertEquals(Integer.MIN_VALUE, wire.read().int32());
        assertEquals(Long.MAX_VALUE, wire.read().int64());
    }

    @Test
    public void readsFloatingPointFromText() {
        TextWire wire = wireWithText("1.25", "-3.5e2", "NaN");
        assertEquals(1.25f, wire.read().float32(), 0.0f);
        assertEquals(-350.0, wire.read().float64(), 0.0);
        assertTrue(Double.isNaN(wire.read().float64()));
    }

    private static TextWire wireWithText(String... values) {
        TextWire wire = new TextWire(Bytes.allocateElasticOnHeap());
        for (String value : values)
            wire.write().text(value);
        return wire;
    }
}
