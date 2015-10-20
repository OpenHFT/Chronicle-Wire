/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * reference to an array fo 32-bit in values in Text wire format.
 */
public class TextLongReference implements LongValue, Byteable {
    private static final byte[] template = "!!atomic { locked: false, value: 00000000000000000000 }".getBytes();
    private static final int FALSE = BytesUtil.asInt("fals");
    private static final int TRUE = BytesUtil.asInt(" tru");
    private static final long UNINITIALIZED = 0x0L;
    private static final int LOCKED = 19;
    static final int VALUE = 33;
    private static final int DIGITS = 20;
    private BytesStore bytes;
    private long offset;

    public static void write(@NotNull Bytes bytes, long value) {
        long position = bytes.writePosition();
        bytes.write(template);
        bytes.append(position + VALUE, value, DIGITS);
    }

    private <T> T withLock(@NotNull Supplier<T> call) {
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
    public void bytesStore(@NotNull BytesStore bytes, long offset, long length) {
        if (length != template.length) throw new IllegalArgumentException();
        this.bytes = bytes;
        this.offset = offset;
        if (bytes.readLong(offset) == UNINITIALIZED)
            bytes.write(offset, template);
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
    public long maxSize() {
        return template.length;
    }

    @Override
    public void setOrderedValue(long value) {
        setValue(value);
    }

    @NotNull
    public String toString() {
        return "value: " + getValue();
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
}
