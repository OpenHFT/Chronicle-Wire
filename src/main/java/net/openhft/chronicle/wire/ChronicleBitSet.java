/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.Closeable;

/**
 * Interface for a bit set that is both {@link Marshallable} and
 * {@link Closeable}. Implementations may store state off-heap so that bits can
 * be persisted or shared between processes. Examples include
 * {@link LongValueBitSet} and {@link LongArrayValueBitSet}.
 */
public interface ChronicleBitSet extends Marshallable, Closeable {

    /** The number of bits in a {@code long} word (64). */
    long BITS_PER_WORD = Long.BYTES * 8L;

    /**
     * Returns the number of bits of space actually in use by this {@code ChronicleBitSet} to represent bit values.
     * The maximum element in the set is the size - 1st element.
     *
     * @return the current capacity of this bit set
     */
    int size();

    /**
     * Sets the bit at the given zero-based index.
     *
     * @param bitIndex index of the bit to set
     * @throws IndexOutOfBoundsException if {@code bitIndex < 0}
     */
    void set(int bitIndex);

    /**
     * Sets the bit at the given index to the provided value.
     *
     * @param bitIndex zero-based index of the bit to modify
     * @param value    the value to store at that index
     * @throws IndexOutOfBoundsException if {@code bitIndex < 0}
     */
    default void set(int bitIndex, boolean value) {
        if (value)
            set(bitIndex);
        else
            clear(bitIndex);
    }

    /**
     * Sets every bit in the range {@code [fromIndex, toIndex)} to
     * {@code true}.
     *
     * @param fromIndex index of the first bit to set, zero-based
     * @param toIndex   index after the last bit to set
     * @throws IndexOutOfBoundsException if an index is negative or the range
     *                                   is invalid
     */
    void set(int fromIndex, int toIndex);

    /**
     * Sets each bit in the range {@code [fromIndex, toIndex)} to the given value.
     *
     * @param fromIndex zero-based index of the first bit to modify
     * @param toIndex   index after the last bit to modify
     * @param value     value to store in the selected range
     * @throws IndexOutOfBoundsException if an index is negative or the range is invalid
     */
    default void set(int fromIndex, int toIndex, boolean value) {
        if (value)
            set(fromIndex, toIndex);
        else
            clear(fromIndex, toIndex);
    }

    /**
     * Returns the value of the bit at the given zero-based index.
     *
     * @param bitIndex index of the bit to query
     * @return {@code true} if the bit is set
     * @throws IndexOutOfBoundsException if {@code bitIndex < 0}
     */
    boolean get(int bitIndex);

    /**
     * Clears the bit at the given zero-based index.
     *
     * @param bitIndex index of the bit to clear
     * @throws IndexOutOfBoundsException if {@code bitIndex < 0}
     */
    void clear(int bitIndex);

    /**
     * Finds the index of the next set bit between {@code fromIndex} and
     * {@code toIndex} inclusive.
     *
     * <p>To iterate over the {@code true} bits in a {@code ChronicleBitSet},
     * use the following loop:
     *
     * <pre> {@code
     * for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1,to)) {
     *     // operate on index i here
     *     if (i == Integer.MAX_VALUE) {
     *         break; // or (i+1) would overflow
     *     }
     * }}</pre>
     *
     * @param fromIndex zero-based index to begin searching (inclusive)
     * @param toIndex   maximum index to examine (inclusive)
     * @return index of the next set bit or {@code -1} if none is found
     * @throws IndexOutOfBoundsException if an index is negative
     */
    int nextSetBit(int fromIndex, int toIndex);

    /**
     * Finds the next set bit at or after {@code fromIndex}.
     *
     * @param fromIndex zero-based index to begin searching
     * @return index of the next set bit or {@code -1} if none is found
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}
     */
    int nextSetBit(int fromIndex);

    /**
     * Sets all the bits in this ChronicleBitSet to {@code false}.
     */
    void clear();

