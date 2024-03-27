/*
 * Copyright 2016-2020 chronicle.software
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

import net.openhft.chronicle.bytes.ref.LongReference;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.AbstractCloseable;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.threads.Pauser;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static net.openhft.chronicle.core.io.Closeable.closeQuietly;

/**
 * This is the LongValueBitSet class extending AbstractCloseable.
 * This class represents a BitSet designed to be shared across processes without requiring locks.
 * It has been implemented as a lock-free solution and does not support resizing.
 */
public class LongValueBitSet extends AbstractCloseable implements Marshallable, ChronicleBitSet {

    // Mask used for operations on partial words.
    private static final long WORD_MASK = ~0L;

    // Provides a pausing strategy for contention management.
    private transient Pauser pauser;

    /**
     * The internal field corresponding to the serialField "bits".
     */
    private LongValue[] words;

    /**
     * Constructor that initializes a LongValueBitSet with a maximum number of bits provided as an integer.
     *
     * @param maxNumberOfBits The maximum number of bits this BitSet can accommodate.
     */
    public LongValueBitSet(final int maxNumberOfBits) {
        this((long) maxNumberOfBits);
    }

    /**
     * Constructor that initializes a LongValueBitSet with a maximum number of bits provided as an integer and associates it with a Wire.
     *
     * @param maxNumberOfBits The maximum number of bits this BitSet can accommodate.
     * @param w The Wire associated with this BitSet.
     */
    public LongValueBitSet(final int maxNumberOfBits, Wire w) {
        this((long) maxNumberOfBits, w);
    }

    /**
     * Constructor that initializes a LongValueBitSet with a maximum number of bits provided as a long.
     *
     * @param maxNumberOfBits The maximum number of bits this BitSet can accommodate.
     */
    public LongValueBitSet(final long maxNumberOfBits) {
        int size = (int) ((maxNumberOfBits + BITS_PER_WORD - 1) / BITS_PER_WORD);
        words = new LongValue[size];
        singleThreadedCheckDisabled(true);
    }

    /**
     * Constructor that initializes a LongValueBitSet with a maximum number of bits provided as a long and associates it with a Wire.
     *
     * @param maxNumberOfBits The maximum number of bits this BitSet can accommodate.
     * @param w The Wire associated with this BitSet.
     */
    public LongValueBitSet(final long maxNumberOfBits, Wire w) {
        this(maxNumberOfBits);
        writeMarshallable(w);
        readMarshallable(w);
    }

    /**
     * Determines the word index for a given bit index. This method helps in locating the word in the array
     * that contains a specific bit.
     *
     * @param bitIndex The index of the bit in the BitSet.
     * @return The index of the word containing the bit.
     */
    private static int wordIndex(int bitIndex) {
        return (int) (bitIndex / BITS_PER_WORD);
    }

    /**
     * Creates a new BitSet from a given byte array.
     *
     * @param bytes The byte array containing the bits.
     * @return A BitSet containing the bits from the byte array.
     */
    public static BitSet valueOf(byte[] bytes) {
        return BitSet.valueOf(ByteBuffer.wrap(bytes));
    }

    /**
     * Validates that the range specified by fromIndex and toIndex is a legitimate range of bit indices.
     * Throws an IndexOutOfBoundsException if any of the indices are invalid.
     *
     * @param fromIndex Starting index of the range.
     * @param toIndex Ending index of the range.
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
        return wordIndex < words.length ? words[wordIndex].getValue() : 0;
    }

    @Override
    public void setWord(int wordIndex, long bits) {
        expandTo(wordIndex);
        words[wordIndex].setValue(bits);
    }

    @Override
    protected void performClose() {
        closeQuietly(words);
    }

    /**
     * Fetches the number of words currently in use in this BitSet.
     *
     * @return The number of words in use.
     */
    public int getWordsInUse() {
        return words.length;
    }

    /**
     * Atomically sets the value of a LongValue object based on a provided function and parameter.
     * It uses a pausing strategy to deal with contention.
     *
     * @param word The LongValue object whose value needs to be set.
     * @param param The parameter to pass to the function.
     * @param function A function that takes the old value of the word and the provided parameter to produce a new value.
     */
    public void set(LongValue word, long param, LongFunction function) {
        throwExceptionIfClosed();

        final Pauser internalPauser = pauser();
        internalPauser.reset();

        for (; ; ) {
            long oldValue = word.getVolatileValue();
            if (word.compareAndSwapValue(oldValue, function.apply(oldValue, param)))
                break;
            internalPauser.pause();
        }
    }

