/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.ref.LongReference;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.threads.Pauser;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.stream.IntStream;

import static net.openhft.chronicle.core.io.Closeable.closeQuietly;

/**
 * A {@link ChronicleBitSet} implementation backed by an array of
 * {@link net.openhft.chronicle.core.values.LongValue}s. The backing
 * words may reside in off-heap memory so the bit set can be shared
 * between processes. Operations are performed using compare-and-swap
 * loops rather than explicit locks.
 * <b>Note:</b> the capacity is fixed on construction and cannot be
 * expanded later.
 */
@SuppressWarnings("this-escape")
public class LongValueBitSet extends AbstractBitSetSupport implements Marshallable, ChronicleBitSet {

    // The array of {@link LongValue} words storing the bits for this set. Each entry is a 64‑bit word that may be shared across processes
    private LongValue[] words;

    /**
     * Creates a bit set sized for {@code maxNumberOfBits} bits.
     * The backing array is allocated but not bound to any {@link Wire}.
     *
     * @param maxNumberOfBits maximum number of bits this set can hold
     */
    public LongValueBitSet(final int maxNumberOfBits) {
        this((long) maxNumberOfBits);
    }

    /**
     * Creates a bit set sized for {@code maxNumberOfBits} bits and immediately
     * binds the backing array to the supplied {@link Wire} for serialisation.
     *
     * @param maxNumberOfBits maximum number of bits this set can hold
     * @param w               wire used to marshal the initial state
     */
    public LongValueBitSet(final int maxNumberOfBits, Wire w) {
        this((long) maxNumberOfBits, w);
    }

    /**
     * Creates a bit set sized for {@code maxNumberOfBits} bits.
     *
     * @param maxNumberOfBits maximum number of bits this set can hold
     */
    public LongValueBitSet(final long maxNumberOfBits) {
        int size = (int) ((maxNumberOfBits + BITS_PER_WORD - 1) / BITS_PER_WORD);
        words = new LongValue[size];
        singleThreadedCheckDisabled(true);
    }

    /**
     * Creates a bit set sized for {@code maxNumberOfBits} bits and binds its
     * storage to a {@link Wire} for marshalling.
     *
     * @param maxNumberOfBits maximum number of bits this set can hold
     * @param w               wire used to marshal the initial state
     */
    public LongValueBitSet(final long maxNumberOfBits, Wire w) {
        this(maxNumberOfBits);
        writeMarshallable(w);
        readMarshallable(w);
    }

    /**
     * Creates a new BitSet from a given byte array.
     *
     * @param bytes The byte array containing the bits.
     * @return A BitSet containing the bits from the byte array.
     */
    @Deprecated(/* to be removed in 2027 */)
    public static BitSet valueOf(byte[] bytes) {
        return valueOfBytes(bytes);
    }

    /**
     * Returns the raw word at {@code wordIndex}. If the index is beyond the
     * backing array the value {@code 0} is returned.
     */
    @Override
    public long getWord(int wordIndex) {
        return wordIndex < words.length ? words[wordIndex].getValue() : 0;
    }

