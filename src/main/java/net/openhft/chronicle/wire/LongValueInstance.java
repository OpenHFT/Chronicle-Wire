/*
 * Copyright 2014 Higher Frequency Trading
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
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.NativeStore;
import net.openhft.chronicle.core.values.LongValue;

import java.io.Closeable;
import java.io.IOException;

public class LongValueInstance implements LongValue, Byteable, Closeable {

    private final NativeStore<Void> bytesStore;
    private final Bytes<Void> bytes;
    LongDirectReference directReference;

    public LongValueInstance() {
        directReference = new LongDirectReference();
        long len = 8;
        bytesStore = NativeStore.nativeStore(len);
        bytes = bytesStore.bytes();
        directReference.bytes(bytes, 0, len);
    }

    @Override
    public void bytes(Bytes bytes, long offset, long length) {

        // todo : change how this is done as we should not be updating the bytes of a
        // todo : LongValueInstance, but this code for the moment is used by the chronicle queue
        // header
        directReference.bytes(bytes, offset, length);
        bytes = bytes;
    }

    @Override
    public Bytes bytes() {
        return bytes;
    }

    @Override
    public long offset() {
        return directReference.offset();
    }

    @Override
    public long maxSize() {
        return directReference.maxSize();
    }

    @Override
    public long getValue() {
        return directReference.getValue();
    }

    @Override
    public void setValue(long value) {
        directReference.setValue(value);
    }

    @Override
    public long getVolatileValue() {
        return directReference.getVolatileValue();
    }

    @Override
    public void setOrderedValue(long value) {
        directReference.setOrderedValue(value);
    }

    @Override
    public long addValue(long delta) {
        return directReference.addValue(delta);
    }

    @Override
    public long addAtomicValue(long delta) {
        return directReference.addAtomicValue(delta);
    }

    @Override
    public boolean compareAndSwapValue(long expected, long value) {
        return directReference.compareAndSwapValue(expected, value);
    }

    @Override
    public void close() throws IOException {
        try {
            bytesStore.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
