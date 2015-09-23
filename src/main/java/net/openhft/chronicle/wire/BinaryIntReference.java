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
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.values.IntValue;
import org.jetbrains.annotations.NotNull;

/**
 * This class acts as a Binary 32-bit in values. c.f. TextIntReference
 */
public class BinaryIntReference implements IntValue, Byteable {
    private BytesStore bytes;
    private long offset;

    @Override
    public void bytesStore(@NotNull BytesStore bytes, long offset, long length) {
        if (length != maxSize()) throw new IllegalArgumentException();
        this.bytes = bytes.bytesStore();
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
        return 4;
    }

    @NotNull
    public String toString() {
        return "value: " + getValue();
    }

    @Override
    public int getValue() {
        return bytes.readInt(offset);
    }

    @Override
    public void setValue(int value) {
        bytes.writeInt(offset, value);
    }

    @Override
    public int getVolatileValue() {
        return bytes.readVolatileInt(offset);
    }

    @Override
    public void setOrderedValue(int value) {
        bytes.writeOrderedInt(offset, value);
    }

    @Override
    public int addValue(int delta) {
        return bytes.addAndGetInt(offset, delta);
    }

    @Override
    public int addAtomicValue(int delta) {
        return addValue(delta);
    }

    @Override
    public boolean compareAndSwapValue(int expected, int value) {
        return bytes.compareAndSwapInt(offset, expected, value);
    }
}
