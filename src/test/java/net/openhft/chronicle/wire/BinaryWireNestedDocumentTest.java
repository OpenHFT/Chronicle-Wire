/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BinaryWireNestedDocumentTest extends WireTestCommon {

    @Test
    @DisplayName("Round-trips nested marshallable document with map hierarchy")
    public void roundTripsNestedMarshallable() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire wire = new BinaryWire(bytes);

        try (DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("outer").marshallable(m ->
                    m.write("inner").marshallable(n -> n.write("value").text("hi")));
        }

        bytes.readPositionRemaining(0, bytes.writePosition());
        try (DocumentContext dc = wire.readingDocument()) {
            Map<String, Object> outer = dc.wire().read("outer").marshallableAsMap(String.class, Object.class);
            assertTrue(outer.containsKey("inner"),
                    "Expected outer map to contain inner entry");
            @SuppressWarnings("unchecked")
            Map<String, Object> inner = (Map<String, Object>) outer.get("inner");
            assertEquals("hi", inner.get("value"), "Expected inner value to round-trip");
        }
    }
}

