package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.ref.LongReference;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * Created by Rob Austin
 */
public class LongValueBitSet implements Marshallable {

    /*
     * BitSets are packed into arrays of "words."  Currently a word is
     * a long, which consists of 64 bits, requiring 6 address bits.
     * The choice of word size is determined purely by performance concerns.
     */
    private final static int ADDRESS_BITS_PER_WORD = 6;
    private final static int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;

    /* Used to shift left or right for a partial word mask */
    private static final long WORD_MASK = 0xffffffffffffffffL;

    /**
     * The internal field corresponding to the serialField "bits".
     */
    LongValue[] words;

    /**
     * The number of words in the logical size of this BitSet.
     */
    IntValue wordsInUse;

    /**
     * Whether the size of "words" is user-specified.  If so, we assume
     * the user knows what he's doing and try harder to preserve it.
     */
    private transient boolean sizeIsSticky = false;

    /* use serialVersionUID from JDK 1.0.2 for interoperability */
    private static final long serialVersionUID = 7997698588986878753L;

    public LongValueBitSet(final int maxNumberOfBits) {
        int size = (maxNumberOfBits / 64) + 1;
        words = new LongValue[size];
    }

    /**
     * Given a bit index, return word index containing it.
     */                           
    private static int wordIndex(int bitIndex) {
        return bitIndex >> ADDRESS_BITS_PER_WORD;
    }

    /**
     * Every public method must preserve these invariants.
     */
    private void checkInvariants() {
    //    assert (wordsInUse.getVolatileValue() == 0 || words[wordsInUse.getVolatileValue() - 1]
   //             .getValue() != 0);
    //    assert (wordsInUse.getVolatileValue() >= 0 && wordsInUse.getVolatileValue() <= words
    //            .length);
    //    assert (wordsInUse.getVolatileValue() == words.length || words[wordsInUse
     //          .getVolatileValue()].getValue() == 0);
    }

    /**
     * Sets the field wordsInUse.getValue() to the logical size in words of the bit set.
     * WARNING:This method assumes that the number of words actually in use is
     * less than or equal to the current value of wordsInUse.getValue()!
     */
    private void recalculatewordsInUse()

    {
        // Traverse the bitset until a used word is found
        int i;
        for (i = wordsInUse.getValue() - 1; i >= 0; i--)
            if (words[i].getVolatileValue() != 0)
                break;

        wordsInUse.setOrderedValue(i + 1); // The new logical size
    }

    /**
     * Returns a new bit set containing all the bits in the given byte array.
     *
     * <p>More precisely,
     * <br>{@code BitSet.valueOf(bytes).get(n) == ((bytes[n/8] & (1<<(n%8))) != 0)}
     * <br>for all {@code n <  8 * bytes.length}.
     *
     * <p>This method is equivalent to
     * {@code BitSet.valueOf(ByteBuffer.wrap(bytes))}.
     *
     * @param bytes a byte array containing a little-endian
     *              representation of a sequence of bits to be used as the
     *              initial bits of the new bit set
     * @return a {@code BitSet} containing all the bits in the byte array
     * @since 1.7
     */
    public static BitSet valueOf(byte[] bytes) {
        return BitSet.valueOf(ByteBuffer.wrap(bytes));
    }

