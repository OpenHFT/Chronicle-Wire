package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.core.values.LongArrayValues;

public interface ByteableLongArrayValues extends LongArrayValues, Byteable {
    long sizeInBytes(long var1);
}
