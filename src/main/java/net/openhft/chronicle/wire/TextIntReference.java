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

import java.util.function.Supplier;

public class TextIntReference implements IntValue, Byteable {
    public static final byte[] template = "!!atomic { locked: false, value: 0000000000 }".getBytes();
    public static final int FALSE = ('f' << 24) | ('a' << 16) | ('l' << 8) | 's';
    public static final int TRUE = (' ' << 24) | ('t' << 16) | ('r' << 8) | 'u';
    static final int LOCKED = 19;
    static final int VALUE = 33;
    private static final int DIGITS = 10;
    private BytesStore bytes;
    private long offset;

    public static void write(@NotNull Bytes bytes, int value) {
        long position = bytes.writePosition();
        bytes.write(template);
        bytes.append(position + VALUE, value, DIGITS);
    }

    <T> T withLock(@NotNull Supplier<T> call) {
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
        withLock(() -> bytes.append(offset + VALUE, value, DIGITS));
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
                return true;
            }
            return false;
        });
    }

    @Override
    public void bytesStore(BytesStore bytes, long offset, long length) {
        if (length != template.length) throw new IllegalArgumentException();
        this.bytes = bytes;
        this.offset = offset;
    }

    @Override
    public long maxSize() {
        return template.length;
    }

    @NotNull
    public String toString() { return "value: "+getValue(); }
}