    /**
     * Sets the raw word at {@code wordIndex} to {@code bits}. The array is
     * not expanded; {@link #expandTo(int)} will throw if the index is beyond the
     * configured capacity.
     */
    @Override
    public void setWord(int wordIndex, long bits) {
        expandTo(wordIndex);
        words[wordIndex].setValue(bits);
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
    protected void performClose() {
        closeQuietly((Object[]) words);
    }

    /**
     * Returns the number of {@link LongValue} words available in the backing
     * array. This is the fixed capacity rather than a logical size.
     */
    @Override
    public int getWordsInUse() {
        return words.length;
    }

    /**
     * Atomically updates {@code word} using a compare‑and‑swap loop. The
     * {@code function} computes a new value from the current one and
     * {@code param}. A pauser is used between failed CAS
     * attempts to reduce contention.
     *
     * @param word     word reference to update
     * @param param    parameter passed to the computation
     * @param function function computing a new value from the current one
     */
    public void set(LongValue word, long param, LongFunction function) {
        final Pauser internalPauser = pauser();
        internalPauser.reset();

        for (; ; ) {
            long oldValue = word.getVolatileValue();
            long value = function.apply(oldValue, param);
            if (oldValue == value || word.compareAndSwapValue(oldValue, value))
                break;
            internalPauser.pause();
        }
    }

    /**
     * Atomically sets {@code word} to {@code newValue}. A CAS loop is used
     * with pausing between attempts when contention occurs.
     *
     * @param word     word reference to update
     * @param newValue value to assign
     */
    @Deprecated(/* to be removed in 2027 */)
    public void set(LongValue word, long newValue) {
        casSet(word, newValue);
    }

    /**
     * Returns a little‑endian byte array representing this set. Each word
     * is written in sequence.
     *
     * @return byte array containing the bitset contents
     */
    @Deprecated(/* to be removed in 2027, as it is only used in tests */)
    public byte[] toByteArray() {
        throwExceptionIfClosed();

        int n = getWordsInUse();
        if (n == 0)
            return new byte[0];
        int len = 8 * (n - 1);
        for (long x = words[n - 1].getValue(); x != 0; x >>>= 8)
            len++;
        byte[] bytes = new byte[len];
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < n - 1; i++)
            bb.putLong(words[i].getVolatileValue());
        for (long x = words[n - 1].getValue(); x != 0; x >>>= 8)
            bb.put((byte) (x & 0xff));
        return bytes;
    }

    /**
     * Verifies that {@code wordIndex} is within the fixed capacity. This
     * implementation never resizes and will throw
     * {@link UnsupportedOperationException} if expansion would be required.
     */
    private void expandTo(int wordIndex) {
        int wordsRequired = wordIndex + 1;
        if (getWordsInUse() < wordsRequired) {
            throw new UnsupportedOperationException("todo: it is not possible currently to expand " +
                    "this structure, because if its concurrent nature and have to implement cross " +
                    "process locking");
        }
    }

    /**
     * Atomically toggles the bit at {@code bitIndex}.
     */
    @Override
    public void flip(int bitIndex) {
        throwExceptionIfClosed();

        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        int wordIndex = toWordIndex(bitIndex);
        expandTo(wordIndex);
        caret(words[wordIndex], 1L << bitIndex);
    }

    /**
     * Atomically performs {@code word ^= param}.
     */
    private void caret(LongValue word, long param) {
        set(word, param, (x, y) -> x ^ y);
    }

    /**
     * Atomically performs {@code word &= param}.
     */
    private void and(LongValue word, final long param) {
        set(word, param, (x, y) -> x & y);
    }

    /**
     * Atomically flips bits in the range [{@code fromIndex}, {@code toIndex}).
     */
    @Override
    public void flip(int fromIndex, int toIndex) {
        throwExceptionIfClosed();

        checkRange(fromIndex, toIndex);

        if (fromIndex == toIndex)
            return;

        int startWordIndex = toWordIndex(fromIndex);
        int endWordIndex = toWordIndex(toIndex - 1);

        // Ensure the BitSet is large enough to accommodate the word index
        expandTo(endWordIndex);

        // Create masks to target specific bits within the words
        long firstWordMask = WORD_MASK << fromIndex;
        long lastWordMask = WORD_MASK >>> -toIndex;
        if (startWordIndex == endWordIndex) {
            // Case 1: One word
            caret(words[startWordIndex], firstWordMask & lastWordMask);
        } else {
            // Case 2: Multiple words
            // Handle first word
            caret(words[startWordIndex], firstWordMask);

            // Handle intermediate words, if any
            for (int i = startWordIndex + 1; i < endWordIndex; i++)
                caret(words[i], WORD_MASK);

            // Handle last word
            caret(words[endWordIndex], lastWordMask);
        }
    }

