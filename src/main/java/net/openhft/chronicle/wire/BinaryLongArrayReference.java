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
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * This class acts a Binary array of 64-bit values. c.f. TextLongArrayReference
 */
public class BinaryLongArrayReference implements Byteable, LongArrayValues {
    //    private static final long CAPACITY = 0;
    private static final long VALUES = 8;
    private BytesStore bytes;
    private long offset;
    private long length = VALUES;

    public static void write(@NotNull Bytes bytes, long capacity) throws BufferOverflowException, IllegalArgumentException {
        bytes.writeLong(capacity);
        long start = bytes.writePosition() + VALUES;
        bytes.zeroOut(start, start + (capacity << 3));
        bytes.writeSkip(capacity << 3);
    }

    public static void lazyWrite(@NotNull Bytes bytes, long capacity) throws BufferOverflowException {
        //System.out.println("capacity location =" + bytes.position());
        bytes.writeLong(capacity);
        bytes.writeSkip(capacity << 3);
    }

    public static long peakLength(@NotNull BytesStore bytes, long offset) throws BufferUnderflowException {
        final long capacity = bytes.readLong(offset);
        assert capacity > 0 : "capacity too small";
        return (capacity << 3) + 8;
    }

    @Override
    public long getCapacity() {
        return (length - VALUES) >>> 3;
    }

    @Override
    public long getValueAt(long index) throws BufferUnderflowException {
        return bytes.readLong(VALUES + offset + (index << 3));
    }

    @Override
    public void setValueAt(long index, long value) throws IllegalArgumentException, BufferOverflowException {
        bytes.writeLong(VALUES + offset + (index << 3), value);
    }

    @Override
    public long getVolatileValueAt(long index) throws BufferUnderflowException {
        return bytes.readVolatileLong(VALUES + offset + (index << 3));
    }

    @Override
    public void bindValueAt(int index, @NotNull LongValue value) {
        ((BinaryLongReference) value).bytesStore(bytes, VALUES + offset + (index << 3), 8);
    }

    @Override
    public void setOrderedValueAt(long index, long value) throws IllegalArgumentException, BufferOverflowException {
        bytes.writeOrderedLong(VALUES + offset + (index << 3), value);
    }

    @Override
    public boolean compareAndSet(long index, long expected, long value) throws IllegalArgumentException, BufferOverflowException {
        return bytes.compareAndSwapLong(VALUES + offset + (index << 3), expected, value);
    }

    @Override
    public void bytesStore(@NotNull BytesStore bytes, long offset, long length) throws BufferUnderflowException, IllegalArgumentException {
        if (length != peakLength(bytes, offset))
            throw new IllegalArgumentException(length + " != " + peakLength(bytes, offset));
        this.bytes = bytes;
        this.offset = offset;
        this.length = length;
    }

    @Override
    public long maxSize() {
        return length;
    }

    @NotNull
    public String toString() {
        try {
            return "value: " + getValueAt(0) + " ...";
        } catch (BufferUnderflowException e) {
            return e.toString();
        }
    }
}
