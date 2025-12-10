/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.ref.BinaryLongArrayReference;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.threads.Pauser;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.IntStream;

import static net.openhft.chronicle.core.io.Closeable.closeQuietly;

/**
 * A {@link ChronicleBitSet} backed by a {@link net.openhft.chronicle.core.values.LongArrayValues}.
 * The backing array can reside off-heap, allowing multiple processes to share the same state.
 * All updates rely on compare‑and‑swap operations and therefore do not require explicit locks.
 * <b>Note:</b> the capacity is fixed when the instance is created and cannot later be expanded.
 */
@SuppressWarnings("this-escape")
public class LongArrayValueBitSet extends AbstractBitSetSupport implements Marshallable, ChronicleBitSet {

    // Holds the 64-bit words representing the bits. Each index is one word in the underlying {@link LongArrayValues} instance
    private LongArrayValues words;

    /**
     * Create a bit set capable of holding {@code maxNumberOfBits} bits.
     * The backing {@link BinaryLongArrayReference} is sized once during construction.
     */
    public LongArrayValueBitSet(final long maxNumberOfBits) {
        words = new BinaryLongArrayReference((maxNumberOfBits + BITS_PER_WORD - 1) / BITS_PER_WORD);
        singleThreadedCheckDisabled(true);
    }

    /**
     * Create a bit set of {@code maxNumberOfBits} bits and immediately marshal its state
     * to and from the provided {@link Wire}. The {@link #words} field is initialised using
     * a {@link BinaryLongArrayReference} before marshalling occurs.
     */
    public LongArrayValueBitSet(final long maxNumberOfBits, Wire w) {
        this(maxNumberOfBits);
        writeMarshallable(w);
        readMarshallable(w);
    }

    /**
     * Constructs and returns a new {@code BitSet} using the bits from the provided byte array.
     *
     * @param bytes The byte array to be used for constructing the {@code BitSet}.
     * @return A new {@code BitSet} containing all bits from the given byte array.
     */
    @Deprecated(/* to be removed in 2027 */)
    public static BitSet valueOf(byte[] bytes) {
        return valueOfBytes(bytes);
    }

    /**
     * Return the value of the word at {@code wordIndex} or zero if beyond {@link #getWordsInUse()}.
     */
    @Override
    public long getWord(int wordIndex) {
        return wordIndex < getWordsInUse() ? words.getValueAt(wordIndex) : 0;
    }

    /**
     * Store {@code bits} at the given {@code wordIndex}. The backing {@link LongArrayValues}
     * is expanded to the index if needed.
     */
    @Override
    public void setWord(int wordIndex, long bits) {
        expandTo(wordIndex);
        words.setValueAt(wordIndex, bits);
    }

    @Override
    protected long wordBits(int wordIndex) {
        return getWord(wordIndex);
    }

    @Override
    protected void setWordDirect(int wordIndex, long bits) {
        setWord(wordIndex, bits);
    }

    @Override
    protected void ensureWordCapacity(int wordIndex) {
        expandTo(wordIndex);
    }

    @Override
    protected int wordsInUse() {
        return getWordsInUse();
    }

    @Override
    protected void orWord(int wordIndex, long mask) {
        pipe(wordIndex, mask);
    }

    @Override
    protected void andWord(int wordIndex, long mask) {
        and(wordIndex, mask);
    }

    @Override
    protected void performClose() {
        closeQuietly(words);
    }

    /**
     * Retrieves the number of words that are currently in use by this bit set.
     * This indicates the number of long values that are currently utilized to represent bits in the bit set.
     *
     * @return The number of words in use, converted to an integer.
     */
    @Override
    public int getWordsInUse() {
        return Math.toIntExact(words.getUsed());
    }

    /**
     * Sets a specific word in this bit set using a provided function and parameter.
     * This method is lock-free and uses CAS operations to safely update the word value.
     *
     * @param wordIndex The index of the word to set.
     * @param param     The parameter to pass to the function.
     * @param function  The function to compute the new word value based on the old value and the provided parameter.
     */
    public void set(int wordIndex, long param, LongFunction function) {
        throwExceptionIfClosed();
        expandTo(wordIndex);

        final Pauser internalPauser = pauser();
        internalPauser.reset();
        for (; ; ) {
            final long oldValue = words.getVolatileValueAt(wordIndex);
            final long value = function.apply(oldValue, param);
            if (oldValue == value || words.compareAndSet(wordIndex, oldValue, value))
                break;
            internalPauser.pause();
        }
    }

