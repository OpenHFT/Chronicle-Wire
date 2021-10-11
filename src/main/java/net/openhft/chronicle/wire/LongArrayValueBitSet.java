/*
 * Copyright 2016-2021 chronicle.software
 *
 * https://chronicle.software
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
 * This <code>ChronicleBitSet</code> is intended to be shared between processes. To minimize locking constraints, it is implemented as a lock-free solution
 * without support for resizing.
 */
public class LongArrayValueBitSet extends AbstractCloseable implements Marshallable, ChronicleBitSet {

    /* Used to shift left or right for a partial word mask */
    private static final long WORD_MASK = ~0L;
    private transient Pauser pauser = Pauser.busy();

    /**
     * The internal field corresponding to the serialField "bits".
     */
    private LongArrayValues words;

    public LongArrayValueBitSet(final long maxNumberOfBits) {
        words = new BinaryLongArrayReference((maxNumberOfBits + BITS_PER_WORD - 1) / BITS_PER_WORD);
        disableThreadSafetyCheck(true);
    }

    public LongArrayValueBitSet(final long maxNumberOfBits, Wire w) {
        this(maxNumberOfBits);
        writeMarshallable(w);
        readMarshallable(w);
    }

    /**
     * Given a bit index, return word index containing it.
     */
    private static int wordIndex(int bitIndex) {
        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        return bitIndex / BITS_PER_WORD;
    }

    /**
     * Returns a new bit set containing all the bits in the given byte array.
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

    public int getWordsInUse() {
        return Math.toIntExact(words.getUsed());
    }

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

    private Pauser pauser() {
        if (this.pauser == null)
            this.pauser = Pauser.busy();
        return this.pauser;
    }

    public void set(LongValue word, long newValue) {
        throwExceptionIfClosed();

        pauser.reset();
        long oldValue = word.getVolatileValue();
        while (!word.compareAndSwapValue(oldValue, newValue)) {
            pauser.pause();
        }
    }

    /**
     * Returns a new byte array containing all the bits in this bit set.
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
     * Sets the bit at the specified index to the complement of its current value.
     */
    public void flip(int bitIndex) {
        throwExceptionIfClosed();

        int wordIndex = wordIndex(bitIndex);
        expandTo(wordIndex);
        caret(wordIndex, 1L << bitIndex);
    }

    private void caret(int wordIndex, long param) {
        set(wordIndex, param, (x, y) -> x ^ y);
    }

    private void and(int wordIndex, final long param) {
        set(wordIndex, param, (x, y) -> x & y);
    }

    /**
     * Sets each bit from the specified {@code fromIndex} (inclusive) to the specified {@code toIndex} (exclusive) to the complement of its current
     * value.
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
     */
    public void set(int bitIndex) {
        throwExceptionIfClosed();

        int wordIndex = wordIndex(bitIndex);

        pipe(wordIndex, (1L << bitIndex)); // Restores
    }

    private void pipe(int wordIndex, long param) {
        set(wordIndex, param, (x, y) -> x | y);
    }

