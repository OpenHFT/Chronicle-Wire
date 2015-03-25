package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class LongTextReference implements LongValue, Byteable {
    public static final byte[] template = "!!atomic { locked: false, value: 00000000000000000000 }".getBytes();
    public static final int FALSE = asInt("fals");
    public static final int TRUE = asInt(" tru");
    static final long UNINITIALIZED = 0x0L;
    static final int LOCKED = 19;
    static final int VALUE = 33;
    private static final int DIGITS = 20;
    private BytesStore bytes;
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
    public long getValue() {
        return withLock(() -> bytes.parseLong(offset + VALUE));
    }

    @Override
    public void setValue(long value) {
        withLock(() -> bytes.append(offset + VALUE, value, DIGITS));
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
            bytes.append(offset + VALUE, value, DIGITS);
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
                bytes.append(offset + VALUE, value, DIGITS);
                return true;
            }
            return false;
        });
    }

    @Override
    public void bytes(BytesStore bytes, long offset, long length) {
        if (length != template.length) throw new IllegalArgumentException();
        this.bytes = bytes;
        this.offset = offset;
        if (bytes.readLong(offset) == UNINITIALIZED)
            bytes.write(offset, template);
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
        return template.length;
    }

    private static int asInt(@NotNull String str) {
        ByteBuffer bb = ByteBuffer.wrap(str.getBytes(StandardCharsets.ISO_8859_1)).order(ByteOrder.nativeOrder());
        return bb.getInt();
    }

    public static void write(Bytes bytes, long value) {
        long position = bytes.position();
        bytes.write(template);
        bytes.append(position+VALUE, value, DIGITS);
    }

    public String toString() { return "value: "+getValue(); }

}
