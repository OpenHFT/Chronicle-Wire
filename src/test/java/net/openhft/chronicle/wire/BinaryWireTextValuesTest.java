/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.*;

public class BinaryWireTextValuesTest extends WireAsTextTestCommon {

    @Test
    public void roundsFractionalTextToInteger() {
        Wire wire = wireWithText("1.5", "-2.5");
        assertEquals(2, wire.read().int32());
        assertEquals(-2L, wire.read().int64());
    }

    @Test
    public void emptyTextReadsAsDefault() {
        Wire wire = wireWithText("", "", "");
        assertEquals(Integer.MIN_VALUE, wire.read().int32());
        assertEquals(Long.MIN_VALUE, wire.read().int64());
        assertTrue(Double.isNaN(wire.read().float64()));
    }

    @Override
    protected Wire wireWithText(String... values) {
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        for (String value : values)
            wire.write().text(value);
        return wire;
    }
}
