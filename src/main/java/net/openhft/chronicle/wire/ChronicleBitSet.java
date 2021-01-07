package net.openhft.chronicle.wire;

public interface ChronicleBitSet extends Marshallable {
    /**
     * Returns the number of bits of space actually in use by this {@code BitSet} to represent bit values. The maximum element in the set is the size
     * - 1st element.
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
     * Returns the value of the bit with the specified index. The value is {@code true} if the bit with the index {@code bitIndex} is currently set in
     * this {@code BitSet}; otherwise, the result is {@code false}.
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
     * <p>To iterate over the {@code true} bits in a {@code BitSet},
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

    /**
     * Sets all of the bits in this BitSet to {@code false}.
     */
    void clear();
}