    /**
     * Sets a new value for a given word in this bit set.
     * This method is lock-free and uses CAS operations to safely set the new word value.
     *
     * @param word     The {@code LongValue} instance representing the word to set.
     * @param newValue The new value to set for the word.
     */
    @Deprecated(/* to be removed in 2027 */)
    public void set(LongValue word, long newValue) {
        casSet(word, newValue);
    }
    @Deprecated(/* to be removed in 2027 */)
    public byte[] toByteArray() {
        throwExceptionIfClosed();

        int n = Math.toIntExact(getWordsInUse());
        if (n == 0)
            return new byte[0];
        Bytes<byte[]> bytes = Bytes.allocateElasticOnHeap(Math.toIntExact(n * 8L));
        bytes.writeLong(words.getVolatileValueAt(0));
        for (int i = 1; i < n; i++)
            bytes.writeLong(words.getValueAt(i));
        return bytes.underlyingObject();
    }

    /**
     * Ensures that the ChronicleBitSet can accommodate a given wordIndex.
     *
     * @param wordIndex the index to be accommodated.
     */
    private void expandTo(int wordIndex) {
        int wordsRequired = wordIndex + 1;
        final long capacity = words.getCapacity();
        if (wordsRequired <= capacity) {
            words.setMaxUsed(wordsRequired);
        } else if (wordsRequired > capacity) {
            throw new UnsupportedOperationException("todo: it is not possible currently to expand " +
                    "this structure, because if its concurrent nature and have to implement cross " +
                    "process locking capacity: " + capacity + ", wordIndex: " + wordIndex);
        }
    }

    /**
     * Flips the bit at the specified index, turning it to its opposite value (0 to 1, or 1 to 0).
     *
     * @param bitIndex The index of the bit to flip.
     */
    @Override
    public void flip(int bitIndex) {
        throwExceptionIfClosed();

        int wordIndex = toWordIndex(bitIndex);
        expandTo(wordIndex);
        caret(wordIndex, 1L << bitIndex);
    }

    /**
     * Performs a bitwise XOR operation on the specified word using the given parameter.
     * The result replaces the word's current value.
     *
     * @param wordIndex The index of the word to modify.
     * @param param     The long value to use in the XOR operation.
     */
    private void caret(int wordIndex, long param) {
        set(wordIndex, param, (x, y) -> x ^ y);
    }

    /**
     * Performs a bitwise AND operation on the specified word using the given parameter.
     * The result replaces the word's current value.
     *
     * @param wordIndex The index of the word to modify.
     * @param param     The long value to use in the AND operation.
     */
    private void and(int wordIndex, final long param) {
        set(wordIndex, param, (x, y) -> x & y);
    }

    /**
     * Flips the bits from a starting index to an ending index.
     * Each bit in the specified range will be turned to its opposite value.
     *
     * @param fromIndex The starting index of the range, inclusive.
     * @param toIndex   The ending index of the range, exclusive.
     */
    @Override
    public void flip(int fromIndex, int toIndex) {
        throwExceptionIfClosed();

        checkRange(fromIndex, toIndex);

        if (fromIndex == toIndex)
            return;

        int startWordIndex = toWordIndex(fromIndex);
        int endWordIndex = toWordIndex(toIndex - 1);
        expandTo(endWordIndex);

        long firstWordMask = WORD_MASK << fromIndex;
        long lastWordMask = WORD_MASK >>> -toIndex;
        if (startWordIndex == endWordIndex) {
            // Case 1: One word
            caret(startWordIndex, firstWordMask & lastWordMask);
        } else {
            // Case 2: Multiple words
            // Handle first word
            caret(startWordIndex, firstWordMask);

            // Handle intermediate words, if any
            for (int i = startWordIndex + 1; i < endWordIndex; i++)
                caret(i, WORD_MASK);

            // Handle last word
            caret(endWordIndex, lastWordMask);
        }
    }

    /**
     * Sets the bit at the specified index to {@code true}.
     *
     * @param bitIndex The index of the bit to be set.
     */
    @Override
    public void set(int bitIndex) {
        throwExceptionIfClosed();

        int wordIndex = toWordIndex(bitIndex);

        pipe(wordIndex, (1L << bitIndex)); // Activates the bit at the specified index
    }

    /**
     * Performs a bitwise OR operation on the specified word using the provided parameter.
     * This operation effectively sets specific bits to {@code true}.
     *
     * @param wordIndex The index of the word to modify.
     * @param param     The long value to use in the OR operation.
     */
    private void pipe(int wordIndex, long param) {
        set(wordIndex, param, (x, y) -> x | y);
    }

