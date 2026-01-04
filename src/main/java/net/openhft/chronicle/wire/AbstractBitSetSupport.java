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

/**
 * Shared low-level operations used by Chronicle bitset implementations. Provides checked range
 * helpers and retrying CAS operations while honouring {@link AbstractCloseable#throwExceptionIfClosed()}.
 */
abstract class AbstractBitSetSupport extends AbstractCloseable {

    /**
     * Mask for a fully set 64-bit word.
     */
    protected static final long WORD_MASK = ~0L;
    private transient Pauser pauser;

    /**
     * Lazily builds a pauser for spin-wait loops used during CAS retries.
     *
     * @return busy pauser reused across operations
     */
    protected Pauser pauser() {
        if (this.pauser == null)
            this.pauser = Pauser.busy();
        return this.pauser;
    }

    /**
     * CAS-writes a single word, pausing and retrying until success.
     *
     * @param word     target word reference
     * @param newValue value to install
     */
    protected void casSet(LongValue word, long newValue) {
        throwExceptionIfClosed();

        Pauser internalPauser = pauser();
        internalPauser.reset();
        long oldValue = word.getVolatileValue();
        while (!word.compareAndSwapValue(oldValue, newValue)) {
            internalPauser.pause();
        }
    }

    /**
     * Generic compare-and-swap loop that computes a new value and retries until the supplied CAS
     * function succeeds or the computed value is unchanged.
     *
     * @param currentSupplier supplies the current value
     * @param compute         computes a candidate new value
     * @param param           parameter passed to {@code compute}
     * @param cas             CAS function that attempts to apply the new value
     */
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

    /**
     * Validates that the bit index range is non-negative and ordered.
     *
     * @param fromIndex inclusive start bit
     * @param toIndex   exclusive end bit
     */
    protected static void checkRange(int fromIndex, int toIndex) {
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        if (toIndex < 0)
            throw new IndexOutOfBoundsException("toIndex < 0: " + toIndex);
        if (fromIndex > toIndex)
            throw new IndexOutOfBoundsException("fromIndex: " + fromIndex +
                    " > toIndex: " + toIndex);
    }

    /**
     * Wraps the given bytes into a {@link BitSet} assuming little-endian layout.
     *
     * @param bytes source bytes
     * @return decoded bit set
     */
    protected static BitSet valueOfBytes(byte[] bytes) {
        return BitSet.valueOf(ByteBuffer.wrap(bytes));
    }

    /**
     * Converts a bit index to the backing word index with bounds checking.
     *
     * @param bitIndex bit index to translate
     * @return word index
     */
    protected int toWordIndex(int bitIndex) {
        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
        return (int) (bitIndex / ChronicleBitSet.BITS_PER_WORD);
    }

    /**
     * Returns the current bits for a backing word.
     *
     * @param wordIndex index of the word to read
     * @return bits for that word
     */
    protected abstract long wordBits(int wordIndex);

    /**
     * Writes a backing word directly without extra checks.
     *
     * @param wordIndex index to update
     * @param bits      new word bits
     */
    protected abstract void setWordDirect(int wordIndex, long bits);

    /**
     * Ensures the storage can hold at least {@code wordIndex + 1} words.
     *
     * @param wordIndex highest word index needed
     */
    protected abstract void ensureWordCapacity(int wordIndex);

    /**
     * Returns the number of active words used by the bitset.
     *
     * @return count of populated words
     */
    protected abstract int wordsInUse();

    /**
     * ORs a mask into the given word.
     *
     * @param wordIndex index of the word to update
     * @param mask      bits to OR in
     */
    protected void orWord(int wordIndex, long mask) {
        setWordDirect(wordIndex, wordBits(wordIndex) | mask);
    }

    /**
     * ANDs a mask with the given word.
     *
     * @param wordIndex index of the word to update
     * @param mask      bits to retain
     */
    protected void andWord(int wordIndex, long mask) {
        setWordDirect(wordIndex, wordBits(wordIndex) & mask);
    }

    /**
     * Fills the target word with all ones.
     *
     * @param wordIndex index of the word to fill
     */
    protected void fillWordFully(int wordIndex) {
        setWordDirect(wordIndex, WORD_MASK);
    }

    /**
     * Clears all bits in the target word.
     *
     * @param wordIndex index of the word to clear
     */
    protected void clearWordFully(int wordIndex) {
        setWordDirect(wordIndex, 0L);
    }

    /**
     * Sets or clears a contiguous bit range, expanding storage on demand when setting.
     *
     * @param fromIndex first bit index (inclusive)
     * @param toIndex   last bit index (exclusive)
     * @param length    total logical length allowed
     * @param value     {@code true} to set bits, {@code false} to clear
     */
    protected void setRange(int fromIndex, int toIndex, int length, boolean value) {
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
                toIndex = Math.min(toIndex, length);
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

    /**
     * Applies a mask to a word either by OR or AND.
     */
    private void applyMask(int wordIndex, long mask, boolean value) {
        if (mask == 0)
            return;
        if (value)
            orWord(wordIndex, mask);
        else
            andWord(wordIndex, ~mask);
    }

    /**
     * Functional form of a CAS to allow lambdas without extra allocations.
     */
    @FunctionalInterface
    protected interface LongBiPredicate {
        boolean test(long expected, long value);
    }
}
