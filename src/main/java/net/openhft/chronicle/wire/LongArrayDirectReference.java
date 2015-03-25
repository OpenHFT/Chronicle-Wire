package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.values.LongArrayValues;

public class LongArrayDirectReference implements LongArrayValues, Byteable {
    private static final long CAPACITY = 0;
    private static final long VALUES = 8;
    private BytesStore bytes;
    private long offset;
    private long length = VALUES;

    @Override
    public long getCapacity() {
        return (length - VALUES) >>> 3;
    }

    @Override
    public long getValueAt(long index) {
        return bytes.readLong(VALUES + offset + index << 3);
    }

    @Override
    public void setValueAt(long index, long value) {
        bytes.writeLong(VALUES + offset + index << 3, value);
    }

    @Override
    public long getVolatileValueAt(long index) {
        return bytes.readVolatileLong(VALUES + offset + index << 3);
    }

    @Override
    public void setOrderedValueAt(long index, long value) {
        bytes.writeOrderedLong(VALUES + offset + index << 3, value);
    }

    @Override
    public void bytesStore(BytesStore bytes, long offset, long length) {
        if (length != peakLength(bytes, offset))
            throw new IllegalArgumentException(length + " != " + peakLength(bytes, offset));
        this.bytes = bytes;
        this.offset = offset;
        this.length = length;
    }

    public static long peakLength(BytesStore bytes, long offset) {
        return (bytes.readLong(offset + CAPACITY) << 3) + VALUES;
    }

    @Override
    public BytesStore bytesStore() {
        return bytes;
    }

    @Override
    public long offset() {
        return offset;
    }

    @Override
    public long maxSize() {
        return length;
    }

    public String toString() {
        return "value: " + getValueAt(0) + " ...";
    }

    public static void write(Bytes bytes, long capacity) {
        bytes.writeLong(capacity);
        long start = bytes.position() + VALUES;
        bytes.zeroOut(start, start + (capacity << 3));
        bytes.skip(capacity << 3);
    }

    public static void lazyWrite(Bytes bytes, long capacity) {
        bytes.writeLong(capacity);
        bytes.skip(capacity << 3);
    }
}
