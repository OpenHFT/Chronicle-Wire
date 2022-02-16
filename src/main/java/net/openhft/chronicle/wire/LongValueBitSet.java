/*
 * Copyright 2016-2020 chronicle.software
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
 * This <code>BitSet</code> is intended to be shared between processes. To minimize locking constraints, it is implemented as a lock-free solution
 * without support for resizing.
 */
public class LongValueBitSet extends AbstractCloseable implements Marshallable, ChronicleBitSet {

    /* Used to shift left or right for a partial word mask */
    private static final long WORD_MASK = ~0L;
    private transient Pauser pauser = Pauser.busy();

    /**
     * The internal field corresponding to the serialField "bits".
     */
    private LongValue[] words;

    public LongValueBitSet(final int maxNumberOfBits) {
        this((long) maxNumberOfBits);
    }

    public LongValueBitSet(final int maxNumberOfBits, Wire w) {
        this((long) maxNumberOfBits, w);
    }

    public LongValueBitSet(final long maxNumberOfBits) {
        int size = (int) ((maxNumberOfBits + BITS_PER_WORD - 1) / BITS_PER_WORD);
        words = new LongValue[size];
        disableThreadSafetyCheck(true);
    }

    public LongValueBitSet(final long maxNumberOfBits, Wire w) {
        this(maxNumberOfBits);
        writeMarshallable(w);
        readMarshallable(w);
    }

    /**
     * Given a bit index, return word index containing it.
     */
    private static int wordIndex(int bitIndex) {
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

    public int getWordsInUse() {
        return words.length;
    }

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
     * Ensures that the ChronicleBitSet can accommodate a given wordIndex.
     */
    private void expandTo(int wordIndex) {
        int wordsRequired = wordIndex + 1;
        if (getWordsInUse() < wordsRequired) {
            throw new UnsupportedOperationException("todo: it is not possible currently to expand " +
                    "this stucture, because if its concurrent nature and have to implement cross " +
                    "process locking");
        }
    }

    /**
     * Sets the bit at the specified index to the complement of its current value.
     */
    public void flip(int bitIndex) {
        throwExceptionIfClosed();

        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        int wordIndex = wordIndex(bitIndex);
        expandTo(wordIndex);
        caret(words[wordIndex], 1L << bitIndex);
    }

    private void caret(LongValue word, long param) {
        set(word, param, (x, y) -> x ^ y);
    }

    private void and(LongValue word, final long param) {
        set(word, param, (x, y) -> x & y);
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
     */
    public void set(int bitIndex) {
        throwExceptionIfClosed();

        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        int wordIndex = wordIndex(bitIndex);

        pipe(words[wordIndex], (1L << bitIndex)); // Restores
    }

    private void pipe(LongValue word, long param) {
        set(word, param, (x, y) -> x | y);
    }

    /**
     * Sets the bit at the specified index to the specified value.
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

        // Increase capacity if necessary
        int startWordIndex = wordIndex(fromIndex);
        int endWordIndex = wordIndex(toIndex - 1);
        expandTo(endWordIndex);

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

        int endWordIndex = wordIndex(toIndex - 1);
        if (endWordIndex >= getWordsInUse()) {
            toIndex = length();
            endWordIndex = getWordsInUse() - 1;
        }

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
     * Returns the value of the bit with the specified index. The value is {@code true} if the bit with the index {@code bitIndex} is currently set in
     * this {@code ChronicleBitSet}; otherwise, the result is {@code false}.
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
     * Returns the index of the first bit that is set to {@code true} that occurs on or after the specified starting index. If no such bit exists then
     * {@code -1} is returned.
     */
    public int nextSetBit(int fromIndex) {
        throwExceptionIfClosed();

        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);

        int u = wordIndex(fromIndex);
        if (u >= getWordsInUse())
            return -1;

        long word = words[u].getVolatileValue() & (WORD_MASK << fromIndex);

        while (true) {
            if (word != 0)
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            if (++u == getWordsInUse())
                return -1;
            word = words[u].getVolatileValue();
        }
    }

    /**
     * Returns the index of the first bit that is set to {@code true} that occurs on or after the specified starting index. If no such bit exists then
     * {@code -1} is returned.
     */
    public int nextSetBit(int fromIndex, int toIndex) {
        throwExceptionIfClosed();

        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);

        int u = wordIndex(fromIndex);
        if (u >= getWordsInUse())
            return -1;

        long word = words[u].getVolatileValue() & (WORD_MASK << fromIndex);

        while (true) {
            if (word != 0)
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            if (++u == getWordsInUse())
                return -1;
            if (u * BITS_PER_WORD > toIndex)
                return -1;
            word = words[u].getVolatileValue();
        }
    }