    public void set(int bitIndex, boolean value) {
        throwExceptionIfClosed();

        if (value)
            set(bitIndex);
        else
            clear(bitIndex);
    }

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
     * Sets the bit specified by the index to {@code false}.
     */
    public void clear(int bitIndex) {
        throwExceptionIfClosed();

        int wordIndex = wordIndex(bitIndex);
        if (wordIndex >= getWordsInUse())
            return;

        and(wordIndex, ~(1L << bitIndex));
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

        int endWordIndex = wordIndex(toIndex - 1);
        if (endWordIndex >= getWordsInUse()) {
            toIndex = length();
            endWordIndex = Math.toIntExact(getWordsInUse() - 1);
        }

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

            // Handle intermediate words, if any
            for (int i = startWordIndex + 1; i < endWordIndex; i++)
                words.setOrderedValueAt(i, 0);

            // Handle last word
            and(endWordIndex, ~lastWordMask);
        }
    }

    /**
     * Sets all of the bits in this ChronicleBitSet to {@code false}.
     */
    public void clear() {
        throwExceptionIfClosed();

        int value = Math.toIntExact(getWordsInUse());
        while (value > 0)
            words.setValueAt(--value, 0);
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
        return (wordIndex < getWordsInUse())
                && ((words.getValueAt(wordIndex) & (1L << bitIndex)) != 0);
    }

    public int nextSetBit(int fromIndex) {
        throwExceptionIfClosed();

        int u = wordIndex(fromIndex);
        if (u >= getWordsInUse())
            return -1;

        long word = words.getVolatileValueAt(u) & (WORD_MASK << fromIndex);

        while (true) {
            if (word != 0)
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            if (++u == getWordsInUse())
                return -1;
            word = words.getValueAt(u);
        }
    }

    /**
     * Returns the index of the first bit that is set to {@code true} that occurs on or after the specified starting index. If no such bit exists then
     * {@code -1} is returned.
     */
    public int nextSetBit(int fromIndex, int toIndex) {
        throwExceptionIfClosed();

        int u = wordIndex(fromIndex);
        if (u >= getWordsInUse())
            return -1;

        long word = words.getVolatileValueAt(u) & (WORD_MASK << fromIndex);

        while (true) {
            if (word != 0)
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            if (++u == getWordsInUse())
                return -1;
            if (u * BITS_PER_WORD > toIndex)
                return -1;
            word = words.getValueAt(u);
        }
    }

    /**
     * Returns the index of the first bit that is set to {@code false} that occurs on or after the specified starting index.
     */
    public int nextClearBit(int fromIndex) {
        throwExceptionIfClosed();

        int u = wordIndex(fromIndex);
        if (u >= getWordsInUse())
            return fromIndex;

        long word = ~words.getVolatileValueAt(u) & (WORD_MASK << fromIndex);

        while (true) {
            if (word != 0)
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            if (++u == getWordsInUse())
                return Math.toIntExact(getWordsInUse() * BITS_PER_WORD);
            word = ~words.getValueAt(u);
        }
    }

    /**
     * Returns the index of the nearest bit that is set to {@code true} that occurs on or before the specified starting index. If no such bit exists,
     * or if {@code -1} is given as the starting index, then {@code -1} is returned.
     */
    public int previousSetBit(int fromIndex) {
        throwExceptionIfClosed();

        if (fromIndex < 0) {
            if (fromIndex == -1)
                return -1;
            throw new IndexOutOfBoundsException(
                    "fromIndex < -1: " + fromIndex);
        }

        int u = wordIndex(fromIndex);
        if (u >= getWordsInUse())
            return length() - 1;

        long word = words.getVolatileValueAt(u) & (WORD_MASK >>> -(fromIndex + 1));

        while (true) {
            if (word != 0)
                return (u + 1) * BITS_PER_WORD - 1 - Long.numberOfLeadingZeros(word);
            if (u-- == 0)
                return -1;
            word = words.getValueAt(u);
        }
    }

    /**
     * Returns the index of the nearest bit that is set to {@code false} that occurs on or before the specified starting index. If no such bit exists,
     * or if {@code -1} is given as the starting index, then {@code -1} is returned.
     */
    public int previousClearBit(int fromIndex) {
        throwExceptionIfClosed();

        if (fromIndex < 0) {
            if (fromIndex == -1)
                return -1;
            throw new IndexOutOfBoundsException(
                    "fromIndex < -1: " + fromIndex);
        }

        int u = wordIndex(fromIndex);
        if (u >= getWordsInUse())
            return fromIndex;

        long word = ~words.getVolatileValueAt(u) & (WORD_MASK >>> -(fromIndex + 1));

        while (true) {
            if (word != 0)
                return (u + 1) * BITS_PER_WORD - 1 - Long.numberOfLeadingZeros(word);
            if (u-- == 0)
                return -1;
            word = ~words.getValueAt(u);
        }
    }

    /**
     * Returns <code>true</code> if the specified {@code ChronicleBitSet} has any bits set to {@code true} that are also set to {@code true} in this {@code ChronicleBitSet}.
     */
    public boolean intersects(ChronicleBitSet set) {
        throwExceptionIfClosed();

        for (int i = Math.min(getWordsInUse(), set.getWordsInUse()) - 1; i >= 0; i--)
            if ((words.getVolatileValueAt(i) & set.getWord(i)) != 0)
                return true;
        return false;
    }

    /**
     * Returns the number of bits set to {@code true} in this {@code ChronicleBitSet}.
     */
    public int cardinality() {
        throwExceptionIfClosed();

        long sum = 0;
        for (int i = 0; i < getWordsInUse(); i++)
            sum += Long.bitCount(words.getVolatileValueAt(i));
        return (int) sum;
    }

    /**
     * Performs a logical <b>AND</b> of this target bit set with the argument bit set. This bit set is modified so that each bit in it has the value
     * {@code true} if and only if it both initially had the value {@code true} and the corresponding bit in the bit set argument also had the value
     * {@code true}.
     */
    public void and(ChronicleBitSet set) {
        throwExceptionIfClosed();

        if (this == set)
            return;

        OS.memory().loadFence();
        int value = Math.toIntExact(getWordsInUse());
        while (value > set.getWordsInUse())
            words.setValueAt(--value, 0);

        // Perform logical AND on words in common
        for (int i = 0; i < value; i++)
            and(i, set.getWord(i));
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

        expandTo(wordsInUse2 - 1);

        OS.memory().loadFence();
        int i;
        for (i = 0; i < wordsInCommon; i++)
            caret(i, set.getWord(i));

        // Copy any remaining words
        for (; i < wordsInUse2; i++)
            words.setValueAt(i, set.getWord(i));
        OS.memory().storeFence();
    }

    /**
     * Clears all of the bits in this {@code ChronicleBitSet} whose corresponding bit is set in the specified {@code ChronicleBitSet}.
     */
    public void andNot(ChronicleBitSet set) {
        throwExceptionIfClosed();

        // Perform logical (a & !b) on words in common
        OS.memory().loadFence();
        for (int i = Math.min(getWordsInUse(), set.getWordsInUse()) - 1; i >= 0; i--)
            and(i, ~set.getWord(i));
        OS.memory().storeFence();
    }

    /**
     * Returns the hash code value for this bit set. The hash code depends only on which bits are set within this {@code ChronicleBitSet}.
     */
    public int hashCode() {
        long h = 1234;
        OS.memory().loadFence();
        for (int i = Math.toIntExact(getWordsInUse()); --i >= 0; )
            h ^= words.getValueAt(i) * (i + 1);

        return (int) ((h >> 32) ^ h);
    }

    /**
     * Returns the number of bits of space actually in use by this {@code ChronicleBitSet} to represent bit values. The maximum element in the set is the size
     * - 1st element.
     */
    public int size() {
        return Math.toIntExact(words.getCapacity() * BITS_PER_WORD);
    }

    /**
     * Compares this object against the specified object. The result is {@code true} if and only if the argument is not {@code null} and is a {@code
     * ChronicleBitSet} object that has exactly the same set of bits set to {@code true} as this bit set. That is, for every nonnegative {@code int} index
     * {@code k},
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
        throwExceptionIfClosed();
        wire.write("words").int64array(words.getCapacity(), words);
    }

    @Override
    public void readMarshallable(@NotNull final WireIn wire) throws IORuntimeException {
        disableThreadSafetyCheck(true);

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

    @FunctionalInterface
    interface LongFunction {
        long apply(long oldValue, long param);
    }
}
