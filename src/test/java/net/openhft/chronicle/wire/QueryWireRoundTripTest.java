/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class QueryWireRoundTripTest extends WireTestCommon {

    @Test
    @DisplayName("QueryWire round-trips all query parameters")
    public void writesAndReadsQueryParameters() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        QueryWire wire = new QueryWire(bytes);

        wire.write("name").text("bob");
        wire.write("age").int32(42);
        wire.write("flag").bool(true);

        String query = bytes.toString();
        assertTrue(query.contains("name=bob"), "query should contain name=bob, actual: " + query);

        QueryWire reader = new QueryWire(Bytes.from(query));
        assertEquals("bob", reader.read("name").text(), "Name should round-trip");
        assertEquals(42, reader.read("age").int32(), "Age should round-trip");
        assertTrue(reader.read("flag").bool(), "Flag should round-trip");
    }
}

