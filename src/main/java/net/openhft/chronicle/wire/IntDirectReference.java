package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.values.IntValue;

public class IntDirectReference implements IntValue, Byteable {
    private BytesStore bytes;
    private long offset;

    @Override
    public int getValue() {
        return bytes.readInt(offset);
    }

    @Override
    public void setValue(int value) {
        bytes.writeInt(offset, value);
    }

    @Override
    public int getVolatileValue() {
        return bytes.readVolatileInt(offset);
    }

    @Override
    public void setOrderedValue(int value) {
        bytes.writeOrderedInt(offset, value);
    }

    @Override
    public int addValue(int delta) {
        return bytes.addAndGetInt(offset, delta);
    }

    @Override
    public int addAtomicValue(int delta) {
        return addValue(delta);
    }

    @Override
    public boolean compareAndSwapValue(int expected, int value) {
        return bytes.compareAndSwapLong(offset, expected, value);
    }

    @Override
    public void bytes(BytesStore bytes, long offset, long length) {
        if (length != maxSize()) throw new IllegalArgumentException();
        this.bytes = bytes;
        this.offset = offset;
    }

    @Override
    public BytesStore bytes() {
        return bytes;
    }

    @Override
    public long offset() {
        return offset;
    }

    @Override
    public long maxSize() {
        return 4;
    }
}
