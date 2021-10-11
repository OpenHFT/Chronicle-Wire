package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.Closeable;

public interface ChronicleBitSet extends Marshallable, Closeable {
    int BITS_PER_WORD = Long.BYTES * 8;

    /**
     * Returns the number of bits of space actually in use by this {@code ChronicleBitSet} to represent bit values.
     * The maximum element in the set is the size - 1st element.
     *
     * @return the number of bits currently in this bit set
     */
    int size();

    /**
     * Sets the bit at the specified index to {@code true}.
     *
     * @param bitIndex a bit index
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    void set(int bitIndex);

    /**
     * Sets the bit at the specified index to the specified value.
     *
     * @param bitIndex a bit index
     * @param value    a boolean value to set
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    default void set(int bitIndex, boolean value) {
        if (value)
            set(bitIndex);
        else
            clear(bitIndex);
    }

    void set(int fromIndex, int toIndex);

    /**
     * Sets the bits from the specified {@code fromIndex} (inclusive) to the specified {@code toIndex} (exclusive) to the specified value.
     *
     * @param fromIndex index of the first bit to be set
     * @param toIndex   index after the last bit to be set
     * @param value     value to set the selected bits to
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, or {@code toIndex} is negative,
     *                                   or {@code fromIndex} is larger than {@code toIndex}
     */
    default void set(int fromIndex, int toIndex, boolean value) {
        if (value)
            set(fromIndex, toIndex);
        else
            clear(fromIndex, toIndex);
    }


    /**
     * Returns the value of the bit with the specified index. The value is {@code true} if the bit with the index {@code bitIndex} is currently set in
     * this {@code ChronicleBitSet}; otherwise, the result is {@code false}.
     *
     * @param bitIndex the bit index
     * @return the value of the bit with the specified index
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    boolean get(int bitIndex);

    /**
     * Sets the bit specified by the index to {@code false}.
     *
     * @param bitIndex the index of the bit to be cleared
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    void clear(int bitIndex);

    /**
     * Returns the index of the first bit that is set to {@code true} that occurs on or after the specified starting index. If no such bit exists then
     * {@code -1} is returned.
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
     * @param fromIndex the index to start checking from (inclusive)
     * @param toIndex   (inclusive) returns -1 if a bit is not found before this value
     * @return the index of the next set bit, or {@code -1} if there is no such bit
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    int nextSetBit(int fromIndex, int toIndex);

    int nextSetBit(int fromIndex);

    /**
     * Sets all the bits in this ChronicleBitSet to {@code false}.
     */
    void clear();

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
                return BITS_PER_WORD * (i + 1) - Long.numberOfLeadingZeros(word);
            }
        }
        return 0;
    }

    int cardinality();

    int nextClearBit(int index);

    void flip(int index);

    void flip(int fromIndex, int toIndex);

    void clear(int fromIndex, int toIndex);

    int getWordsInUse();

    long getWord(int wordIndex);

    void setWord(int wordIndex, long bits);

    void andNot(ChronicleBitSet bitSet);

    void and(ChronicleBitSet bitSet);

    void xor(ChronicleBitSet bitSet);

    void or(ChronicleBitSet bitSet);

    boolean intersects(ChronicleBitSet bitSet);

    void copyFrom(ChronicleBitSet bitSet);

//    ChronicleBitSet get(int rangeStart, int rangeEnd);
}
