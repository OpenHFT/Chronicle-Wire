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

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.values.LongValue;

public class LongDirectReference implements LongValue, Byteable {
    private BytesStore bytes;
    private long offset;

    @Override
    public long getValue() {
        return bytes.readLong(offset);
    }

    @Override
    public void setValue(long value) {
        bytes.writeLong(offset, value);
    }

    @Override
    public long getVolatileValue() {
        return bytes.readVolatileLong(offset);
    }

    @Override
    public void setOrderedValue(long value) {
        bytes.writeOrderedLong(offset, value);
    }

    @Override
    public long addValue(long delta) {
        return bytes.addAndGetLong(offset, delta);
    }

    @Override
    public long addAtomicValue(long delta) {
        return addValue(delta);
    }

    @Override
    public boolean compareAndSwapValue(long expected, long value) {
        return bytes.compareAndSwapLong(offset, expected, value);
    }

    @Override
    public void bytesStore(BytesStore bytes, long offset, long length) {
        if (length != maxSize()) throw new IllegalArgumentException();
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
        return 8;
    }

    public String toString() { return "value: "+getValue(); }
}