    /**
     * Fetches the Pauser object for this bit set.
     * If the Pauser object is not yet initialized, it initializes it to a busy pauser.
     *
     * @return The Pauser object associated with this bit set.
     */
    private Pauser pauser() {
        if (this.pauser == null)
            this.pauser = Pauser.busy();
        return this.pauser;
    }

    /**
     * Atomically sets the value of a LongValue object to a new value.
     * It uses a pausing strategy to handle contention during the set operation.
     *
     * @param word The LongValue object whose value needs to be set.
     * @param newValue The new value to be set.
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
     * Converts the bit set to a byte array representation.
     * It serializes the bits into bytes in little-endian order.
     *
     * @return A byte array containing all the bits set in this bit set.
     */
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
     * Ensures that the current bit set can accommodate a specified word index.
     * Throws an UnsupportedOperationException if the bit set cannot be expanded.
     *
     * @param wordIndex The word index that needs to be accommodated.
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
     * Flips the bit at the specified index, toggling it from its current value.
     * If the bit is currently set to 0, it will become 1, and vice versa.
     *
     * @param bitIndex The index of the bit to flip.
     */
    public void flip(int bitIndex) {
        throwExceptionIfClosed();

        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        int wordIndex = wordIndex(bitIndex);
        expandTo(wordIndex);
        caret(words[wordIndex], 1L << bitIndex);
    }

    /**
     * Applies the bitwise XOR operation between the provided word and parameter.
     * The result of this operation toggles the bits where they differ.
     *
     * @param word The LongValue object representing the word.
     * @param param The parameter against which XOR operation needs to be performed.
     */
    private void caret(LongValue word, long param) {
        set(word, param, (x, y) -> x ^ y);
    }

    /**
     * Applies the bitwise AND operation between the provided word and parameter.
     * The result of this operation retains the bits that are set in both the word and the parameter.
     *
     * @param word The LongValue object representing the word.
     * @param param The parameter against which AND operation needs to be performed.
     */
    private void and(LongValue word, final long param) {
        set(word, param, (x, y) -> x & y);
    }

