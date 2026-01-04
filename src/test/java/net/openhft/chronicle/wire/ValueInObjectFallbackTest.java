/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValueInObjectFallbackTest extends WireTestCommon {

    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    public static class Holder extends SelfDescribingMarshallable {
        String name;
    }

    @Test
    @DisplayName("Reads marshallable object via fallback object read")
    void readsMarshallableAsObject() {
        String yaml = "value: { name: bob }";
        TextWire wire = TextWire.from(yaml);
        Holder holder = wire.read("value").object(null, Holder.class, false);
        assertEquals("bob", holder.name, "Holder name should be read from YAML object fallback: " + yaml);
    }
}
