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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import org.jetbrains.annotations.NotNull;

public class BinaryLongArrayReference implements ByteableLongArrayValues {
    private static final long CAPACITY = 0;
    private static final long VALUES = 8;
    private BytesStore bytes;
    private long offset;
    private long length = VALUES;

    public static void write(@NotNull Bytes bytes, long capacity) {
        bytes.writeLong(capacity);
        long start = bytes.position() + VALUES;
        bytes.zeroOut(start, start + (capacity << 3));
        bytes.skip(capacity << 3);
    }

    public static void lazyWrite(@NotNull Bytes bytes, long capacity) {
        //System.out.println("capacity location =" + bytes.position());
        bytes.writeLong(capacity);
        bytes.skip(capacity << 3);
    }

    public static long peakLength(@NotNull BytesStore bytes, long offset) {
        final long capacity = bytes.readLong(offset);
        assert capacity > 0 : "capacity too small";
        return (capacity << 3) + 8;
    }

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
    public void bytesStore(@NotNull BytesStore bytes, long offset, long length) {
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
        return "value: " + getValueAt(0) + " ...";
    }

    @Override
    public long sizeInBytes(long capacity) {
        return (capacity << 3) + VALUES;
    }
}
