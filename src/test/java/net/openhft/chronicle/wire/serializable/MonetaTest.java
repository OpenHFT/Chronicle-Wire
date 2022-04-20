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

public class MonetaTest {
    @Test
    public void monetary() {
        SortedSet<NonScalarComparable> set = new TreeSet<>();
        for (String curr : "GBP,USD,EUR,AUD".split(","))
            set.add(new NonScalarComparable(Currency.getInstance(curr)));

        Wire wire = new TextWire(Bytes.allocateElasticOnHeap());
        wire.write("currencies")
                .object(set);
//        System.out.println(wire);
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