    /**
     * Tests whether no bits are currently set.
     *
     * @return {@code true} if all words are zero
     */
    default boolean isEmpty() {
        final int wordsInUse = getWordsInUse();
        for (int i = 0; i < wordsInUse; i++)
            if (getWord(i) != 0)
                return false;
        return true;
    }

    /**
     * Returns the "logical size" of this {@code ChronicleBitSet}: the index of the highest set bit in the {@code ChronicleBitSet} plus one.
     * Returns zero if the {@code ChronicleBitSet} contains no set bits.
     *
     * @return the logical size of this {@code ChronicleBitSet}
     */
    default int length() {
        for (int i = getWordsInUse() - 1; i >= 0; i--) {
            long word = getWord(i);
            if (word != 0) {
                return Math.toIntExact(BITS_PER_WORD * (i + 1) - Long.numberOfLeadingZeros(word));
            }
        }
        return 0;
    }

    /**
     * Counts the number of bits set to {@code true}.
     *
     * @return number of set bits
     */
    int cardinality();

    /**
     * Returns the index of the next clear bit at or after {@code index}.
     *
     * @param index zero-based starting position
     * @return index of the next clear bit or {@code -1} if none is found
     * @throws IndexOutOfBoundsException if {@code index < 0}
     */
    int nextClearBit(int index);

    /**
     * Flips the state of the bit at {@code index}.
     *
     * @param index zero-based index of the bit to flip
     * @throws IndexOutOfBoundsException if {@code index < 0}
     */
    void flip(int index);

    /**
     * Toggles all bits in the range {@code [fromIndex, toIndex)}.
     *
     * @param fromIndex zero-based start of the range
     * @param toIndex   end of the range (exclusive)
     * @throws IndexOutOfBoundsException if the range is invalid
     */
    void flip(int fromIndex, int toIndex);

    /**
     * Clears all bits in the range {@code [fromIndex, toIndex)}.
     *
     * @param fromIndex zero-based start of the range
     * @param toIndex   end of the range (exclusive)
     * @throws IndexOutOfBoundsException if the range is invalid
     */
    void clear(int fromIndex, int toIndex);

    /**
     * Returns the number of {@code long} words currently used to hold the bits.
     * Trailing zero words are not counted.
     *
     * @return number of words in use
     */
    int getWordsInUse();

    /**
     * Fetches the word at {@code wordIndex} from the internal representation.
     *
     * @param wordIndex zero-based index of the word
     * @return the word value or {@code 0} if out of range
     */
    long getWord(int wordIndex);

    /**
     * Sets the word at {@code wordIndex} to {@code bits}. Implementations may
     * extend the storage to accommodate the index.
     *
     * @param wordIndex zero-based index of the word to modify
     * @param bits      the 64 bits to store
     */
    void setWord(int wordIndex, long bits);

    /**
     * Clears the bits that are set in the given set from this set.
     *
     * @param bitSet bit set defining the bits to clear
     */
    void andNot(ChronicleBitSet bitSet);

    /**
     * Applies a logical AND with the given set, clearing bits not present in both.
     *
     * @param bitSet other bit set
     */
    void and(ChronicleBitSet bitSet);

    /**
     * Performs a logical XOR with the given set.
     *
     * @param bitSet other bit set
     */
    void xor(ChronicleBitSet bitSet);

    /**
     * Applies a logical OR with the given set, setting bits present in either set.
     *
     * @param bitSet other bit set
     */
    void or(ChronicleBitSet bitSet);

    /**
     * Checks whether any bit is set in both this set and {@code bitSet}.
     *
     * @param bitSet other bit set
     * @return {@code true} if the sets share a common set bit
     */
    boolean intersects(ChronicleBitSet bitSet);

    /**
     * Copies all bits from {@code bitSet} into this set.
     *
     * @param bitSet source bit set
     */
    void copyFrom(ChronicleBitSet bitSet);
}
