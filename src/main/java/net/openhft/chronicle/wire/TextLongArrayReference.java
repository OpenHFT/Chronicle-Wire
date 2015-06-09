/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.OS;
import org.jetbrains.annotations.NotNull;

/*
The format for a long array in text is
{ capacity: 12345678901234567890, values: [ 12345678901234567890, ... ] }
 */

public class TextLongArrayReference implements ByteableLongArrayValues {
    static final byte[] SECTION1 = "{ capacity: ".getBytes();
    static final byte[] SECTION2 = ", values: [ ".getBytes();
    static final byte[] SECTION3 = " ] }\n".getBytes();
    static final byte[] ZERO = "00000000000000000000".getBytes();
    static final byte[] SEP = ", ".getBytes();

    private static final int DIGITS = ZERO.length;
    private static final int CAPACITY = SECTION1.length;
    private static final int VALUES = CAPACITY + DIGITS + SECTION2.length;
    private static final int VALUE_SIZE = DIGITS + SEP.length;

    private BytesStore bytes;
    private long offset;
    private long length = VALUES;

    public static void write(@NotNull Bytes bytes, long capacity) {
        bytes.write(SECTION1);
        bytes.append(capacity, 20);
        bytes.write(SECTION2);
        for (long i = 0; i < capacity; i++) {
            if (i > 0)
                bytes.append(", ");
            bytes.write(ZERO);
        }
        bytes.write(SECTION3);
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
    public void bytesStore(@NotNull BytesStore bytes, long offset, long length) {
        if (length != peakLength(bytes, offset))
            throw new IllegalArgumentException(length + " != " + peakLength(bytes, offset));
        this.bytes = bytes;
        this.offset = offset;
        this.length = length;
    }

    public static long peakLength(@NotNull BytesStore bytes, long offset) {
        //todo check this, I think there could be a bug here
        return (bytes.parseLong(offset + CAPACITY) * VALUE_SIZE) + VALUES + SECTION3.length - SEP.length;
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
                    "bytes=" + bytes +
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