    /**
     * Atomically sets the bit at {@code bitIndex}.
     */
    @Override
    public void set(int bitIndex) {
        // Check if the BitSet is closed, if so, throws an exception
        throwExceptionIfClosed();

        // Validate the bit index
        // Calculate the word index based on the given bit index
        int wordIndex = toWordIndex(bitIndex);

        // Set the desired bit to 1 (true) within the corresponding word
        pipe(words[wordIndex], (1L << bitIndex));
    }

    /**
     * Atomically performs {@code word |= param}.
     */
    private void pipe(LongValue word, long param) {
        // Set the desired bit by using the OR operation
        set(word, param, (x, y) -> x | y);
    }

    /**
     * Sets the bit at {@code bitIndex} to {@code value}.
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
     * Atomically sets all bits in the range [{@code fromIndex}, {@code toIndex}) to {@code true}.
     */
    @Override
    public void set(int fromIndex, int toIndex) {
        setRange(fromIndex, toIndex, length(), true);
    }

    /**
     * Atomically sets all bits in the range to {@code value}.
     */
    @Override
    public void set(int fromIndex, int toIndex, boolean value) {
        throwExceptionIfClosed();

        if (value)
            set(fromIndex, toIndex);
        else
            clear(fromIndex, toIndex);
    }

    /**
     * Sets the bit specified by the index to {@code false}.
     */
    @Override
    public void clear(int bitIndex) {
        throwExceptionIfClosed();

        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        int wordIndex = toWordIndex(bitIndex);
        if (wordIndex >= getWordsInUse())
            return;

        and(words[wordIndex], ~(1L << bitIndex));
    }

    /**
     * Sets the bits from the specified {@code fromIndex} (inclusive) to the specified {@code toIndex} (exclusive) to {@code false}.
     */
    @Override
    public void clear(int fromIndex, int toIndex) {
        setRange(fromIndex, toIndex, length(), false);
    }

    /**
     * Sets all of the bits in this ChronicleBitSet to {@code false}.
     */
    @Override
    public void clear() {
        throwExceptionIfClosed();

        int value = getWordsInUse();
        while (value > 0)
            words[--value].setValue(0);
    }

    /**
     * Returns the value of the bit with the specified index. The value is {@code true} if the bit with the index {@code bitIndex}
     * is currently set in this {@code ChronicleBitSet}; otherwise, the result is {@code false}.
     *
     * @param bitIndex the index of the bit to check
     * @return true if the bit at the specified index is set, false otherwise
     */
    @Override
    public boolean get(int bitIndex) {
        throwExceptionIfClosed();

        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        int wordIndex = toWordIndex(bitIndex);
        return (wordIndex < getWordsInUse())
                && ((words[wordIndex].getValue() & (1L << bitIndex)) != 0);
    }

    /**
     * Returns the index of the first bit that is set to {@code true} that occurs on or after the specified starting index.
     * If no such bit exists then {@code -1} is returned.
     *
     * @param fromIndex the index to start checking from
     * @return the index of the next set bit, or -1 if no such bit is found
     */
    @Override
    public int nextSetBit(int fromIndex) {
        throwExceptionIfClosed();

        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);

        int u = toWordIndex(fromIndex);

        // If the starting word index is beyond the current words in use, return -1 immediately.
        if (u >= getWordsInUse())
            return -1;

        // Mask off any bits in the current word before the starting index.
        long word = words[u].getVolatileValue() & (WORD_MASK << fromIndex);

