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

final class ChronicleBitSetSupport {
    private ChronicleBitSetSupport() {
    }

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

    @Deprecated(/* to be removed in 2027 */)
    static IntStream stream(ChronicleBitSet bitSet, Runnable closeCheck) {
        closeCheck.run();

        class BitSetIterator implements PrimitiveIterator.OfInt {
            int next = bitSet.nextSetBit(0);

            @Override
            public boolean hasNext() {
                closeCheck.run();
                return next != -1;
            }

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
