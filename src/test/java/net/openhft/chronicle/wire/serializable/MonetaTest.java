/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.serializable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Currency;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings("unchecked")
public class MonetaTest extends net.openhft.chronicle.wire.WireTestCommon {
    // Test method for serialization and deserialization of a SortedSet with custom Comparable objects
    @Test
    public void monetary() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        // Create a sorted set of NonScalarComparable objects
        SortedSet<NonScalarComparable> set = new TreeSet<>();
        // Add currency instances to the set
        for (String curr : "GBP,USD,EUR,AUD".split(","))
            set.add(new NonScalarComparable(Currency.getInstance(curr)));

        // Initialize a TextWire with allocated elastic heap memory
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap());
        // Write the set of NonScalarComparable objects to the wire
        wire.write("currencies")
                .object(set);

        // Read the set back from the wire
        SortedSet<NonScalarComparable> set2 = wire
                .read("currencies")
                .object(SortedSet.class);

        // Assert that the original set and the set read from the wire are equal

        assertEquals(set, set2);
    }

    // Inner class representing a non-scalar comparable object
    static class NonScalarComparable implements Serializable, Comparable<NonScalarComparable> {
        private static final long serialVersionUID = 0L;
        // Currency instance
        final Currency currency;

        // Constructor accepting a Currency instance
        NonScalarComparable(Currency currency) {
            this.currency = currency;
        }

        // Overriding compareTo method for comparison based on currency display name
        @Override
        public int compareTo(@NotNull MonetaTest.NonScalarComparable o) {
            return currency.getDisplayName().compareTo(o.currency.getDisplayName());
        }
    }
}
