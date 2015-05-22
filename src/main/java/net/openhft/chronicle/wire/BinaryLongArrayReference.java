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

public class BinaryLongArrayReference implements ByteableLongArrayValues {
    private static final long CAPACITY = 0;
    private static final long VALUES = 8;
    private BytesStore bytes;
    private long offset;
    private long length = VALUES;

    public static void write(Bytes bytes, long capacity) {
        bytes.writeLong(capacity);
        long start = bytes.position() + VALUES;
        bytes.zeroOut(start, start + (capacity << 3));
        bytes.skip(capacity << 3);
    }

    public static void lazyWrite(Bytes bytes, long capacity) {
        //System.out.println("capacity location =" + bytes.position());
        bytes.writeLong(capacity);
        bytes.skip(capacity << 3);
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
    public void bytesStore(BytesStore bytes, long offset, long length) {
        if (length != peakLength(bytes, offset))
            throw new IllegalArgumentException(length + " != " + peakLength(bytes, offset));
        this.bytes = bytes;
        this.offset = offset;
        this.length = length;
    }

    public static long peakLength(BytesStore bytes, long offset) {
        final long capacity = bytes.readLong(offset );
        assert capacity > 0 : "capacity too small";
        return (capacity << 3) + 8;
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

    public String toString() {
        return "value: " + getValueAt(0) + " ...";
    }

    @Override
    public long sizeInBytes(long capacity) {
        return (capacity << 3) + VALUES;
    }
}
