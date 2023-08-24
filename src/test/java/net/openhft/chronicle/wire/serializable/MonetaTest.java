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

public class MonetaTest extends net.openhft.chronicle.wire.WireTestCommon {
    @Test
    public void monetary() {
        SortedSet<NonScalarComparable> set = new TreeSet<>();
        for (String curr : "GBP,USD,EUR,AUD".split(","))
            set.add(new NonScalarComparable(Currency.getInstance(curr)));

        Wire wire = new TextWire(Bytes.allocateElasticOnHeap());
        wire.write("currencies")
                .object(set);
        SortedSet<NonScalarComparable> set2 = wire
                .read("currencies")
                .object(SortedSet.class);
        assertEquals(set, set2);
    }

    public static class NonScalarComparable implements Serializable, Comparable<NonScalarComparable> {
        final Currency currency;

        public NonScalarComparable(Currency currency) {
            this.currency = currency;
        }

        @Override
        public int compareTo(@NotNull MonetaTest.NonScalarComparable o) {
            return currency.getDisplayName().compareTo(o.currency.getDisplayName());
        }
    }
}
