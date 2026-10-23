/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReadAnyWireTextValuesTest extends WireTestCommon {

    @Test
    public void readsValuesFromTextWrittenAsText() {
        assertReadsValues(WireType.TEXT);
    }

    @Test
    public void readsValuesFromTextWrittenAsBinary() {
        assertReadsValues(WireType.BINARY);
    }

    private static void assertReadsValues(WireType writer) {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire out = writer.apply(bytes);
        for (String value : new String[]{"true", "false", "42", "-3.5e2"})
            out.write().text(value);

        Wire wire = WireType.READ_ANY.apply(bytes);
        assertTrue(wire.read().bool());
        assertFalse(wire.read().bool());
        assertEquals(42, wire.read().int32());
        assertEquals(-350.0, wire.read().float64(), 0.0);
    }
}