    /**
     * Returns a new byte array containing all the bits in this bit set.
     *
     * <p>More precisely, if
     * <br>{@code byte[] bytes = s.toByteArray();}
     * <br>then {@code bytes.length == (s.length()+7)/8} and
     * <br>{@code s.get(n) == ((bytes[n/8] & (1<<(n%8))) != 0)}
     * <br>for all {@code n < 8 * bytes.length}.
     *
     * @return a byte array containing a little-endian representation
     * of all the bits in this bit set
     * @since 1.7
     */
    public byte[] toByteArray() {
        int n = wordsInUse.getValue();
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
     * Ensures that the BitSet can hold enough words.
     *
     * @param wordsRequired the minimum acceptable number of words.
     */
    private void ensureCapacity(int wordsRequired) {
        if (words.length < wordsRequired) {
            // Allocate larger of doubled size or required size
            int request = Math.max(2 * words.length, wordsRequired);
            words = Arrays.copyOf(words, request);
            sizeIsSticky = false;
        }
    }

    /**
     * Ensures that the BitSet can accommodate a given wordIndex,
     * temporarily violating the invariants.  The caller must
     * restore the invariants before returning to the user,
     * possibly using recalculatewordsInUse.getValue()().
     *
     * @param wordIndex the index to be accommodated.
     */
    private void expandTo(int wordIndex) {
        int wordsRequired = wordIndex + 1;
        if (wordsInUse.getValue() < wordsRequired) {
            ensureCapacity(wordsRequired);
            wordsInUse.setValue(wordsRequired);
        }
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

    /**
     * Sets the bit at the specified index to the complement of its
     * current value.
     *
     * @param bitIndex the index of the bit to flip
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @since 1.4
     */
    public void flip(int bitIndex) {
        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        int wordIndex = wordIndex(bitIndex);
        expandTo(wordIndex);

        words[wordIndex].setValue(words[wordIndex].getValue() ^ (1L << bitIndex));

        recalculatewordsInUse();
        checkInvariants();
    }

    /**
     * Sets each bit from the specified {@code fromIndex} (inclusive) to the
     * specified {@code toIndex} (exclusive) to the complement of its current
     * value.
     *
     * @param fromIndex index of the first bit to flip
     * @param toIndex   index after the last bit to flip
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative,
     *                                   or {@code toIndex} is negative, or {@code fromIndex} is
     *                                   larger than {@code toIndex}
     * @since 1.4
     */
    public void flip(int fromIndex, int toIndex) {
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
            words[startWordIndex].setValue(words[startWordIndex].getValue() ^ (firstWordMask &
                    lastWordMask));
        } else {
            // Case 2: Multiple words
            // Handle first word
            words[startWordIndex].setValue(words[startWordIndex].getValue() ^ firstWordMask);

            // Handle intermediate words, if any
            for (int i = startWordIndex + 1; i < endWordIndex; i++)
                words[i].setOrderedValue(words[i].getVolatileValue() ^ WORD_MASK);

            // Handle last word
            words[endWordIndex].setValue(words[endWordIndex].getValue() ^ lastWordMask);
        }

        recalculatewordsInUse();
        checkInvariants();
    }

    /**
     * Sets the bit at the specified index to {@code true}.
     *
     * @param bitIndex a bit index
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @since JDK1.0
     */
    public void set(int bitIndex) {
        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        int wordIndex = wordIndex(bitIndex);
        expandTo(wordIndex);

        words[wordIndex].setValue(words[wordIndex].getValue() | (1L << bitIndex)); // Restores
        // invariants

        checkInvariants();
    }

    /**
     * Sets the bit at the specified index to the specified value.
     *
     * @param bitIndex a bit index
     * @param value    a boolean value to set
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @since 1.4
     */
    public void set(int bitIndex, boolean value) {
        if (value)
            set(bitIndex);
        else
            clear(bitIndex);
    }

    /**
     * Sets the bits from the specified {@code fromIndex} (inclusive) to the
     * specified {@code toIndex} (exclusive) to {@code true}.
     *
     * @param fromIndex index of the first bit to be set
     * @param toIndex   index after the last bit to be set
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative,
     *                                   or {@code toIndex} is negative, or {@code fromIndex} is
     *                                   larger than {@code toIndex}
     * @since 1.4
     */
    public void set(int fromIndex, int toIndex) {
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
            words[startWordIndex].setValue(words[startWordIndex].getValue() | (firstWordMask &
                    lastWordMask));
        } else {
            // Case 2: Multiple words
            // Handle first word
            words[startWordIndex].setValue(words[startWordIndex].getValue() | firstWordMask);

            // Handle intermediate words, if any
            for (int i = startWordIndex + 1; i < endWordIndex; i++)
                words[i].setOrderedValue(WORD_MASK);

            // Handle last word (restores invariants)
            words[endWordIndex].setValue(words[endWordIndex].getValue() | lastWordMask);
        }

        checkInvariants();
    }

