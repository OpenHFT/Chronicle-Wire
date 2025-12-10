/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.AbstractCloseable;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.threads.Pauser;

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.function.LongBinaryOperator;
import java.util.function.LongSupplier;

abstract class AbstractBitSetSupport extends AbstractCloseable {

    protected static final long WORD_MASK = ~0L;
    private transient Pauser pauser;

    protected Pauser pauser() {
        if (this.pauser == null)
            this.pauser = Pauser.busy();
        return this.pauser;
    }

    protected void casSet(LongValue word, long newValue) {
        throwExceptionIfClosed();

        Pauser internalPauser = pauser();
        internalPauser.reset();
        long oldValue = word.getVolatileValue();
        while (!word.compareAndSwapValue(oldValue, newValue)) {
            internalPauser.pause();
        }
    }

    protected void updateWithRetry(LongSupplier currentSupplier,
                                   LongBinaryOperator compute,
                                   long param,
                                   LongBiPredicate cas) {
        throwExceptionIfClosed();

        final Pauser internalPauser = pauser();
        internalPauser.reset();

        while (true) {
            long oldValue = currentSupplier.getAsLong();
            long value = compute.applyAsLong(oldValue, param);
            if (oldValue == value || cas.test(oldValue, value))
                break;
            internalPauser.pause();
        }
    }

    protected static void checkRange(int fromIndex, int toIndex) {
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        if (toIndex < 0)
            throw new IndexOutOfBoundsException("toIndex < 0: " + toIndex);
        if (fromIndex > toIndex)
            throw new IndexOutOfBoundsException("fromIndex: " + fromIndex +
                    " > toIndex: " + toIndex);
    }

    protected static BitSet valueOfBytes(byte[] bytes) {
        return BitSet.valueOf(ByteBuffer.wrap(bytes));
    }

    protected int toWordIndex(int bitIndex) {
        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        return (int) (bitIndex / ChronicleBitSet.BITS_PER_WORD);
    }

    protected abstract long wordBits(int wordIndex);

    protected abstract void setWordDirect(int wordIndex, long bits);

    protected abstract void ensureWordCapacity(int wordIndex);

    protected abstract int wordsInUse();

    protected void orWord(int wordIndex, long mask) {
        setWordDirect(wordIndex, wordBits(wordIndex) | mask);
    }

    protected void andWord(int wordIndex, long mask) {
        setWordDirect(wordIndex, wordBits(wordIndex) & mask);
    }

    protected void fillWordFully(int wordIndex) {
        setWordDirect(wordIndex, WORD_MASK);
    }

    protected void clearWordFully(int wordIndex) {
        setWordDirect(wordIndex, 0L);
    }

    protected void setRange(int fromIndex, int toIndex, boolean value) {
        throwExceptionIfClosed();
        checkRange(fromIndex, toIndex);
        if (fromIndex == toIndex)
            return;

        int startWordIndex = toWordIndex(fromIndex);
        int endWordIndex = toWordIndex(toIndex - 1);
        if (value) {
            ensureWordCapacity(endWordIndex);
        } else {
            int currentWords = wordsInUse();
            if (startWordIndex >= currentWords)
                return;
            if (endWordIndex >= currentWords) {
                toIndex = Math.min(toIndex, length());
                if (toIndex <= fromIndex)
                    return;
                endWordIndex = toWordIndex(toIndex - 1);
            }
        }

        long firstWordMask = WORD_MASK << fromIndex;
        long lastWordMask = WORD_MASK >>> -toIndex;

        if (startWordIndex == endWordIndex) {
            applyMask(startWordIndex, firstWordMask & lastWordMask, value);
            return;
        }

        applyMask(startWordIndex, firstWordMask, value);

        for (int i = startWordIndex + 1; i < endWordIndex; i++) {
            if (value)
                fillWordFully(i);
            else
                clearWordFully(i);
        }

        applyMask(endWordIndex, lastWordMask, value);
    }

    private void applyMask(int wordIndex, long mask, boolean value) {
        if (mask == 0)
            return;
        if (value)
            orWord(wordIndex, mask);
        else
            andWord(wordIndex, ~mask);
    }

    @FunctionalInterface
    protected interface LongBiPredicate {
        boolean test(long expected, long value);
    }
}