    /**
     * Flips a range of bits, toggling them from their current value.
     * If a bit within the range is currently set to 0, it will become 1, and vice versa.
     *
     * @param fromIndex Index of the first bit to flip (inclusive).
     * @param toIndex Index of the last bit to flip (exclusive).
     */
    public void flip(int fromIndex, int toIndex) {
        throwExceptionIfClosed();

        checkRange(fromIndex, toIndex);

        if (fromIndex == toIndex)
            return;

        int startWordIndex = wordIndex(fromIndex);
        int endWordIndex = wordIndex(toIndex - 1);

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
     * Sets the bit at the specified index to {@code true}.
     *
     * @param bitIndex The index of the bit to be set to {@code true}.
     */
    public void set(int bitIndex) {
        // Check if the BitSet is closed, if so, throws an exception
        throwExceptionIfClosed();

        // Validate the bit index
        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        // Calculate the word index based on the given bit index
        int wordIndex = wordIndex(bitIndex);

        // Set the desired bit to 1 (true) within the corresponding word
        pipe(words[wordIndex], (1L << bitIndex));
    }

    /**
     * Performs a bitwise OR operation on the given word and parameter.
     * This method is particularly used to set a specific bit to {@code true} within a word.
     *
     * @param word The word on which the operation will be performed.
     * @param param The parameter value used in the OR operation.
     */
    private void pipe(LongValue word, long param) {
        // Set the desired bit by using the OR operation
        set(word, param, (x, y) -> x | y);
    }

    /**
     * Sets the bit at the specified index to the specified value.
     * If the value is {@code true}, the bit is set to 1, otherwise it is set to 0.
     *
     * @param bitIndex The index of the bit to be modified.
     * @param value The new value for the specified bit.
     */
    public void set(int bitIndex, boolean value) {
        throwExceptionIfClosed();

        if (value)
            set(bitIndex);
        else
            clear(bitIndex);
    }

    /**
     * Sets the bits from the specified {@code fromIndex} (inclusive) to the specified {@code toIndex} (exclusive) to {@code true}.
     */
    public void set(int fromIndex, int toIndex) {
        throwExceptionIfClosed();

        checkRange(fromIndex, toIndex);

        if (fromIndex == toIndex)
            return;

        // Determine the word indexes for the start and end of the range
        int startWordIndex = wordIndex(fromIndex);
        int endWordIndex = wordIndex(toIndex - 1);
        expandTo(endWordIndex);

        // Create masks for the start and end words
        long firstWordMask = WORD_MASK << fromIndex;
        long lastWordMask = WORD_MASK >>> -toIndex;

        if (startWordIndex == endWordIndex) {
            // Case 1: One word
            pipe(words[startWordIndex], firstWordMask & lastWordMask);
        } else {
            // Case 2: Multiple words
            // Handle first word
            pipe(words[startWordIndex], firstWordMask);

            // Handle intermediate words, if any
            for (int i = startWordIndex + 1; i < endWordIndex; i++)
                setWord(i, WORD_MASK);

            // Handle last word (restores invariants)
            pipe(words[endWordIndex], lastWordMask);
        }
    }

    /**
     * Sets the bits from the specified {@code fromIndex} (inclusive) to the specified {@code toIndex} (exclusive) to the specified value.
     *
     * @param fromIndex index of the first bit to be set
     * @param toIndex   index after the last bit to be set
     * @param value     value to set the selected bits to
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, or {@code toIndex} is negative, or {@code fromIndex} is larger than {@code
     *                                   toIndex}
     */
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
    public void clear(int bitIndex) {
        throwExceptionIfClosed();

        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        int wordIndex = wordIndex(bitIndex);
        if (wordIndex >= getWordsInUse())
            return;

        and(words[wordIndex], ~(1L << bitIndex));
    }

    /**
     * Sets the bits from the specified {@code fromIndex} (inclusive) to the specified {@code toIndex} (exclusive) to {@code false}.
     */
    public void clear(int fromIndex, int toIndex) {
        throwExceptionIfClosed();

        checkRange(fromIndex, toIndex);

        if (fromIndex == toIndex)
            return;

        int startWordIndex = wordIndex(fromIndex);
        if (startWordIndex >= getWordsInUse())
            return;

        // Determine the word indexes for the end of the range.
        int endWordIndex = wordIndex(toIndex - 1);

        // Adjust the end index and word if it exceeds the current words in use.
        if (endWordIndex >= getWordsInUse()) {
            toIndex = length();
            endWordIndex = getWordsInUse() - 1;
        }

        // Create masks for the start and end words.
        long firstWordMask = WORD_MASK << fromIndex;
        long lastWordMask = WORD_MASK >>> -toIndex;
        if (startWordIndex == endWordIndex) {
            // Case 1: One word
            and(words[startWordIndex], ~(firstWordMask &
                    lastWordMask));
        } else {
            // Case 2: Multiple words
            // Handle first word
            and(words[startWordIndex], ~firstWordMask);

            // Handle intermediate words, if any
            for (int i = startWordIndex + 1; i < endWordIndex; i++)
                words[i].setOrderedValue(0);

            // Handle last word
            and(words[endWordIndex], ~lastWordMask);
        }
    }

    /**
     * Sets all of the bits in this ChronicleBitSet to {@code false}.
     */
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
    public boolean get(int bitIndex) {
        throwExceptionIfClosed();

        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        int wordIndex = wordIndex(bitIndex);
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
    public int nextSetBit(int fromIndex) {
        throwExceptionIfClosed();

        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);

        int u = wordIndex(fromIndex);

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
    public int nextSetBit(int fromIndex, int toIndex) {
        throwExceptionIfClosed();

        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);

        int u = wordIndex(fromIndex);
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
    public int nextClearBit(int fromIndex) {
        throwExceptionIfClosed();

        // Neither spec nor implementation handle ChronicleBitSets of maximal length.
        // See 4816253.
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);

        int u = wordIndex(fromIndex);

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
    public int previousSetBit(int fromIndex) {
        throwExceptionIfClosed();

        // Check for special case where index is -1
        if (fromIndex < 0) {
            if (fromIndex == -1)
                return -1;
            throw new IndexOutOfBoundsException(
                    "fromIndex < -1: " + fromIndex);
        }

        int u = wordIndex(fromIndex);
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
    public int previousClearBit(int fromIndex) {
        throwExceptionIfClosed();

        // Check for special case where index is -1
        if (fromIndex < 0) {
            if (fromIndex == -1)
                return -1;
            throw new IndexOutOfBoundsException(
                    "fromIndex < -1: " + fromIndex);
        }

        int u = wordIndex(fromIndex);
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
    public boolean intersects(LongValueBitSet set) {
        return intersects((ChronicleBitSet) set);
    }

    /**
     * Calculates and returns the number of bits set to {@code true} in this {@code ChronicleBitSet}.
     *
     * @return The number of bits currently set to {@code true}.
     */
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
    public void and(LongValueBitSet set) {
        and((ChronicleBitSet) set);
    }

    /**
     * Performs a logical <b>OR</b> operation between this {@code ChronicleBitSet} and the provided {@code LongValueBitSet}.
     * This overloaded version casts the provided set to its base type {@code ChronicleBitSet} before performing the operation.
     *
     * @param set The {@code LongValueBitSet} to perform the logical <b>OR</b> operation with.
     */
    public void or(LongValueBitSet set) {
        or((ChronicleBitSet) set);
    }

    /**
     * Executes a logical <b>OR</b> operation between this {@code ChronicleBitSet} and the provided {@code ChronicleBitSet}.
     * Each bit in this set will be set to {@code true} if it was originally {@code true} or the corresponding bit in the provided set is {@code true}.
     *
     * @param set The {@code ChronicleBitSet} to perform the logical <b>OR</b> operation with.
     */
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
    public void xor(LongValueBitSet set) {
        xor((ChronicleBitSet) set);
    }

    /**
     * Clears all the bits in this {@code ChronicleBitSet} where the corresponding bit is set in the specified {@code ChronicleBitSet}.
     * Effectively performs a logical <b>AND NOT</b> operation on this bit set with the given set.
     *
     * @param set The {@code ChronicleBitSet} to use for clearing matching bits.
     */
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
    public void andNot(LongValueBitSet set) {
        andNot((ChronicleBitSet) set);
    }

    /**
     * Computes the hash code for this {@code ChronicleBitSet}. The hash code is calculated based on the bit values that are set.
     *
     * @return The computed hash code.
     */
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
    public boolean equals(Object obj) {
        throwExceptionIfClosed();

        if (!(obj instanceof ChronicleBitSet))
            return false;
        if (this == obj)
            return true;

        ChronicleBitSet set = (ChronicleBitSet) obj;

        // Check words in use by both ChronicleBitSets
        OS.memory().loadFence();
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

        int numBits = (getWordsInUse() > 128) ?
                cardinality() : Math.toIntExact(getWordsInUse() * BITS_PER_WORD);
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
     * Returns a stream of indices for which this {@code ChronicleBitSet} contains a bit in the set state. The indices are returned in order, from lowest to
     * highest. The size of the stream is the number of bits in the set state, equal to the value returned by the {@link #cardinality()} method.
     */
    public IntStream stream() {
        throwExceptionIfClosed();

        class BitSetIterator implements PrimitiveIterator.OfInt {
            int next = nextSetBit(0);

            @Override
            public boolean hasNext() {
                throwExceptionIfClosed();

                return next != -1;
            }

            @Override
            public int nextInt() {
                throwExceptionIfClosed();

                if (next != -1) {
                    int ret = next;
                    next = nextSetBit(next + 1);
                    return ret;
                } else {
                    throw new NoSuchElementException();
                }
            }
        }

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
        wire.write("numberOfLongValues").int32(words.length);

        for (int i = 0; i < words.length; i++) {
            if (words[i] == null)
                words[i] = wire.newLongReference();
            wire.getValueOut().int64forBinding(words[i].getValue());
        }
    }

    @Override
    public void readMarshallable(@NotNull final WireIn wire) throws IORuntimeException {
        singleThreadedCheckDisabled(true);
        throwExceptionIfClosed();

        closeQuietly(words);

        int numberOfLongValues = wire.read("numberOfLongValues").int32();
        words = new LongReference[numberOfLongValues];
        for (int i = 0; i < numberOfLongValues; i++) {
            words[i] = wire.getValueIn().int64ForBinding(null);
        }

    }

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