        // Loop to find the first set bit.
        while (true) {
            // If a set bit is found in the current word, calculate its index and return.
            if (word != 0)
                return Math.toIntExact((u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word));

            // Move to the next word.
            if (++u == getWordsInUse())
                return -1; // No set bits found in remaining words.

            // Load the next word's value.
            word = words[u].getVolatileValue();
        }
    }

    /**
     * Returns the index of the first bit that is set to {@code true} that occurs on or after the specified starting index but before the toIndex.
     * If no such bit exists then {@code -1} is returned.
     *
     * @param fromIndex the index to start checking from
     * @param toIndex the index to stop checking (exclusive)
     * @return the index of the next set bit within the specified range, or -1 if no such bit is found
     */
    @Override
    public int nextSetBit(int fromIndex, int toIndex) {
        throwExceptionIfClosed();

        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);

        int u = toWordIndex(fromIndex);
        if (u >= getWordsInUse())
            return -1;

        // Mask off any bits in the current word before the starting index.
        long word = words[u].getVolatileValue() & (WORD_MASK << fromIndex);

        // Loop to find the first set bit.
        while (true) {
            if (word != 0)
                return Math.toIntExact((u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word));

            // Move to the next word.
            if (++u == getWordsInUse())
                return -1; // No set bits found in remaining words.

            // Exit if we have crossed the toIndex boundary.
            if (u * BITS_PER_WORD > toIndex)
                return -1;

            // Load the next word's value.
            word = words[u].getVolatileValue();
        }
    }

    /**
     * Returns the index of the first bit that is set to {@code false} that occurs on or after the specified starting index.
     *
     * @param fromIndex the index to start checking from
     * @return the index of the next unset bit, or the total length if all bits are set
     */
    @Override
    public int nextClearBit(int fromIndex) {
        throwExceptionIfClosed();

        // Neither spec nor implementation handle ChronicleBitSets of maximal length.
        // See 4816253.
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);

        int u = toWordIndex(fromIndex);

        // If the starting word index is beyond the current words in use,
        // return the fromIndex as no words after it have been set.
        if (u >= getWordsInUse())
            return fromIndex;

        // Invert the word's bits (i.e., 'set' becomes 'unset' and vice versa)
        // and mask off any bits in the current word before the starting index.
        long word = ~words[u].getVolatileValue() & (WORD_MASK << fromIndex);

        // Loop to find the first unset bit.
        while (true) {
            if (word != 0)
                return Math.toIntExact((u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word));

            // Move to the next word.
            if (++u == getWordsInUse())
                return Math.toIntExact(getWordsInUse() * BITS_PER_WORD); // All bits in use are set.

            // Invert the next word's bits.
            word = ~words[u].getValue();
        }
    }

    /**
     * This method searches for the closest bit set to {@code true} from the specified starting index moving backwards.
     * If the bit at the specified starting index is set to {@code true}, it will return the index itself.
     * If no such bit exists before the given index, or if {@code -1} is the specified index, then {@code -1} is returned.
     *
     * @param fromIndex The starting index to begin the search. The search moves towards the index 0 from this point.
     * @return The index of the nearest set bit (with value {@code true}) before the specified starting index, or {@code -1} if none exists.
     * @throws IndexOutOfBoundsException if {@code fromIndex} is less than {@code -1}
     */
    @Deprecated(/* to be removed in 2027 */)
    public int previousSetBit(int fromIndex) {
        throwExceptionIfClosed();

        // Check for special case where index is -1
        if (fromIndex < 0) {
            if (fromIndex == -1)
                return -1;
            throw new IndexOutOfBoundsException(
                    "fromIndex < -1: " + fromIndex);
        }

        int u = toWordIndex(fromIndex);
        if (u >= getWordsInUse())
            return length() - 1;

        long word = words[u].getValue() & (WORD_MASK >>> -(fromIndex + 1));

        while (true) {
            if (word != 0)
                return Math.toIntExact((u + 1) * BITS_PER_WORD - 1 - Long.numberOfLeadingZeros(word));
            if (u-- == 0)
                return -1;
            word = words[u].getValue();
        }
    }

    /**
     * This method searches for the closest bit set to {@code false} from the specified starting index moving backwards.
     * If the bit at the specified starting index is set to {@code false}, it will return the index itself.
     * If no such unset bit exists before the given index, or if {@code -1} is the specified index, then {@code -1} is returned.
     *
     * @param fromIndex The starting index to begin the search. The search moves towards the index 0 from this point.
     * @return The index of the nearest unset bit (with value {@code false}) before the specified starting index, or {@code -1} if none exists.
     * @throws IndexOutOfBoundsException if {@code fromIndex} is less than {@code -1}
     */
    @Deprecated(/* to be removed in 2027, as it is only used in tests */)
    public int previousClearBit(int fromIndex) {
        throwExceptionIfClosed();

        // Check for special case where index is -1
        if (fromIndex < 0) {
            if (fromIndex == -1)
                return -1;
            throw new IndexOutOfBoundsException(
                    "fromIndex < -1: " + fromIndex);
        }

        int u = toWordIndex(fromIndex);
        if (u >= getWordsInUse())
            return fromIndex;

        long word = ~words[u].getVolatileValue() & (WORD_MASK >>> -(fromIndex + 1));

        while (true) {
            if (word != 0)
                return Math.toIntExact((u + 1) * BITS_PER_WORD - 1 - Long.numberOfLeadingZeros(word));
            if (u-- == 0)
                return -1;
            word = ~words[u].getValue();
        }
    }

    /**
     * Checks if the current {@code ChronicleBitSet} has any common set bits with the specified {@code ChronicleBitSet}.
     * If any bits set to {@code true} in the provided set are also set to {@code true} in this set, then the method returns {@code true}.
     *
     * @param set The {@code ChronicleBitSet} to compare with the current instance.
     * @return {@code true} if there's an intersection, otherwise {@code false}.
     */
    @Override
    public boolean intersects(ChronicleBitSet set) {
        throwExceptionIfClosed();

        // Check common words between both bitsets for any intersection
        for (int i = Math.min(getWordsInUse(), set.getWordsInUse()) - 1; i >= 0; i--)
            if ((words[i].getVolatileValue() & set.getWord(i)) != 0)
                return true;
        return false;
    }

    /**
     * Checks if the current {@code ChronicleBitSet} intersects with the provided {@code LongValueBitSet}.
     * This method is an overloaded version of the intersects method, designed to work specifically with {@code LongValueBitSet}.
     *
     * @param set The {@code LongValueBitSet} to compare with the current instance.
     * @return {@code true} if there's an intersection, otherwise {@code false}.
     */
    @Deprecated(/* to be removed in 2027 */)
    public boolean intersects(LongValueBitSet set) {
        return intersects((ChronicleBitSet) set);
    }

    /**
     * Counts the bits set to {@code true}. Each word is read
     * with {@link LongValue#getVolatileValue()} for visibility.
     */
    @Override
    public int cardinality() {
        throwExceptionIfClosed();

        int sum = 0;
        // Count set bits in each word
        for (int i = 0; i < getWordsInUse(); i++)
            sum += Long.bitCount(words[i].getVolatileValue());
        return sum;
    }

    /**
     * Performs a logical <b>AND</b> operation between this {@code ChronicleBitSet} and the specified {@code ChronicleBitSet}.
     * After this operation, a bit in this set will be set to {@code true} only if it was originally {@code true} and the corresponding bit in the specified set is {@code true}.
     *
     * @param set The {@code ChronicleBitSet} to perform the logical <b>AND</b> operation with.
     */
    @Override
    public void and(ChronicleBitSet set) {
        throwExceptionIfClosed();

        // If both bitsets are the same, no operation is needed
        if (this == set)
            return;

        // Ensure words in excess in this set are set to 0
        int value = getWordsInUse();
        while (value > set.getWordsInUse()) {
            words[--value].setValue(0);
        }

        // Perform logical AND operation on words in common
        for (int i = 0; i < getWordsInUse(); i++)
            and(words[i], set.getWord(i));
    }

    /**
     * Performs a logical <b>AND</b> operation between this {@code ChronicleBitSet} and the specified {@code LongValueBitSet}.
     * This is an overloaded version of the method that accepts {@code ChronicleBitSet}, designed to work with {@code LongValueBitSet}.
     * The logic of the operation is handled by the base method.
     *
     * @param set The {@code LongValueBitSet} to perform the logical <b>AND</b> operation with.
     */
    @Deprecated(/* to be removed in 2027 */)
    public void and(LongValueBitSet set) {
        and((ChronicleBitSet) set);
    }

    /**
     * Performs a logical <b>OR</b> operation between this {@code ChronicleBitSet} and the provided {@code LongValueBitSet}.
     * This overloaded version casts the provided set to its base type {@code ChronicleBitSet} before performing the operation.
     *
     * @param set The {@code LongValueBitSet} to perform the logical <b>OR</b> operation with.
     */
    @Deprecated(/* to be removed in 2027 */)
    public void or(LongValueBitSet set) {
        or((ChronicleBitSet) set);
    }

    /**
     * Executes a logical <b>OR</b> operation between this {@code ChronicleBitSet} and the provided {@code ChronicleBitSet}.
     * Each bit in this set will be set to {@code true} if it was originally {@code true} or the corresponding bit in the provided set is {@code true}.
     *
     * @param set The {@code ChronicleBitSet} to perform the logical <b>OR</b> operation with.
     */
    @Override
    public void or(ChronicleBitSet set) {
        throwExceptionIfClosed();

        if (this == set)
            return;

        int wordsInCommon = Math.min(getWordsInUse(), set.getWordsInUse());

        OS.memory().loadFence();
        // Perform logical OR on words in common
        int i;
        for (i = 0; i < wordsInCommon; i++)
            pipe(words[i], set.getWord(i));

        // Copy any remaining words from the argument bit set
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
     *
     * @param set The {@code ChronicleBitSet} to perform the logical <b>XOR</b> operation with.
     */
    @Override
    public void xor(ChronicleBitSet set) {
        throwExceptionIfClosed();

        int wordsInCommon = Math.min(getWordsInUse(), set.getWordsInUse());

        OS.memory().loadFence();
        // Perform logical XOR on words in common
        int i;
        for (i = 0; i < wordsInCommon; i++)
            caret(words[i], set.getWord(i));

        // Copy any remaining words
        for (; i < set.getWordsInUse(); i++)
            setWord(i, set.getWord(i));
        OS.memory().storeFence();
    }

    /**
     * Performs a logical <b>XOR</b> operation between this {@code ChronicleBitSet} and the specified {@code LongValueBitSet}.
     * This is an overloaded version of the method that accepts {@code ChronicleBitSet}, designed to work with {@code LongValueBitSet}.
     * The logic of the operation is handled by the base method.
     *
     * @param set The {@code LongValueBitSet} to perform the logical <b>XOR</b> operation with.
     */
    @Deprecated(/* to be removed in 2027 */)
    public void xor(LongValueBitSet set) {
        xor((ChronicleBitSet) set);
    }

    /**
     * Clears all the bits in this {@code ChronicleBitSet} where the corresponding bit is set in the specified {@code ChronicleBitSet}.
     * Effectively performs a logical <b>AND NOT</b> operation on this bit set with the given set.
     *
     * @param set The {@code ChronicleBitSet} to use for clearing matching bits.
     */
    @Override
    public void andNot(ChronicleBitSet set) {
        throwExceptionIfClosed();

        // Perform logical (a & !b) on words in common
        OS.memory().loadFence();
        for (int i = Math.min(getWordsInUse(), set.getWordsInUse()) - 1; i >= 0; i--)
            and(words[i], ~set.getWord(i));
        OS.memory().storeFence();
    }

    /**
     * Clears all the bits in this {@code ChronicleBitSet} where the corresponding bit is set in the specified {@code LongValueBitSet}.
     * This is an overloaded version designed to work with {@code LongValueBitSet}.
     *
     * @param set The {@code LongValueBitSet} to use for clearing matching bits.
     */
    @Deprecated(/* to be removed in 2027 */)
    public void andNot(LongValueBitSet set) {
        andNot((ChronicleBitSet) set);
    }

    /**
     * Computes the hash code for this {@code ChronicleBitSet}. The hash code is calculated based on the bit values that are set.
     *
     * @return The computed hash code.
     */
    @Override
    public int hashCode() {
        long h = 1234;
        OS.memory().loadFence();
        for (int i = getWordsInUse(); --i >= 0; )
            h ^= words[i].getValue() * (i + 1);

        return (int) ((h >> 32) ^ h);
    }

    /**
     * Retrieves the number of bits that are actually being used by this {@code ChronicleBitSet} to represent bit values.
     * Essentially, this is the highest set bit plus one.
     *
     * @return The number of bits of space in use.
     */
    @Override
    public int size() {
        return Math.toIntExact(words.length * BITS_PER_WORD);
    }

    /**
     * Compares this {@code ChronicleBitSet} object against the specified object. The result is {@code true} if and only if:
     * <ul>
     *     <li>The provided object is not {@code null}.
     *     <li>The provided object is an instance of {@code ChronicleBitSet}.
     *     <li>Both {@code ChronicleBitSet} objects have the exact same set of bits set to {@code true}.
     * </ul>
     * In essence, for every non-negative {@code int} index {@code k}, the bits of both {@code ChronicleBitSet} objects at index {@code k} should be identical.
     *
     * @param obj The object to compare with.
     * @return {@code true} if the objects are the same; {@code false} otherwise.
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
     * Returns a stream of indices for which this {@code ChronicleBitSet} contains a bit in the set state. The indices are returned in order, from lowest to
     * highest. The size of the stream is the number of bits in the set state, equal to the value returned by the {@link #cardinality()} method.
     *
     * @return stream of set bit indices
     */
    @Deprecated(/* to be removed in 2027, as it is only used in tests */)
    public IntStream stream() {
        return ChronicleBitSetSupport.stream(this, this::throwExceptionIfClosed);
    }

    /** Serialises the backing words to {@code wire}. */
    @Override
    public void writeMarshallable(@NotNull final WireOut wire) {
        wire.write("numberOfLongValues").int32(words.length);

        for (int i = 0; i < words.length; i++) {
            if (words[i] == null)
                words[i] = wire.newLongReference();
            wire.getValueOut().int64forBinding(words[i].getValue());
        }
    }

    /** Deserialises the backing words from {@code wire}. */
    @Override
    public void readMarshallable(@NotNull final WireIn wire) throws IORuntimeException {
        singleThreadedCheckDisabled(true);
        throwExceptionIfClosed();

        closeQuietly((Object[]) words);

        int numberOfLongValues = wire.read("numberOfLongValues").int32();
        words = new LongReference[numberOfLongValues];
        for (int i = 0; i < numberOfLongValues; i++) {
            words[i] = wire.getValueIn().int64ForBinding(null);
        }
    }

    /** Copies the contents of {@code bitSet} into this instance. */
    @Override
    public void copyFrom(ChronicleBitSet bitSet) {
        OS.memory().loadFence();
        final int wordsInUse = bitSet.getWordsInUse();
        if (wordsInUse > words.length)
            throw new IllegalArgumentException("Too much data " + wordsInUse + " words > " + words.length);
        for (int i = getWordsInUse(); i > wordsInUse; i--)
            words[i].setValue(0L);
        for (int i = 0; i < wordsInUse; i++)
            words[i].setValue(bitSet.getWord(i));
        OS.memory().storeFence();
    }

    /**
     * Represents a function that accepts two long values (an old value and a parameter) and produces a long result.
     * This is the {@code long}-consuming and {@code long}-producing primitive specialization for
     * {@link java.util.function.Function}.
     *
     * <p>For example, this interface can be used to represent functions like addition:
     * <pre>
     * {@code
     * LongFunction add = (oldValue, param) -> oldValue + param;
     * long result = add.apply(2L, 3L);  // result will be 5
     * }
     * </pre>
     *
         */
    @FunctionalInterface
    interface LongFunction {

        /**
         * Applies this function to the given arguments.
         *
         * @param oldValue The old long value.
         * @param param The long parameter.
         * @return The function result.
         */
        long apply(long oldValue, long param);
    }
}
