/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ValueInObjectFallbackTest extends WireTestCommon {

    public static class Holder extends SelfDescribingMarshallable {
        String name;
    }

    @Test
    public void readsMarshallableAsObject() {
        String yaml = "value: { name: bob }";
        TextWire wire = TextWire.from(yaml);
        Holder holder = wire.read("value").object(null, Holder.class, false);
        assertEquals("bob", holder.name);
    }
}

