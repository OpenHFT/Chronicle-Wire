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
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;

/*
The format for a long array in text is
{ capacity: 12345678901234567890, values: [ 12345678901234567890, ... ] }
 */

public class TextLongArrayReference implements ByteableLongArrayValues {
    private static final byte[] SECTION1 = "{ capacity: ".getBytes();
    private static final byte[] SECTION2 = ", values: [ ".getBytes();
    private static final byte[] SECTION3 = " ] }\n".getBytes();
    private static final byte[] ZERO = "00000000000000000000".getBytes();
    private static final byte[] SEP = ", ".getBytes();

    private static final int DIGITS = ZERO.length;
    private static final int CAPACITY = SECTION1.length;
    private static final int VALUES = CAPACITY + DIGITS + SECTION2.length;
    private static final int VALUE_SIZE = DIGITS + SEP.length;

    private BytesStore bytes;
    private long offset;
    private long length = VALUES;

    public static void write(@NotNull Bytes bytes, long capacity) {
        bytes.write(SECTION1);
        bytes.append(bytes.writePosition(), capacity, 20);
        bytes.writeSkip(20);
        bytes.write(SECTION2);
        for (long i = 0; i < capacity; i++) {
            if (i > 0)
                bytes.appendUtf8(", ");
            bytes.write(ZERO);
        }
        bytes.write(SECTION3);
    }

    public static long peakLength(@NotNull BytesStore bytes, long offset) {
        //todo check this, I think there could be a bug here
        return (bytes.parseLong(offset + CAPACITY) * VALUE_SIZE) + VALUES + SECTION3.length - SEP.length;
    }

    @Override
    public long getCapacity() {
        return (length - VALUES) / VALUE_SIZE;
    }

    @Override
    public long getValueAt(long index) {
        return bytes.parseLong(VALUES + offset + index * VALUE_SIZE);
    }

    @Override
    public void setValueAt(long index, long value) {
        bytes.append(VALUES + offset + index * VALUE_SIZE, value, DIGITS);
    }

    @Override
    public void bindValueAt(int index, LongValue value) {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public long getVolatileValueAt(long index) {
        OS.memory().loadFence();
        return getValueAt(index);
    }

    @Override
    public void setOrderedValueAt(long index, long value) {
        setValueAt(index, value);
        OS.memory().storeFence();
    }

    @Override
    public boolean compareAndSet(long index, long expected, long value) {
        throw new UnsupportedOperationException("todo");
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

    @NotNull
    public String toString() {
        if (bytes == null) {
            return "LongArrayTextReference{" +
                    "bytes=null" +
                    ", offset=" + offset +
                    ", length=" + length +
                    '}';
        }

        return "value: " + getValueAt(0) + " ...";
    }

    @Override
    public long sizeInBytes(long capacity) {
        return (capacity * VALUE_SIZE) + VALUES + SECTION3.length - SEP.length;
    }
}
