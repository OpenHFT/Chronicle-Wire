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
import net.openhft.chronicle.core.values.IntValue;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntSupplier;

/**
 * Implementation of a reference to a 32-bit in in text wire format.
 */
class TextIntReference implements IntValue, Byteable {
    private static final byte[] template = "!!atomic { locked: false, value: 0000000000 }".getBytes();
    private static final int FALSE = 'f' | ('a' << 8) | ('l' << 16) | ('s' << 24);
    private static final int TRUE = ' ' | ('t' << 8) | ('r' << 16) | ('u' << 24);
    private static final int INT_TRUE = 1;
    private static final int INT_FALSE = 0;
    private static final int LOCKED = 19;
    private static final int VALUE = 33;
    private static final int DIGITS = 10;
    private BytesStore bytes;
    private long offset;

    public static void write(@NotNull Bytes bytes, int value) {
        long position = bytes.writePosition();
        bytes.write(template);
        bytes.append(position + VALUE, value, DIGITS);
    }

    private int withLock(@NotNull IntSupplier call) {
        long valueOffset = offset + LOCKED;
        int value = bytes.readVolatileInt(valueOffset);
        if (value != FALSE && value != TRUE)
            throw new IllegalStateException();
        while (true) {
            if (bytes.compareAndSwapInt(valueOffset, FALSE, TRUE)) {
                int t = call.getAsInt();
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
        withLock(() -> {
            bytes.append(offset + VALUE, value, DIGITS);
            return INT_TRUE;
        });
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
            bytes.append(offset + VALUE, value, DIGITS);
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
                bytes.append(offset + VALUE, value, DIGITS);
                return INT_TRUE;
            }
            return INT_FALSE;
        }) == INT_TRUE;
    }

    @Override
    public void bytesStore(BytesStore bytes, long offset, long length) {
        if (length != template.length)
            throw new IllegalArgumentException();
        this.bytes = bytes;
        this.offset = offset;
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
        return template.length;
    }

    @NotNull
    public String toString() {
        return "value: " + getValue();
    }
}
