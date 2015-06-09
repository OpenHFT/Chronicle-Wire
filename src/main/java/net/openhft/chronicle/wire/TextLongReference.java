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
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class TextLongReference implements LongValue, Byteable {
    public static final byte[] template = "!!atomic { locked: false, value: 00000000000000000000 }".getBytes();
    public static final int FALSE = BytesUtil.asInt("fals");
    public static final int TRUE = BytesUtil.asInt(" tru");
    static final long UNINITIALIZED = 0x0L;
    static final int LOCKED = 19;
    static final int VALUE = 33;
    private static final int DIGITS = 20;
    private BytesStore bytes;
    private long offset;

    public static void write(@NotNull Bytes bytes, long value) {
        long position = bytes.position();
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
    public void bytesStore(@NotNull BytesStore bytes, long offset, long length) {
        if (length != template.length) throw new IllegalArgumentException();
        this.bytes = bytes;
        this.offset = offset;
        if (bytes.readLong(offset) == UNINITIALIZED)
            bytes.write(offset, template);
    }

    @Override
    public long getValue() {
        return withLock(() -> bytes.parseLong(offset + VALUE));
    }

    @Override
    public BytesStore bytesStore() {
        return bytes;
    }

    @Override
    public void setValue(long value) {
        withLock(() -> bytes.append(offset + VALUE, value, DIGITS));
    }

    @Override
    public long offset() {
        return offset;
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
