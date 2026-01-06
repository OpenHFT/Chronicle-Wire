/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WiresTupleFieldInfoTest extends WireTestCommon {

    @Test
    @DisplayName("Null strategy yields UNKNOWN bracket type")
    void nullStrategyUsesUnknownBracketType() {
        assertEquals(BracketType.UNKNOWN, Wires.TupleFieldInfo.bracketType(null),
                "null strategy uses the UNKNOWN bracket type");
    }
}