    /**
     * Returns the index of the first bit that is set to {@code false} that occurs on or after the specified starting index.
     */
    public int nextClearBit(int fromIndex) {
        throwExceptionIfClosed();

        // Neither spec nor implementation handle ChronicleBitSets of maximal length.
        // See 4816253.
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);

        int u = wordIndex(fromIndex);
        if (u >= getWordsInUse())
            return fromIndex;

        long word = ~words[u].getVolatileValue() & (WORD_MASK << fromIndex);

        while (true) {
            if (word != 0)
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            if (++u == getWordsInUse())
                return getWordsInUse() * BITS_PER_WORD;
            word = ~words[u].getValue();
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

        long word = words[u].getValue() & (WORD_MASK >>> -(fromIndex + 1));

        while (true) {
            if (word != 0)
                return (u + 1) * BITS_PER_WORD - 1 - Long.numberOfLeadingZeros(word);
            if (u-- == 0)
                return -1;
            word = words[u].getValue();
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

        long word = ~words[u].getVolatileValue() & (WORD_MASK >>> -(fromIndex + 1));

        while (true) {
            if (word != 0)
                return (u + 1) * BITS_PER_WORD - 1 - Long.numberOfLeadingZeros(word);
            if (u-- == 0)
                return -1;
            word = ~words[u].getValue();
        }
    }

    /**
     * Returns <code>true</code> if the specified {@code ChronicleBitSet} has any bits set to {@code true} that are also set to {@code true} in this {@code ChronicleBitSet}.
     */
    public boolean intersects(ChronicleBitSet set) {
        throwExceptionIfClosed();

        for (int i = Math.min(getWordsInUse(), set.getWordsInUse()) - 1; i >= 0; i--)
            if ((words[i].getVolatileValue() & set.getWord(i)) != 0)
                return true;
        return false;
    }

    public boolean intersects(LongValueBitSet set) {
        return intersects((ChronicleBitSet) set);
    }

    /**
     * Returns the number of bits set to {@code true} in this {@code ChronicleBitSet}.
     */
    public int cardinality() {
        throwExceptionIfClosed();

        int sum = 0;
        for (int i = 0; i < getWordsInUse(); i++)
            sum += Long.bitCount(words[i].getVolatileValue());
        return sum;
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

        int value = getWordsInUse();
        while (value > set.getWordsInUse()) {
            words[--value].setValue(0);
        }

        // Perform logical AND on words in common
        for (int i = 0; i < getWordsInUse(); i++)
            and(words[i], set.getWord(i));
    }

    public void and(LongValueBitSet set) {
        and((ChronicleBitSet) set);
    }

    /**
     * Performs a logical <b>OR</b> of this bit set with the bit set argument. This bit set is modified so that a bit in it has the value {@code true}
     * if and only if it either already had the value {@code true} or the corresponding bit in the bit set argument has the value {@code true}.
     */
    public void or(LongValueBitSet set) {
        or((ChronicleBitSet) set);
    }

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

        int wordsInCommon = Math.min(getWordsInUse(), set.getWordsInUse());

        OS.memory().loadFence();
        int i;
        for (i = 0; i < wordsInCommon; i++)
            caret(words[i], set.getWord(i));

        // Copy any remaining words
        for (; i < set.getWordsInUse(); i++)
            setWord(i, set.getWord(i));
        OS.memory().storeFence();
    }

    public void xor(LongValueBitSet set) {
        xor((ChronicleBitSet) set);
    }

    /**
     * Clears all of the bits in this {@code ChronicleBitSet} whose corresponding bit is set in the specified {@code ChronicleBitSet}.
     */
    public void andNot(ChronicleBitSet set) {
        throwExceptionIfClosed();

        // Perform logical (a & !b) on words in common
        OS.memory().loadFence();
        for (int i = Math.min(getWordsInUse(), set.getWordsInUse()) - 1; i >= 0; i--)
            and(words[i], ~set.getWord(i));
        OS.memory().storeFence();
    }

    public void andNot(LongValueBitSet set) {
        andNot((ChronicleBitSet) set);
    }

    /**
     * Returns the hash code value for this bit set. The hash code depends only on which bits are set within this {@code ChronicleBitSet}.
     */
    public int hashCode() {
        long h = 1234;
        OS.memory().loadFence();
        for (int i = getWordsInUse(); --i >= 0; )
            h ^= words[i].getValue() * (i + 1);

        return (int) ((h >> 32) ^ h);
    }

    /**
     * Returns the number of bits of space actually in use by this {@code ChronicleBitSet} to represent bit values. The maximum element in the set is the size
     * - 1st element.
     */
    public int size() {
        return words.length * BITS_PER_WORD;
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

        int numBits = (getWordsInUse() > 128) ?
                cardinality() : getWordsInUse() * BITS_PER_WORD;
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
        disableThreadSafetyCheck(true);
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

    @FunctionalInterface
    interface LongFunction {
        long apply(long oldValue, long param);
    }
}