    /**
     * Sets or clears the bit at the specified index based on the provided boolean value.
     *
     * @param bitIndex The index of the bit to be modified.
     * @param value    If {@code true}, the bit will be set; if {@code false}, the bit will be cleared.
     */
    @Override
    public void set(int bitIndex, boolean value) {
        throwExceptionIfClosed();

        if (value)
            set(bitIndex);
        else
            clear(bitIndex);
    }

    /**
     * Sets all bits in the specified range to {@code true}.
     *
     * @param fromIndex The starting index of the range, inclusive.
     * @param toIndex   The ending index of the range, exclusive.
     */
    @Override
    public void set(int fromIndex, int toIndex) {
        setRange(fromIndex, toIndex, length(), true);
    }

    /**
     * Clears (sets to {@code false}) the bit specified by the index.
     * If the bit index is beyond the current words in use, the operation is ignored.
     *
     * @param bitIndex The index of the bit to be cleared.
     */
    @Override
    public void clear(int bitIndex) {
        throwExceptionIfClosed();

        int wordIndex = toWordIndex(bitIndex);

        // If the wordIndex is beyond current words in use, there's nothing to clear
        if (wordIndex >= getWordsInUse())
            return;

        // AND operation with the complement of the bit set to clear the specified bit
        and(wordIndex, ~(1L << bitIndex));
    }

    /**
     * Clears (sets to {@code false}) the range of bits from the specified starting index (inclusive) to the ending index (exclusive).
     * The method adjusts ranges if necessary and ensures efficient clearing even for larger ranges.
     *
     * @param fromIndex Starting index of the range (inclusive).
     * @param toIndex Ending index of the range (exclusive).
     */
    @Override
    public void clear(int fromIndex, int toIndex) {
        setRange(fromIndex, toIndex, length(), false);
    }

    /**
     * Sets all bits in this ChronicleBitSet to {@code false}.
     * Post invocation, the ChronicleBitSet is effectively empty with no bits set to {@code true}.
     */
    @Override
    public void clear() {
        throwExceptionIfClosed();

        int value = Math.toIntExact(getWordsInUse());

        // Iterate and set each word to zero
        while (value > 0)
            words.setValueAt(--value, 0);

        // Reset the number of words in use
        words.setUsed(0);
    }

    /**
     * Returns the value of the bit with the specified index. The value is {@code true} if the bit with the index {@code bitIndex} is currently set in
     * this {@code ChronicleBitSet}; otherwise, the result is {@code false}.
     *
     * @param bitIndex the bit index
     * @return the value of the bit with the specified index
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    @Override
    public boolean get(int bitIndex) {
        throwExceptionIfClosed();

        int wordIndex = toWordIndex(bitIndex);

        // Check if the bit at the specified index is set and within the current words in use
        return (wordIndex < getWordsInUse())
                && ((words.getValueAt(wordIndex) & (1L << bitIndex)) != 0);
    }

    /**
     * Finds and returns the index of the first bit that is set to {@code true} after the specified starting index.
     * If no such bit exists then it returns -1.
     *
     * @param fromIndex The index to start checking from (inclusive).
     * @return Index of the next set bit, or -1 if there's no set bit found.
     */
    @Override
    public int nextSetBit(int fromIndex) {
        throwExceptionIfClosed();

        int u = toWordIndex(fromIndex);

        // If the word index is beyond the current words in use, no set bit exists
        if (u >= getWordsInUse())
            return -1;

        // Create a mask to filter out bits before the fromIndex
        long word = words.getVolatileValueAt(u) & (WORD_MASK << fromIndex);

        while (true) {
            if (word != 0)
                // Use Long's numberOfTrailingZeros to quickly find the next set bit in the current word
                return Math.toIntExact((u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word));
            if (++u == getWordsInUse())
                return -1;
            word = words.getValueAt(u);
        }
    }

