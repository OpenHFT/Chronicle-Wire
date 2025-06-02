/*
 * Copyright 2016-2021 chronicle.software
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
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.ref.BinaryLongArrayReference;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.AbstractCloseable;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.threads.Pauser;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static net.openhft.chronicle.core.io.Closeable.closeQuietly;

/**
 * This {@code ChronicleBitSet} is intended to be shared between processes. To minimize locking constraints, it is implemented as a lock-free solution
 * without support for resizing.
 */
@SuppressWarnings("this-escape")
public class LongArrayValueBitSet extends AbstractCloseable implements Marshallable, ChronicleBitSet {

    /* Used to shift left or right for a partial word mask */
    private static final long WORD_MASK = ~0L;

    // Pauser object used for managing concurrent access (assuming based on its name, actual use needs context)
    private transient Pauser pauser;

    /**
     * The internal field corresponding to the serialField "bits".
     */
    private LongArrayValues words;

    /**
     * Constructs a new {@code LongArrayValueBitSet} with the given maximum number of bits.
     *
     * @param maxNumberOfBits Maximum number of bits that the bit set can handle.
     */
    public LongArrayValueBitSet(final long maxNumberOfBits) {
        words = new BinaryLongArrayReference((maxNumberOfBits + BITS_PER_WORD - 1) / BITS_PER_WORD);
        singleThreadedCheckDisabled(true);
    }

    /**
     * Constructs a new {@code LongArrayValueBitSet} with the given maximum number of bits and initializes it with the given {@code Wire}.
     *
     * @param maxNumberOfBits Maximum number of bits that the bit set can handle.
     * @param w               The {@code Wire} object to be used for initialization.
     */
    public LongArrayValueBitSet(final long maxNumberOfBits, Wire w) {
        this(maxNumberOfBits);
        writeMarshallable(w);
        readMarshallable(w);
    }

    /**
     * Calculates the word index in the internal storage corresponding to a given bit index.
     *
     * @param bitIndex The bit index for which to find the word index.
     * @return The word index containing the given bit index.
     * @throws IndexOutOfBoundsException if the provided bitIndex is negative.
     */
    private static int wordIndex(int bitIndex) {
        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        return (int) (bitIndex / BITS_PER_WORD);
    }

    /**
     * Constructs and returns a new {@code BitSet} using the bits from the provided byte array.
     *
     * @param bytes The byte array to be used for constructing the {@code BitSet}.
     * @return A new {@code BitSet} containing all bits from the given byte array.
     */
    public static BitSet valueOf(byte[] bytes) {
        return BitSet.valueOf(ByteBuffer.wrap(bytes));
    }

    /**
     * Checks that fromIndex ... toIndex is a valid range of bit indices.
     */
    private static void checkRange(int fromIndex, int toIndex) {
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        if (toIndex < 0)
            throw new IndexOutOfBoundsException("toIndex < 0: " + toIndex);
        if (fromIndex > toIndex)
            throw new IndexOutOfBoundsException("fromIndex: " + fromIndex +
                    " > toIndex: " + toIndex);
    }

    @Override
    public long getWord(int wordIndex) {
        return wordIndex < getWordsInUse() ? words.getValueAt(wordIndex) : 0;
    }

    @Override
    public void setWord(int wordIndex, long bits) {
        expandTo(wordIndex);
        words.setValueAt(wordIndex, bits);
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

        final Pauser internalPauser = pauser();
        internalPauser.reset();
        expandTo(wordIndex);

        for (; ; ) {
            final long oldValue = words.getVolatileValueAt(wordIndex);
            final long value = function.apply(oldValue, param);
            if (oldValue == value || words.compareAndSet(wordIndex, oldValue, value))
                break;
            internalPauser.pause();
        }
    }

    /**
     * Retrieves or initializes the internal {@code Pauser}, which is used to manage pauses during lock-free operations.
     *
     * @return The {@code Pauser} instance associated with this object.
     */
    private Pauser pauser() {
        if (this.pauser == null)
            this.pauser = Pauser.busy();
        return this.pauser;
    }

