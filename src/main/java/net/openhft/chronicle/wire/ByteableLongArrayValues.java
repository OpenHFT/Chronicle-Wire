package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.core.values.LongArrayValues;

/**
 * Created by Rob Austin
 */
public interface ByteableLongArrayValues extends LongArrayValues, Byteable {

    /**
     * @param capacity the number of elements of the array
     * @return the length in bytes to hold this {@code capacity} of elements
     */
    long sizeInBytes(long capacity);
}
