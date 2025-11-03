/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class BinaryWireNestedDocumentTest extends WireTestCommon {

    @Test
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
            assertTrue(outer.containsKey("inner"));
            @SuppressWarnings("unchecked")
            Map<String, Object> inner = (Map<String, Object>) outer.get("inner");
            assertEquals("hi", inner.get("value"));
        }
    }
}

