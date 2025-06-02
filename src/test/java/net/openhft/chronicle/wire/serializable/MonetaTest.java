/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire.serializable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.Serializable;
import java.util.Currency;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("unchecked")
public class MonetaTest extends net.openhft.chronicle.wire.WireTestCommon {
    // Test method for serialization and deserialization of a SortedSet with custom Comparable objects
    @Test
    public void monetary() {
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
    public static class NonScalarComparable implements Serializable, Comparable<NonScalarComparable> {
        private static final long serialVersionUID = 0L;
        // Currency instance
        final Currency currency;

        // Constructor accepting a Currency instance
        public NonScalarComparable(Currency currency) {
            this.currency = currency;
        }

        // Overriding compareTo method for comparison based on currency display name
        @Override
        public int compareTo(@NotNull MonetaTest.NonScalarComparable o) {
            return currency.getDisplayName().compareTo(o.currency.getDisplayName());
        }
    }
}