    /**
     * Retrieves the index of the first bit set to {@code true} that occurs on or after the specified starting index
     * and before the specified ending index. If no such bit exists within the specified range, it returns {@code -1}.
     *
     * @param fromIndex The index to start checking from (inclusive).
     * @param toIndex The index to stop checking at (exclusive).
     * @return Index of the next set bit in the specified range, or {@code -1} if there's no set bit found.
     */
    @Override
    public int nextSetBit(int fromIndex, int toIndex) {
        throwExceptionIfClosed();

        int u = toWordIndex(fromIndex);

        // If the word index is beyond the current words in use, no set bit exists
        if (u >= getWordsInUse())
            return -1;

        // Create a mask to filter out bits before the fromIndex
        long word = words.getVolatileValueAt(u) & (WORD_MASK << fromIndex);

        while (true) {
            if (word != 0)
                // Use Long's numberOfTrailingZeros to quickly find the next set bit in the current word
                return Math.toIntExact((u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word));
            if (++u == getWordsInUse())
                return -1;
            if (u * BITS_PER_WORD > toIndex)
                return -1;
            word = words.getValueAt(u);
        }
    }

    /**
     * Retrieves the index of the first bit set to {@code false} that occurs on or after the specified starting index.
     * If all bits are set to {@code true} after the specified index, it returns the length of this ChronicleBitSet.
     *
     * @param fromIndex The index to start checking from (inclusive).
     * @return Index of the next clear bit or the length of this ChronicleBitSet if no clear bit is found.
     */
    @Override
    public int nextClearBit(int fromIndex) {
        throwExceptionIfClosed();

        int u = toWordIndex(fromIndex);

        // If the word index is beyond the current words in use, return the fromIndex as no bit is set
        if (u >= getWordsInUse())
            return fromIndex;

        // Complement the word to find clear bits and create a mask for bits after the fromIndex
        long word = ~words.getVolatileValueAt(u) & (WORD_MASK << fromIndex);

        while (true) {
            if (word != 0)
                // Use Long's numberOfTrailingZeros to quickly find the next clear bit in the current word
                return Math.toIntExact((u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word));
            if (++u == getWordsInUse())
                // TODO Overflows to MIN_VALUE
                return (int) (getWordsInUse() * BITS_PER_WORD);
            word = ~words.getValueAt(u);
        }
    }

    /**
     * Identifies the closest bit set to {@code true} that occurs on or before the given starting index.
     * If no such bit exists or if {@code -1} is the starting index, the method returns {@code -1}.
     *
     * @param fromIndex The index to start the reverse search from (inclusive).
     * @return Index of the previous set bit or {@code -1} if no set bit is found.
     */
    @Deprecated(/* to be removed in 2027, as it is only used in tests */)
    public int previousSetBit(int fromIndex) {
        throwExceptionIfClosed();

        // Special case for -1, to return -1 as specified
        if (fromIndex < 0) {
            if (fromIndex == -1)
                return -1;
            throw new IndexOutOfBoundsException(
                    "fromIndex < -1: " + fromIndex);
        }

        int u = toWordIndex(fromIndex);

        // If the word index surpasses the number of words currently in use
        if (u >= getWordsInUse())
            return length() - 1;

        // Create a mask to filter out bits after the fromIndex
        long word = words.getVolatileValueAt(u) & (WORD_MASK >>> -(fromIndex + 1));

        while (true) {
            if (word != 0)
                // Utilize Long's numberOfLeadingZeros to swiftly identify the previous set bit in the current word
                return Math.toIntExact((u + 1) * BITS_PER_WORD - 1 - Long.numberOfLeadingZeros(word));
            if (u-- == 0)
                return -1;
            word = words.getValueAt(u);
        }
    }

    /**
     * Identifies the closest bit set to {@code false} that occurs on or before the given starting index.
     * If no such bit exists or if {@code -1} is the starting index, the method returns {@code -1}.
     *
     * @param fromIndex The index to start the reverse search from (inclusive).
     * @return Index of the previous clear bit or {@code -1} if no clear bit is found.
     */
    @Deprecated(/* to be removed in 2027 */)
    public int previousClearBit(int fromIndex) {
        throwExceptionIfClosed();

        // Special case for -1, to return -1 as specified
        if (fromIndex < 0) {
            if (fromIndex == -1)
                return -1;
            throw new IndexOutOfBoundsException(
                    "fromIndex < -1: " + fromIndex);
        }

        int u = toWordIndex(fromIndex);

        // If the word index surpasses the number of words currently in use
        if (u >= getWordsInUse())
            return fromIndex;

        // Complement the word to search for clear bits and apply a mask for bits after the fromIndex
        long word = ~words.getVolatileValueAt(u) & (WORD_MASK >>> -(fromIndex + 1));

        while (true) {
            if (word != 0)
                // Utilize Long's numberOfLeadingZeros to swiftly identify the previous clear bit in the current word
                return Math.toIntExact((u + 1) * BITS_PER_WORD - 1 - Long.numberOfLeadingZeros(word));
            if (u-- == 0)
                return -1;
            word = ~words.getValueAt(u);
        }
    }

