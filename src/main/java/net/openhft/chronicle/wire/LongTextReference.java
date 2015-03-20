package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.ByteStringAppender;
import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.NativeStore;
import net.openhft.chronicle.core.values.LongValue;

import java.util.function.Supplier;

public class LongTextReference implements LongValue, Byteable {
    public static final byte[] template = "!!atomic { locked: false, value: 00000000000000000000 }".getBytes();
    public static final int FALSE = ('f' << 24) | ('a' << 16) | ('l' << 8) | 's';
    public static final int TRUE = (' ' << 24) | ('t' << 16) | ('r' << 8) | 'u';
    static final int LOCKED = 19;
    static final int VALUE = 33;
    private Bytes bytes;
    private long offset;

    {
        // todo improve later - better size this and support the offset changing
        bytes = NativeStore.nativeStore(offset + VALUE+30).bytes();
        bytes.writeOrderedInt(offset + LOCKED, FALSE);
    }


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
    public long getValue() {
        return withLock(() -> {
            return bytes.parseLong(offset + VALUE);
        });
    }

    @Override
    public void setValue(long value) {
        ByteStringAppender byteStringAppender = withLock(() -> {
            ByteStringAppender append = bytes.append(offset + VALUE, value);

            System.out.println(Bytes.toDebugString(bytes, offset + VALUE, 20));
            return append;

        });
    }

    @Override
    public long getVolatileValue() {
        return getValue();
    }

    @Override
    public void setOrderedValue(long value) {
        setValue(value);
    }

    @Override
    public long addValue(long delta) {
        return withLock(() -> {
            long value = bytes.parseLong(offset + VALUE) + delta;
            bytes.append(offset + VALUE, value);
            return value;
        });
    }

    @Override
    public long addAtomicValue(long delta) {
        return addValue(delta);
    }

    @Override
    public boolean compareAndSwapValue(long expected, long value) {
        return withLock(() -> {
            if (bytes.parseLong(offset + VALUE) == expected) {
                bytes.append(offset + VALUE, value);
                return true;
            }
            return false;
        });
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
