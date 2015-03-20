package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.values.IntValue;

import java.util.function.Supplier;

public class IntTextReference implements IntValue, Byteable {
    public static final byte[] template = "!!atomic { locked: false, value: 0000000000 }".getBytes();
    public static final int FALSE = ('f' << 24) | ('a' << 16) | ('l' << 8) | 's';
    public static final int TRUE = (' ' << 24) | ('t' << 16) | ('r' << 8) | 'u';
    static final int LOCKED = 19;
    static final int VALUE = 33;
    private Bytes bytes;
    private long offset;

    <T> T withLock(Supplier<T> call) {
        long valueOffset = offset + LOCKED;
        int value = bytes.readVolatileInt(valueOffset);
        if (value != FALSE && value != TRUE)
            throw new IllegalStateException();
        while (true) {
            if (bytes.compareAndSwapInt(valueOffset, FALSE, TRUE)) {
                T t = call.get();
                bytes.writeOrderedInt(valueOffset, FALSE);
                return t;
            }
        }
    }

    @Override
    public int getValue() {
        return withLock(() -> (int) bytes.parseLong(offset + VALUE));
    }

    @Override
    public void setValue(int value) {
        withLock(() -> bytes.append(offset + VALUE, value));
    }

    @Override
    public int getVolatileValue() {
        return getValue();
    }

    @Override
    public void setOrderedValue(int value) {
        setValue(value);
    }

    @Override
    public int addValue(int delta) {
        return withLock(() -> {
            long value = bytes.parseLong(offset + VALUE) + delta;
            bytes.append(offset + VALUE, value);
            return (int) value;
        });
    }

    @Override
    public int addAtomicValue(int delta) {
        return addValue(delta);
    }

    @Override
    public boolean compareAndSwapValue(int expected, int value) {
        return withLock(() -> {
            if (bytes.parseLong(offset + VALUE) == expected) {
                bytes.append(offset + VALUE, value);
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean tryLockValue() {
        return false;
    }

    @Override
    public boolean tryLockNanosValue(long nanos) {
        return false;
    }

    @Override
    public void busyLockValue() throws InterruptedException, IllegalStateException {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public void unlockValue() throws IllegalMonitorStateException {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public void bytes(Bytes bytes, long offset, long length) {
        if (length != template.length) throw new IllegalArgumentException();
        this.bytes = bytes;
        this.offset = offset;
    }

    @Override
    public Bytes bytes() {
        return bytes;
    }

    @Override
    public long offset() {
        return offset;
    }

    @Override
    public long maxSize() {
        return template.length;
    }
}