    /**
     * Determines if there's any overlapping bit set to {@code true} between this {@code ChronicleBitSet} and the given {@code ChronicleBitSet}.
     *
     * @param set The {@code ChronicleBitSet} to compare against.
     * @return {@code true} if there's an intersecting bit set to {@code true} between the two sets, {@code false} otherwise.
     */
    @Override
    public boolean intersects(ChronicleBitSet set) {
        throwExceptionIfClosed();

        // Loop through words of both bitsets to check for intersection
        for (int i = Math.min(getWordsInUse(), set.getWordsInUse()) - 1; i >= 0; i--)
            if ((words.getVolatileValueAt(i) & set.getWord(i)) != 0)
                return true;  // Intersection found
        return false;  // No intersection found
    }

    /**
     * Computes the number of bits that are currently set to {@code true} in this {@code ChronicleBitSet}.
     *
     * @return The count of bits set to {@code true}.
     */
    @Override
    public int cardinality() {
        throwExceptionIfClosed();

        long sum = 0;
        // Counting bits set to true for each word
        for (int i = 0; i < getWordsInUse(); i++)
            sum += Long.bitCount(words.getVolatileValueAt(i));
        return (int) sum;
    }

    /**
     * Performs a bitwise AND operation between this {@code ChronicleBitSet} and the provided set.
     * After this operation, a bit will be set to {@code true} in this set only if it was already set to {@code true} and the corresponding bit in the provided set is {@code true}.
     *
     * @param set The {@code ChronicleBitSet} to perform the AND operation with.
     */
    @Override
    public void and(ChronicleBitSet set) {
        throwExceptionIfClosed();

        // Check for self-reference, if true, no need to modify the current bitset
        if (this == set)
            return;

        OS.memory().loadFence();  // Ensuring recent changes to memory are visible

        int value = Math.toIntExact(getWordsInUse());
        // Resetting any bits that are beyond the word length of the provided set
        while (value > set.getWordsInUse())
            words.setValueAt(--value, 0);

        // Perform bitwise AND for overlapping words
        for (int i = 0; i < value; i++)
            and(i, set.getWord(i));
        OS.memory().storeFence();  // Ensuring changes made are visible to other threads
    }

    /**
     * Performs a logical <b>OR</b> of this bit set with the bit set argument. This bit set is modified so that a bit in it has the value {@code true}
     * if and only if it either already had the value {@code true} or the corresponding bit in the bit set argument has the value {@code true}.
     */
    @Override
    public void or(ChronicleBitSet set) {
        throwExceptionIfClosed();

        if (this == set)
            return;

        expandTo(set.getWordsInUse() - 1);
        long wordsInCommon = Math.min(getWordsInUse(), set.getWordsInUse());

        OS.memory().loadFence();
        // Perform logical OR on words in common
        int i;
        for (i = 0; i < wordsInCommon; i++)
            pipe(i, set.getWord(i));

        // Copy any remaining words
        for (; i < set.getWordsInUse(); i++)
            setWord(i, set.getWord(i));
        OS.memory().storeFence();
    }

    /**
     * Performs a logical <b>XOR</b> of this bit set with the bit set argument. This bit set is modified so that a bit in it has the value {@code
     * true} if and only if one of the following statements holds:
     * <ul>
     * <li>The bit initially has the value {@code true}, and the
     * corresponding bit in the argument has the value {@code false}.
     * <li>The bit initially has the value {@code false}, and the
     * corresponding bit in the argument has the value {@code true}.
     * </ul>
     */
    @Override
    public void xor(ChronicleBitSet set) {
        throwExceptionIfClosed();

        final int wordsInUse = getWordsInUse();
        final int wordsInUse2 = set.getWordsInUse();

        final int wordsInCommon = Math.toIntExact(Math.min(wordsInUse, wordsInUse2));

        // Expand current bitset if necessary to ensure it can accommodate all bits from the provided set
        expandTo(wordsInUse2 - 1);

        OS.memory().loadFence();
        // Ensuring recent changes to memory are visible

        // Perform bitwise XOR on words in common
        int i;
        for (i = 0; i < wordsInCommon; i++)
            caret(i, set.getWord(i));

        // Copy any remaining words from the provided set
        for (; i < wordsInUse2; i++)
            words.setValueAt(i, set.getWord(i));
        OS.memory().storeFence(); // Ensuring changes made are visible to other threads
    }

