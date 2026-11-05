/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Common tests for a wire that reads a value written as text as a typed value.
 * Each subclass supplies the wire under test. A subclass can add tests that apply to that wire only.
 */
public abstract class WireAsTextTestCommon extends WireTestCommon {

    /**
     * Returns a wire that reads the given values. Each value is written as text.
     */
    protected abstract Wire wireWithText(String... values);

    @Test
    public void readsBooleansFromText() {
        Wire wire = wireWithText("true", "false", "TRUE", "y", "yes", "n", "no");
        assertTrue(wire.read().bool());
        assertFalse(wire.read().bool());
        assertTrue(wire.read().bool());
        assertTrue(wire.read().bool());
        assertTrue(wire.read().bool());
        assertFalse(wire.read().bool());
        assertFalse(wire.read().bool());
    }

    @Test
    public void readsIntegersFromText() {
        Wire wire = wireWithText("-128", "42", "32767", "65535", "-2147483648", "9223372036854775807");
        assertEquals(Byte.MIN_VALUE, wire.read().int8());
        assertEquals((byte) 42, wire.read().int8());
        assertEquals(Short.MAX_VALUE, wire.read().int16());
        assertEquals(0xFFFF, wire.read().uint16());
        assertEquals(Integer.MIN_VALUE, wire.read().int32());
        assertEquals(Long.MAX_VALUE, wire.read().int64());
    }

    @Test
    public void readsFloatingPointFromText() {
        Wire wire = wireWithText("1.25", "-3.5e2", "0.000000000000000000000000000000001", "NaN");
        assertEquals(1.25f, wire.read().float32(), 0.0f);
        assertEquals(-350.0, wire.read().float64(), 0.0);
        assertEquals(1e-33, wire.read().float64(), 0.0);
        assertTrue(Double.isNaN(wire.read().float64()));
    }
}