    /**
     * Sets the bits from the specified {@code fromIndex} (inclusive) to the
     * specified {@code toIndex} (exclusive) to the specified value.
     *
     * @param fromIndex index of the first bit to be set
     * @param toIndex   index after the last bit to be set
     * @param value     value to set the selected bits to
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative,
     *                                   or {@code toIndex} is negative, or {@code fromIndex} is
     *                                   larger than {@code toIndex}
     * @since 1.4
     */
    public void set(int fromIndex, int toIndex, boolean value) {
        if (value)
            set(fromIndex, toIndex);
        else
            clear(fromIndex, toIndex);
    }

    /**
     * Sets the bit specified by the index to {@code false}.
     *
     * @param bitIndex the index of the bit to be cleared
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @since JDK1.0
     */
    public void clear(int bitIndex) {
        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        int wordIndex = wordIndex(bitIndex);
        if (wordIndex >= wordsInUse.getValue())
            return;

        words[wordIndex].setValue(words[wordIndex].getValue() & ~(1L << bitIndex));

        recalculatewordsInUse();
        checkInvariants();
    }

    /**
     * Sets the bits from the specified {@code fromIndex} (inclusive) to the
     * specified {@code toIndex} (exclusive) to {@code false}.
     *
     * @param fromIndex index of the first bit to be cleared
     * @param toIndex   index after the last bit to be cleared
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative,
     *                                   or {@code toIndex} is negative, or {@code fromIndex} is
     *                                   larger than {@code toIndex}
     * @since 1.4
     */
    public void clear(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);

        if (fromIndex == toIndex)
            return;

        int startWordIndex = wordIndex(fromIndex);
        if (startWordIndex >= wordsInUse.getValue())
            return;

        int endWordIndex = wordIndex(toIndex - 1);
        if (endWordIndex >= wordsInUse.getValue()) {
            toIndex = length();
            endWordIndex = wordsInUse.getValue() - 1;
        }

        long firstWordMask = WORD_MASK << fromIndex;
        long lastWordMask = WORD_MASK >>> -toIndex;
        if (startWordIndex == endWordIndex) {
            // Case 1: One word
            words[startWordIndex].setValue(words[startWordIndex].getValue() & ~(firstWordMask &
                    lastWordMask));
        } else {
            // Case 2: Multiple words
            // Handle first word
            words[startWordIndex].setValue(words[startWordIndex].getValue() & ~firstWordMask);

            // Handle intermediate words, if any
            for (int i = startWordIndex + 1; i < endWordIndex; i++)
                words[i].setOrderedValue(0);

            // Handle last word
            words[endWordIndex].setValue(words[endWordIndex].getValue() & ~lastWordMask);
        }

