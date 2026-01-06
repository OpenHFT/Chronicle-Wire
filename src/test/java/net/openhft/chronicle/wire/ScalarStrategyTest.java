/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ScalarStrategyTest extends WireTestCommon {

    @Test
    @DisplayName("ScalarStrategy returns null when ValueIn reports a null field input")
    void scalarStrategyReturnsNullForNullInput() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(64));
        wire.write().text((String) null);

        ScalarStrategy<String> strategy = ScalarStrategy.text(String.class, String::toUpperCase);
        ValueIn valueIn = wire.read();
        assertNull(strategy.readUsing(String.class, null, valueIn, BracketType.NONE),
                "ScalarStrategy.readUsing should return null for null input");
    }

    @Test
    @DisplayName("ScalarStrategy reads text values with the supplied converter")
    void scalarStrategyReadsTextValues() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(64));
        wire.write().text("abc");

        ScalarStrategy<String> strategy = ScalarStrategy.text(String.class, String::toUpperCase);
        ValueIn valueIn = wire.read();
        assertEquals("ABC", strategy.readUsing(String.class, null, valueIn, BracketType.NONE),
                "Text values should be converted by the supplied function");
    }
}