    /**
     * Sets a new value for a given word in this bit set.
     * This method is lock-free and uses CAS operations to safely set the new word value.
     *
     * @param word     The {@code LongValue} instance representing the word to set.
     * @param newValue The new value to set for the word.
     */
    public void set(LongValue word, long newValue) {
        throwExceptionIfClosed();

        pauser.reset();
        long oldValue = word.getVolatileValue();
        while (!word.compareAndSwapValue(oldValue, newValue)) {
            pauser.pause();
        }
    }

    /**
     * Converts the bits in this bit set to a byte array.
     * This allows the bit set to be easily serialized or transferred.
     *
     * @return A byte array containing all the bits in this bit set.
     */
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
    public void flip(int bitIndex) {
        throwExceptionIfClosed();

        int wordIndex = wordIndex(bitIndex);
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
    public void flip(int fromIndex, int toIndex) {
        throwExceptionIfClosed();

        checkRange(fromIndex, toIndex);

        if (fromIndex == toIndex)
            return;

        int startWordIndex = wordIndex(fromIndex);
        int endWordIndex = wordIndex(toIndex - 1);
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
    public void set(int bitIndex) {
        throwExceptionIfClosed();

        int wordIndex = wordIndex(bitIndex);

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
    public void set(int fromIndex, int toIndex) {
        throwExceptionIfClosed();

        checkRange(fromIndex, toIndex);

        if (fromIndex == toIndex)
            return;

        // Increase capacity if necessary
        int startWordIndex = wordIndex(fromIndex);
        int endWordIndex = wordIndex(toIndex - 1);
        expandTo(endWordIndex);

        long firstWordMask = WORD_MASK << fromIndex;
        long lastWordMask = WORD_MASK >>> -toIndex;
        if (startWordIndex == endWordIndex) {
            // Case 1: One word
            pipe(startWordIndex, firstWordMask & lastWordMask);
        } else {
            // Case 2: Multiple words
            // Handle first word
            pipe(startWordIndex, firstWordMask);

            // Handle intermediate words, if any
            for (int i = startWordIndex + 1; i < endWordIndex; i++)
                setWord(i, WORD_MASK);

            // Handle last word (restores invariants)
            pipe(endWordIndex, lastWordMask);
        }
    }

    /**
     * Clears (sets to {@code false}) the bit specified by the index.
     * If the bit index is beyond the current words in use, the operation is ignored.
     *
     * @param bitIndex The index of the bit to be cleared.
     */
    public void clear(int bitIndex) {
        throwExceptionIfClosed();

        int wordIndex = wordIndex(bitIndex);

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
    public void clear(int fromIndex, int toIndex) {
        throwExceptionIfClosed();

        checkRange(fromIndex, toIndex);

        // If start and end are the same, no bits to clear
        if (fromIndex == toIndex)
            return;

        int startWordIndex = wordIndex(fromIndex);

        // If the startWordIndex is beyond current words in use, there's nothing to clear
        if (startWordIndex >= getWordsInUse())
            return;

        int endWordIndex = wordIndex(toIndex - 1);

        // Adjust the range if the endWordIndex is beyond words in use
        if (endWordIndex >= getWordsInUse()) {
            toIndex = length();
            endWordIndex = getWordsInUse() - 1;
        }

        // Generate masks for the start and end words
        long firstWordMask = WORD_MASK << fromIndex;
        long lastWordMask = WORD_MASK >>> -toIndex;
        if (startWordIndex == endWordIndex) {
            // Case 1: One word
            and(startWordIndex, ~(firstWordMask &
                    lastWordMask));
        } else {
            // Case 2: Multiple words
            // Handle first word
            and(startWordIndex, ~firstWordMask);

            // Set all bits to false for intermediate words
            for (int i = startWordIndex + 1; i < endWordIndex; i++)
                words.setOrderedValueAt(i, 0);

            // Clear bits in the last word
            and(endWordIndex, ~lastWordMask);
        }
    }

    /**
     * Sets all bits in this ChronicleBitSet to {@code false}.
     * Post invocation, the ChronicleBitSet is effectively empty with no bits set to {@code true}.
     */
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
    public boolean get(int bitIndex) {
        throwExceptionIfClosed();

        int wordIndex = wordIndex(bitIndex);

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
    public int nextSetBit(int fromIndex) {
        throwExceptionIfClosed();

        int u = wordIndex(fromIndex);

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
    public int nextSetBit(int fromIndex, int toIndex) {
        throwExceptionIfClosed();

        int u = wordIndex(fromIndex);

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
    public int nextClearBit(int fromIndex) {
        throwExceptionIfClosed();

        int u = wordIndex(fromIndex);

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
    public int previousSetBit(int fromIndex) {
        throwExceptionIfClosed();

        // Special case for -1, to return -1 as specified
        if (fromIndex < 0) {
            if (fromIndex == -1)
                return -1;
            throw new IndexOutOfBoundsException(
                    "fromIndex < -1: " + fromIndex);
        }

        int u = wordIndex(fromIndex);

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
    public int previousClearBit(int fromIndex) {
        throwExceptionIfClosed();

        // Special case for -1, to return -1 as specified
        if (fromIndex < 0) {
            if (fromIndex == -1)
                return -1;
            throw new IndexOutOfBoundsException(
                    "fromIndex < -1: " + fromIndex);
        }

        int u = wordIndex(fromIndex);

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
    public boolean equals(Object obj) {
        throwExceptionIfClosed();

        // Check for object type and self-reference
        if (!(obj instanceof ChronicleBitSet))
            return false;
        if (this == obj)
            return true;

        ChronicleBitSet set = (ChronicleBitSet) obj;

        OS.memory().loadFence();  // Ensuring recent changes to memory are visible

        // Compare words in use by both ChronicleBitSets
        for (int i = 0, max = Math.max(getWordsInUse(), set.getWordsInUse()); i < max; i++)
            if (getWord(i) != set.getWord(i))
                return false;

        return true;
    }

    /**
     * Returns a string representation of this bit set. For every index for which this {@code ChronicleBitSet} contains a bit in the set state, the decimal
     * representation of that index is included in the result. Such indices are listed in order from lowest to highest, separated by ",&nbsp;" (a
     * comma and a space) and surrounded by braces, resulting in the usual mathematical notation for a set of integers.
     */
    @Override
    public String toString() {

        int numBits = Math.toIntExact((getWordsInUse() > 128) ?
                cardinality() : getWordsInUse() * BITS_PER_WORD);
        StringBuilder b = new StringBuilder(6 * numBits + 2);
        b.append('{');

        int i = nextSetBit(0);
        if (i != -1) {
            b.append(i);
            while (true) {
                if (++i < 0) break;
                if ((i = nextSetBit(i)) < 0) break;
                int endOfRun = nextClearBit(i);
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
     * Returns an ordered stream of indices for which this {@code ChronicleBitSet} contains a bit set to true.
     * The indices are returned in ascending order. The size of the stream corresponds to the number of bits
     * set to true, which matches the value returned by the {@link #cardinality()} method.
     *
     * @return An ordered IntStream of indices of bits set to true.
     */
    public IntStream stream() {
        throwExceptionIfClosed();

        // Internal iterator to loop over set bits in the ChronicleBitSet
        class BitSetIterator implements PrimitiveIterator.OfInt {
            int next = nextSetBit(0);  // starting from the first bit

            @Override
            public boolean hasNext() {
                throwExceptionIfClosed();

                return next != -1;  // returns true if there is another set bit
            }

            @Override
            public int nextInt() {
                throwExceptionIfClosed();

                if (next != -1) {
                    int ret = next;
                    next = nextSetBit(next + 1);  // find the next set bit after the current one
                    return ret;
                } else {
                    throw new NoSuchElementException();  // if no more set bits are found
                }
            }
        }

        // Constructing the IntStream using a spliterator and the internal iterator
        return StreamSupport.intStream(
                () -> Spliterators.spliterator(
                        new BitSetIterator(), cardinality(),
                        Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.SORTED),
                Spliterator.SIZED | Spliterator.SUBSIZED |
                        Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.SORTED,
                false);
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