        recalculatewordsInUse();
        checkInvariants();
    }

    /**
     * Sets all of the bits in this BitSet to {@code false}.
     *
     * @since 1.4
     */
    public void clear() {
        int value = wordsInUse.getValue();
        while (value > 0)
            words[--value].setValue(0);
        wordsInUse.setValue(value);
    }

    /**
     * Returns the value of the bit with the specified index. The value
     * is {@code true} if the bit with the index {@code bitIndex}
     * is currently set in this {@code BitSet}; otherwise, the result
     * is {@code false}.
     *
     * @param bitIndex the bit index
     * @return the value of the bit with the specified index
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    public boolean get(int bitIndex) {
        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        checkInvariants();

        int wordIndex = wordIndex(bitIndex);
        return (wordIndex < wordsInUse.getValue())
                && ((words[wordIndex].getValue() & (1L << bitIndex)) != 0);
    }

    /**
     * Returns the index of the first bit that is set to {@code true}
     * that occurs on or after the specified starting index. If no such
     * bit exists then {@code -1} is returned.
     *
     * <p>To iterate over the {@code true} bits in a {@code BitSet},
     * use the following loop:
     *
     * <pre> {@code
     * for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
     *     // operate on index i here
     *     if (i == Integer.MAX_VALUE) {
     *         break; // or (i+1) would overflow
     *     }
     * }}</pre>
     *
     * @param fromIndex the index to start checking from (inclusive)
     * @return the index of the next set bit, or {@code -1} if there
     * is no such bit
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @since 1.4
     */
    public int nextSetBit(int fromIndex) {
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);

        checkInvariants();

        int u = wordIndex(fromIndex);
        if (u >= wordsInUse.getValue())
            return -1;

        long word = words[u].getValue() & (WORD_MASK << fromIndex);

        while (true) {
            if (word != 0)
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            if (++u == wordsInUse.getValue())
                return -1;
            word = words[u].getValue();
        }
    }

    /**
     * Returns the index of the first bit that is set to {@code false}
     * that occurs on or after the specified starting index.
     *
     * @param fromIndex the index to start checking from (inclusive)
     * @return the index of the next clear bit
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @since 1.4
     */
    public int nextClearBit(int fromIndex) {
        // Neither spec nor implementation handle bitsets of maximal length.
        // See 4816253.
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);

        checkInvariants();

        int u = wordIndex(fromIndex);
        if (u >= wordsInUse.getValue())
            return fromIndex;

        long word = ~words[u].getValue() & (WORD_MASK << fromIndex);

        while (true) {
            if (word != 0)
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            if (++u == wordsInUse.getValue())
                return wordsInUse.getValue() * BITS_PER_WORD;
            word = ~words[u].getValue();
        }
    }

    /**
     * Returns the index of the nearest bit that is set to {@code true}
     * that occurs on or before the specified starting index.
     * If no such bit exists, or if {@code -1} is given as the
     * starting index, then {@code -1} is returned.
     *
     * <p>To iterate over the {@code true} bits in a {@code BitSet},
     * use the following loop:
     *
     * <pre> {@code
     * for (int i = bs.length(); (i = bs.previousSetBit(i-1)) >= 0; ) {
     *     // operate on index i here
     * }}</pre>
     *
     * @param fromIndex the index to start checking from (inclusive)
     * @return the index of the previous set bit, or {@code -1} if there
     * is no such bit
     * @throws IndexOutOfBoundsException if the specified index is less
     *                                   than {@code -1}
     * @since 1.7
     */
    public int previousSetBit(int fromIndex) {
        if (fromIndex < 0) {
            if (fromIndex == -1)
                return -1;
            throw new IndexOutOfBoundsException(
                    "fromIndex < -1: " + fromIndex);
        }

        checkInvariants();

        int u = wordIndex(fromIndex);
        if (u >= wordsInUse.getValue())
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
     * Returns the index of the nearest bit that is set to {@code false}
     * that occurs on or before the specified starting index.
     * If no such bit exists, or if {@code -1} is given as the
     * starting index, then {@code -1} is returned.
     *
     * @param fromIndex the index to start checking from (inclusive)
     * @return the index of the previous clear bit, or {@code -1} if there
     * is no such bit
     * @throws IndexOutOfBoundsException if the specified index is less
     *                                   than {@code -1}
     * @since 1.7
     */
    public int previousClearBit(int fromIndex) {
        if (fromIndex < 0) {
            if (fromIndex == -1)
                return -1;
            throw new IndexOutOfBoundsException(
                    "fromIndex < -1: " + fromIndex);
        }

        checkInvariants();

        int u = wordIndex(fromIndex);
        if (u >= wordsInUse.getValue())
            return fromIndex;

        long word = ~words[u].getValue() & (WORD_MASK >>> -(fromIndex + 1));

        while (true) {
            if (word != 0)
                return (u + 1) * BITS_PER_WORD - 1 - Long.numberOfLeadingZeros(word);
            if (u-- == 0)
                return -1;
            word = ~words[u].getValue();
        }
    }

    /**
     * Returns the "logical size" of this {@code BitSet}: the index of
     * the highest set bit in the {@code BitSet} plus one. Returns zero
     * if the {@code BitSet} contains no set bits.
     *
     * @return the logical size of this {@code BitSet}
     * @since 1.2
     */
    public int length() {
        if (wordsInUse.getValue() == 0)
            return 0;

        return BITS_PER_WORD * (wordsInUse.getValue() - 1) +
                (BITS_PER_WORD - Long.numberOfLeadingZeros(words[wordsInUse.getValue() - 1].getValue()));
    }

    /**
     * Returns true if this {@code BitSet} contains no bits that are set
     * to {@code true}.
     *
     * @return boolean indicating whether this {@code BitSet} is empty
     * @since 1.4
     */
    public boolean isEmpty() {
        return wordsInUse.getValue() == 0;
    }

    /**
     * Returns true if the specified {@code BitSet} has any bits set to
     * {@code true} that are also set to {@code true} in this {@code BitSet}.
     *
     * @param set {@code BitSet} to intersect with
     * @return boolean indicating whether this {@code BitSet} intersects
     * the specified {@code BitSet}
     * @since 1.4
     */
    public boolean intersects(LongValueBitSet set) {
        for (int i = Math.min(wordsInUse.getValue(), set.wordsInUse.getValue()) - 1; i >= 0; i--)
            if ((words[i].getVolatileValue() & set.words[i].getVolatileValue()) != 0)
                return true;
        return false;
    }

    /**
     * Returns the number of bits set to {@code true} in this {@code BitSet}.
     *
     * @return the number of bits set to {@code true} in this {@code BitSet}
     * @since 1.4
     */
    public int cardinality() {
        int sum = 0;
        for (int i = 0; i < wordsInUse.getValue(); i++)
            sum += Long.bitCount(words[i].getVolatileValue());
        return sum;
    }

    /**
     * Performs a logical <b>AND</b> of this target bit set with the
     * argument bit set. This bit set is modified so that each bit in it
     * has the value {@code true} if and only if it both initially
     * had the value {@code true} and the corresponding bit in the
     * bit set argument also had the value {@code true}.
     *
     * @param set a bit set
     */
    public void and(LongValueBitSet set) {
        if (this == set)
            return;

        int value = wordsInUse.getValue();
        while (wordsInUse.getValue() > set.wordsInUse.getValue()) {
            words[--value].setValue(0);
        }
        wordsInUse.setValue(value);

        // Perform logical AND on words in common
        for (int i = 0; i < wordsInUse.getValue(); i++)
            words[i].setOrderedValue(words[i].getVolatileValue() & set.words[i].getVolatileValue());

        recalculatewordsInUse();
        checkInvariants();
    }

    /**
     * Performs a logical <b>OR</b> of this bit set with the bit set
     * argument. This bit set is modified so that a bit in it has the
     * value {@code true} if and only if it either already had the
     * value {@code true} or the corresponding bit in the bit set
     * argument has the value {@code true}.
     *
     * @param set a bit set
     */
    public void or(LongValueBitSet set) {
        if (this == set)
            return;

        int wordsInCommon = Math.min(wordsInUse.getValue(), set.wordsInUse.getValue());

        if (wordsInUse.getValue() < set.wordsInUse.getValue()) {
            ensureCapacity(set.wordsInUse.getValue());
            wordsInUse.setValue(set.wordsInUse.getValue());
        }

        // Perform logical OR on words in common
        for (int i = 0; i < wordsInCommon; i++)
            words[i].setOrderedValue(words[i].getVolatileValue() | set.words[i].getVolatileValue());

        // Copy any remaining words
        if (wordsInCommon < set.wordsInUse.getValue())
            System.arraycopy(set.words, wordsInCommon,
                    words, wordsInCommon,
                    wordsInUse.getValue() - wordsInCommon);

        // recalculatewordsInUse.getValue()() is unnecessary
        checkInvariants();
    }

    /**
     * Performs a logical <b>XOR</b> of this bit set with the bit set
     * argument. This bit set is modified so that a bit in it has the
     * value {@code true} if and only if one of the following
     * statements holds:
     * <ul>
     * <li>The bit initially has the value {@code true}, and the
     * corresponding bit in the argument has the value {@code false}.
     * <li>The bit initially has the value {@code false}, and the
     * corresponding bit in the argument has the value {@code true}.
     * </ul>
     *
     * @param set a bit set
     */
    public void xor(LongValueBitSet set) {
        int wordsInCommon = Math.min(wordsInUse.getValue(), set.wordsInUse.getValue());

        if (wordsInUse.getValue() < set.wordsInUse.getValue()) {
            ensureCapacity(set.wordsInUse.getValue());
            wordsInUse.setValue(set.wordsInUse.getValue());
        }

        // Perform logical XOR on words in common
        for (int i = 0; i < wordsInCommon; i++)
            words[i].setOrderedValue(words[i].getVolatileValue() ^ set.words[i].getVolatileValue());

        // Copy any remaining words
        if (wordsInCommon < set.wordsInUse.getValue())
            System.arraycopy(set.words, wordsInCommon,
                    words, wordsInCommon,
                    set.wordsInUse.getValue() - wordsInCommon);

        recalculatewordsInUse();
        checkInvariants();
    }

    /**
     * Clears all of the bits in this {@code BitSet} whose corresponding
     * bit is set in the specified {@code BitSet}.
     *
     * @param set the {@code BitSet} with which to mask this
     *            {@code BitSet}
     * @since 1.2
     */
    public void andNot(LongValueBitSet set) {
        // Perform logical (a & !b) on words in common
        for (int i = Math.min(wordsInUse.getValue(), set.wordsInUse.getValue()) - 1; i >= 0; i--)
            words[i].setOrderedValue(words[i].getVolatileValue() & ~set.words[i].getVolatileValue());

        recalculatewordsInUse();
        checkInvariants();
    }

    /**
     * Returns the hash code value for this bit set. The hash code depends
     * only on which bits are set within this {@code BitSet}.
     *
     * <p>The hash code is defined to be the result of the following
     * calculation:
     * <pre> {@code
     * public int hashCode() {
     *     long h = 1234;
     *     long[] words = toLongArray();
     *     for (int i = words.length; --i >= 0; )
     *         h ^= words[i] * (i + 1);
     *     return (int)((h >> 32) ^ h);
     * }}</pre>
     * Note that the hash code changes if the set of bits is altered.
     *
     * @return the hash code value for this bit set
     */
    public int hashCode() {
        long h = 1234;
        for (int i = wordsInUse.getValue(); --i >= 0; )
            h ^= words[i].getVolatileValue() * (i + 1);

        return (int) ((h >> 32) ^ h);
    }

    /**
     * Returns the number of bits of space actually in use by this
     * {@code BitSet} to represent bit values.
     * The maximum element in the set is the size - 1st element.
     *
     * @return the number of bits currently in this bit set
     */
    public int size() {
        return words.length * BITS_PER_WORD;
    }

    /**
     * Compares this object against the specified object.
     * The result is {@code true} if and only if the argument is
     * not {@code null} and is a {@code Bitset} object that has
     * exactly the same set of bits set to {@code true} as this bit
     * set. That is, for every nonnegative {@code int} index {@code k},
     * <pre>((BitSet)obj).get(k) == this.get(k)</pre>
     * must be true. The current sizes of the two bit sets are not compared.
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are the same;
     * {@code false} otherwise
     * @see #size()
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof LongValueBitSet))
            return false;
        if (this == obj)
            return true;

        LongValueBitSet set = (LongValueBitSet) obj;

        checkInvariants();
        set.checkInvariants();

        if (wordsInUse.getValue() != set.wordsInUse.getValue())
            return false;

        // Check words in use by both BitSets
        for (int i = 0; i < wordsInUse.getValue(); i++)
            if (words[i].getVolatileValue() != set.words[i].getVolatileValue())
                return false;

        return true;
    }

    /**
     * Attempts to reduce internal storage used for the bits in this bit set.
     * Calling this method may, but is not required to, affect the value
     * returned by a subsequent call to the {@link #size()} method.
     */
    private void trimToSize() {
        if (wordsInUse.getValue() != words.length) {
            words = Arrays.copyOf(words, wordsInUse.getValue());
            checkInvariants();
        }
    }

    /**
     * Save the state of the {@code BitSet} instance to a stream (i.e.,
     * serialize it).
     */
    private void writeObject(ObjectOutputStream s)
            throws IOException {

        checkInvariants();

        if (!sizeIsSticky)
            trimToSize();

        ObjectOutputStream.PutField fields = s.putFields();
        fields.put("bits", words);
        s.writeFields();
    }

    /**
     * Returns a string representation of this bit set. For every index
     * for which this {@code BitSet} contains a bit in the set
     * state, the decimal representation of that index is included in
     * the result. Such indices are listed in order from lowest to
     * highest, separated by ",&nbsp;" (a comma and a space) and
     * surrounded by braces, resulting in the usual mathematical
     * notation for a set of integers.
     *
     * <p>Example:
     * <pre>
     * BitSet drPepper = new BitSet();</pre>
     * Now {@code drPepper.toString()} returns "{@code {}}".
     * <pre>
     * drPepper.set(2);</pre>
     * Now {@code drPepper.toString()} returns "{@code {2}}".
     * <pre>
     * drPepper.set(4);
     * drPepper.set(10);</pre>
     * Now {@code drPepper.toString()} returns "{@code {2, 4, 10}}".
     *
     * @return a string representation of this bit set
     */
    public String toString() {
        checkInvariants();

        int numBits = (wordsInUse.getValue() > 128) ?
                cardinality() : wordsInUse.getValue() * BITS_PER_WORD;
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
     * Returns a stream of indices for which this {@code BitSet}
     * contains a bit in the set state. The indices are returned
     * in order, from lowest to highest. The size of the stream
     * is the number of bits in the set state, equal to the value
     * returned by the {@link #cardinality()} method.
     *
     * <p>The bit set must remain constant during the execution of the
     * terminal stream operation.  Otherwise, the result of the terminal
     * stream operation is undefined.
     *
     * @return a stream of integers representing set indices
     * @since 1.8
     */
    public IntStream stream() {

        class BitSetIterator implements PrimitiveIterator.OfInt {
            int next = nextSetBit(0);

            @Override
            public boolean hasNext() {
                return next != -1;
            }

            @Override
            public int nextInt() {
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
        try (DocumentContext dc = wire.writingDocument()) {

            dc.wire().consumePadding();
            wire.write("numberOfLongValues").int32(words.length);
            wire.write("wordsInUse").int32forBinding(wordsInUse == null ? 0 : wordsInUse.getValue
                    ());
            dc.wire().consumePadding();

            for (int i = 0; i < words.length; i++) {
                if (words[i] == null)
                    words[i] = wire.newLongReference();
                wire.getValueOut().int64forBinding(words[i].getValue());
            }
        }

    }

    @Override
    public void readMarshallable(@NotNull final WireIn wire) throws IORuntimeException {

        try (DocumentContext dc = wire.readingDocument()) {
            dc.wire().padToCacheAlign();
            int numberOfLongValues = wire.read("numberOfLongValues").int32();
            this.wordsInUse = wire.read("wordsInUse").int32ForBinding((IntValue) null);
            dc.wire().padToCacheAlign();
            words = new LongReference[numberOfLongValues];
            for (int i = 0; i < numberOfLongValues; i++) {
                words[i] = wire.getValueIn().int64ForBinding(null);
            }
        }
    }


}
