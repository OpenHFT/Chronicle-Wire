/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class WireArrayTestSupport {
    private WireArrayTestSupport() {
    }

    static void assertEmptyArrayRoundTrip(Wire wire, boolean expectTextString) {
        Object[] noObjects = {};
        wire.write("a").object(noObjects);

        if (expectTextString && wire instanceof TextWire) {
            assertEquals("a: []\n", wire.toString());
        }

        Object[] object = wire.read().object(Object[].class);
        assertEquals(0, object.length);
    }

    static void assertSimpleStringArrayRoundTrip(Supplier<Wire> wireSupplier, boolean expectTextString) {
        Wire wire = wireSupplier.get();
        Object[] threeObjects = {"abc", "def", "ghi"};
        wire.write("b").object(threeObjects);

        if (expectTextString && wire instanceof TextWire) {
            assertEquals("b: [\n" +
                    "  abc,\n" +
                    "  def,\n" +
                    "  ghi\n" +
                    "]\n", wire.toString());
        }

        Object[] object2 = wire.read().object(Object[].class);
        assertEquals(3, object2.length);
        assertEquals("[abc, def, ghi]", Arrays.toString(object2));
    }

    static void assertMixedArraysRoundTrip(Wire wire) {
        Object[] a1 = new Object[0];
        wire.write("empty").object(a1);
        Object[] a2 = {1L};
        wire.write("one").object(a2);
        Object[] a3 = {"Hello", 123, 10.1};
        wire.write("three").object(Object[].class, a3);

        Object o1 = wire.read().object(Object[].class);
        assertArrayEquals(a1, (Object[]) o1);
        Object o2 = wire.read().object(Object[].class);
        assertArrayEquals(a2, (Object[]) o2);
        Object o3 = wire.read().object(Object[].class);
        assertArrayEquals(a3, (Object[]) o3);
    }

    static void writeAndAssertMixedArrays(Wire wire) {
        Object[] a1 = new Object[0];
        wire.write("empty").object(a1);
        Object[] a2 = {1L};
        wire.write("one").object(a2);
        Object[] a3 = {"Hello", 123, 10.1};
        wire.write("three").object(Object[].class, a3);

        assertMixedArraysRoundTrip(wire);
    }
}
