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

package net.openhft.chronicle.wire.benchmarks.bytes;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.wire.benchmarks.Data;
import net.openhft.chronicle.wire.benchmarks.Side;


public class NativeData implements Byteable {
    static final int PRICE = 0;
    static final int LONG_INT = PRICE + 8;
    static final int SMALL_INT = LONG_INT + 8;
    static final int SIDE = SMALL_INT + 4;
    static final int FLAG = SIDE + 1;
    static final int TEXT = FLAG + 1;
    private static final int MAX_TEXT = 16;

    private BytesStore bytesStore;
    private long offset;
    private long length;

    public int getSmallInt() {
        return bytesStore.readInt(offset + SMALL_INT);
    }

    public void setSmallInt(int smallInt) {
        bytesStore.writeInt(offset + SMALL_INT, smallInt);
    }

    public long getLongInt() {
        return bytesStore.readLong(offset + LONG_INT);
    }

    public void setLongInt(long longInt) {
        bytesStore.writeLong(offset + LONG_INT, longInt);
    }

    public double getPrice() {
        return bytesStore.readDouble(offset + PRICE);
    }

    public void setPrice(double price) {
        bytesStore.writeDouble(offset + PRICE, price);
    }

    public boolean isFlag() {
        return bytesStore.readBoolean(offset + FLAG);
    }

    public void setFlag(boolean flag) {
        bytesStore.writeBoolean(offset + FLAG, flag);
    }

    public Side getSide() {
        return bytesStore.readBoolean(offset + SIDE) ? Side.Buy : Side.Sell;
    }

    public void setSide(Side side) {
        bytesStore.writeBoolean(offset + SIDE, side == Side.Buy);
    }

    @Override
    public void bytesStore(BytesStore bytesStore, long offset, long length) {
        this.bytesStore = bytesStore;
        this.offset = offset;
        this.length = length;
    }

    public int encodedLength() {
        return TEXT + 1 + bytesStore.readUnsignedByte(offset + TEXT);
    }

    @Override
    public long maxSize() {
        return TEXT + 1 + MAX_TEXT;
    }

    public void copyTo(Data data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BytesStore bytesStore() {
        return bytesStore;
    }

    @Override
    public long offset() {
        return offset;
    }
}
