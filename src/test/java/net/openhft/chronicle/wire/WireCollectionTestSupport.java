/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"unchecked", "deprecation", "rawtypes"})
final class WireCollectionTestSupport {
    private WireCollectionTestSupport() {
    }

    static void assertStringArraysRoundTrip(Supplier<Wire> wireSupplier) {
        Wire wire = wireSupplier.get();
        @NotNull String[] noObjects = {};
        wire.write().object(noObjects);

        @NotNull String[] object = wire.read().object(String[].class);
        assertEquals(0, object.length);

        wire = wireSupplier.get();
        @NotNull String[] threeObjects = {"abc", "def", "ghi"};
        wire.write().object(threeObjects);

        @NotNull String[] object2 = wire.read().object(String[].class);
        assertEquals(3, object2.length);
        assertEquals("[abc, def, ghi]", Arrays.toString(object2));
    }

    static void assertStringListRoundTrip(Supplier<Wire> wireSupplier) {
        Wire wire = wireSupplier.get();
        @NotNull List<String> noObjects = new ArrayList<>();
        wire.write().object(noObjects);

        @NotNull List<String> list = wire.read().object(List.class);
        assertEquals(0, list.size());

        wire = wireSupplier.get();
        @NotNull List<String> threeObjects = Arrays.asList("abc", "def", "ghi");
        wire.write().object(threeObjects);

        @NotNull List<String> list2 = wire.read().object(List.class);
        assertEquals(3, list2.size());
        assertEquals("[abc, def, ghi]", list2.toString());
    }

    static void assertStringSetRoundTrip(Supplier<Wire> wireSupplier) {
        Wire wire = wireSupplier.get();
        @NotNull Set<String> noObjects = new HashSet<>();
        wire.write().object(noObjects);

        @NotNull Set<String> list = wire.read().object(Set.class);
        assertEquals(0, list.size());

        wire = wireSupplier.get();
        @NotNull Set<String> threeObjects = new HashSet<>(Arrays.asList("abc", "def", "ghi"));
        wire.write().object(threeObjects);

        @NotNull Set<String> list2 = wire.read().object(Set.class);
        assertEquals(3, list2.size());
        assertEquals("[abc, def, ghi]", list2.toString());
    }
}