    /**
     * Clears all the bits in this {@code ChronicleBitSet} that are set in the specified {@code ChronicleBitSet}.
     * In other words, for each bit set in the provided set, the corresponding bit in this set is cleared.
     *
     * @param set The {@code ChronicleBitSet} whose set bits will be used to clear the corresponding bits in this set.
     */
    @Override
    public void andNot(ChronicleBitSet set) {
        throwExceptionIfClosed();

        // Perform logical (a & !b) on words in common
        OS.memory().loadFence();
        for (int i = Math.min(getWordsInUse(), set.getWordsInUse()) - 1; i >= 0; i--)
            and(i, ~set.getWord(i));

        OS.memory().storeFence();  // Ensuring changes made are visible to other threads
    }

    /**
     * Returns the hash code for this {@code ChronicleBitSet}. The hash code is calculated based on the set bits in this bit set.
     *
     * @return The hash code for this bit set.
     */
    @Override
    public int hashCode() {
        long h = 1234;
        OS.memory().loadFence();  // Ensuring recent changes to memory are visible

        for (int i = Math.toIntExact(getWordsInUse()); --i >= 0; )
            h ^= words.getValueAt(i) * (i + 1);

        return (int) ((h >> 32) ^ h);
    }

    /**
     * Returns the total number of bits that this {@code ChronicleBitSet} can represent. This includes both set and unset bits.
     *
     * @return The number of bits this bit set can represent.
     */
    @Override
    public int size() {
        return Math.toIntExact(words.getCapacity() * BITS_PER_WORD);
    }

    /**
     * Compares this {@code ChronicleBitSet} against the specified object.
     * The result is {@code true} if the object is a {@code ChronicleBitSet} and has the exact same set of bits set to {@code true} as this bit set.
     *
     * @param obj The object to compare with.
     * @return {@code true} if the specified object is equivalent to this bit set, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        return ChronicleBitSetSupport.equalsBitSet(this, obj, this::throwExceptionIfClosed);
    }

    /**
     * Returns a string representation of this bit set. For every index for which this {@code ChronicleBitSet} contains a bit in the set state, the decimal
     * representation of that index is included in the result. Such indices are listed in order from lowest to highest, separated by ",&nbsp;" (a
     * comma and a space) and surrounded by braces, resulting in the usual mathematical notation for a set of integers.
     */
    @Override
    public String toString() {
        return ChronicleBitSetSupport.toString(this);
    }

    /**
     * Returns an ordered stream of indices for which this {@code ChronicleBitSet} contains a bit set to true.
     * The indices are returned in ascending order. The size of the stream corresponds to the number of bits
     * set to true, which matches the value returned by the {@link #cardinality()} method.
     *
     * @return An ordered IntStream of indices of bits set to true.
     */
    @Deprecated(/* to be removed in 2027 */)
    public IntStream stream() {
        return ChronicleBitSetSupport.stream(this, this::throwExceptionIfClosed);
    }

    @Override
    public void writeMarshallable(@NotNull final WireOut wire) {
        throwExceptionIfClosed();
        wire.write("words").int64array(words.getCapacity(), words);
    }

    @Override
    public void readMarshallable(@NotNull final WireIn wire) throws IORuntimeException {
        singleThreadedCheckDisabled(true);

        throwExceptionIfClosed();

        closeQuietly(words);

        wire.read("words").int64array(null, this, (t, a) -> t.words = a);
    }

    @Override
    public void copyFrom(ChronicleBitSet bitSet) {
        OS.memory().loadFence();
        final int wordsInUse = bitSet.getWordsInUse();
        final long capacity = words.getCapacity();
        if (wordsInUse > capacity)
            throw new IllegalArgumentException("Too much data " + wordsInUse + " words > " + capacity);
        for (int i = getWordsInUse(); i > wordsInUse; i--)
            words.setValueAt(i, 0L);
        words.setUsed(wordsInUse);
        for (int i = 0; i < wordsInUse; i++)
            words.setValueAt(i, bitSet.getWord(i));
        OS.memory().storeFence();
    }

    /**
     * Represents a functional interface for a long-to-long function.
     * This can be useful for operations that require transforming or manipulating long values.
     */
    @FunctionalInterface
    interface LongFunction {
        /**
         * Applies the function on the given long values.
         *
         * @param oldValue The old value.
         * @param param The parameter value.
         * @return The result of applying the function.
         */
        long apply(long oldValue, long param);
    }
}
