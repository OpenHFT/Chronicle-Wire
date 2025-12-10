/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.OS;

import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * Support utilities for {@link ChronicleBitSet} implementations (equality, string representation,
 * and streaming). Isolated here to keep the core class lean while still reusing the shared logic.
 */
final class ChronicleBitSetSupport {
    /**
     * Utility holder; not instantiable.
     */
    private ChronicleBitSetSupport() {
    }

    /**
     * Compares two {@link ChronicleBitSet} instances for equality while honouring close checks.
     */
    static boolean equalsBitSet(ChronicleBitSet bitSet, Object obj, Runnable closeCheck) {
        closeCheck.run();

        if (!(obj instanceof ChronicleBitSet))
            return false;
        if (bitSet == obj)
            return true;

        ChronicleBitSet set = (ChronicleBitSet) obj;

        OS.memory().loadFence();

        for (int i = 0, max = Math.max(bitSet.getWordsInUse(), set.getWordsInUse()); i < max; i++)
            if (bitSet.getWord(i) != set.getWord(i))
                return false;

        return true;
    }

    /**
     * Builds a human-readable representation in the form {@code {0, 3, 4}}.
     */
    static String toString(ChronicleBitSet bitSet) {
        int numBits = Math.toIntExact((bitSet.getWordsInUse() > 128) ?
                bitSet.cardinality() : bitSet.getWordsInUse() * ChronicleBitSet.BITS_PER_WORD);
        StringBuilder b = new StringBuilder(6 * numBits + 2);
        b.append('{');

        int i = bitSet.nextSetBit(0);
        if (i != -1) {
            b.append(i);
            while (true) {
                if (++i < 0) break;
                if ((i = bitSet.nextSetBit(i)) < 0) break;
                int endOfRun = bitSet.nextClearBit(i);
                do {
                    b.append(", ").append(i);
                }
                while (++i != endOfRun);
            }
        }

        b.append('}');
        return b.toString();
    }

    /**
     * Streams the set bit indices with close checks on each iteration. Marked deprecated because
     * it exposes internal iteration details and will be removed in 2027.
     */
    @Deprecated(/* to be removed in 2027 */)
    static IntStream stream(ChronicleBitSet bitSet, Runnable closeCheck) {
        closeCheck.run();

        /**
         * Iterator that walks set bits, rechecking the close guard on each access.
         */
        class BitSetIterator implements PrimitiveIterator.OfInt {
            int next = bitSet.nextSetBit(0);

            /**
             * Returns whether any set bits remain.
             */
            @Override
            public boolean hasNext() {
                closeCheck.run();
                return next != -1;
            }

            /**
             * Returns the next set bit index or throws when none remain.
             */
            @Override
            public int nextInt() {
                closeCheck.run();
                if (next != -1) {
                    int ret = next;
                    next = bitSet.nextSetBit(next + 1);
                    return ret;
                } else {
                    throw new NoSuchElementException();
                }
            }
        }

        return StreamSupport.intStream(
                () -> Spliterators.spliterator(
                        new BitSetIterator(), bitSet.cardinality(),
                        Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.SORTED),
                Spliterator.SIZED | Spliterator.SUBSIZED |
                        Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.SORTED,
                false);
    }
}
