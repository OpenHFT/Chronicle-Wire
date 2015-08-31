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

package net.openhft.chronicle.wire.benchmarks;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;

/**
 * Created by peter on 12/08/15.
 */
public class Data2 implements Marshallable, BytesMarshallable {
    int smallInt = 0;
    long longInt = 0;
    double price = 0;
    boolean flag = false;
    transient Bytes text = Bytes.allocateDirect(16).unchecked(true);
    Side side;

    public Data2(int smallInt, long longInt, double price, boolean flag, CharSequence text, Side side) {
        this.smallInt = smallInt;
        this.longInt = longInt;
        this.price = price;
        this.flag = flag;
        this.side = side;
        this.text.append(text);
    }

    public Data2() {

    }

    @Override
    public void readMarshallable(WireIn wire) throws IllegalStateException {
        price = wire.read(DataFields.price).float64();
        longInt = wire.read(DataFields.longInt).int64();
        smallInt = wire.read(DataFields.smallInt).int32();
        flag = wire.read(DataFields.flag).bool();
        wire.read(DataFields.text).text(text);
        side = wire.read(DataFields.side).asEnum(Side.class);
    }

    @Override
    public void writeMarshallable(WireOut wire) {
        wire.write(DataFields.price).float64(price)
                .write(DataFields.longInt).int64(longInt)
                .write(DataFields.smallInt).int32(smallInt)
                .write(DataFields.flag).bool(flag)
                .write(DataFields.text).text(text)
                .write(DataFields.side).asEnum(side);
    }

    public int getSmallInt() {
        return smallInt;
    }

    public void setSmallInt(int smallInt) {
        this.smallInt = smallInt;
    }

    public long getLongInt() {
        return longInt;
    }

    public void setLongInt(long longInt) {
        this.longInt = longInt;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    public String getText() {
        return text.toString();
    }

    public void setText(String text) {
        this.text.clear();
        this.text.append(text);
    }

    public Bytes textAsBytes() {
        return text;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    @Override
    public void readMarshallable(Bytes<?> bytes) {
        price = bytes.readDouble();
        longInt = bytes.readLong();
        smallInt = bytes.readInt();
        flag = bytes.readBoolean();
//        side = bytes.readEnum(Side.class);
        side = bytes.readBoolean() ? Side.Buy : Side.Sell;
        bytes.read8bit(text);
    }

    @Override
    public void writeMarshallable(Bytes bytes) {
        bytes.writeDouble(price)
                .writeLong(longInt)
                .writeInt(smallInt)
                .writeBoolean(flag)
//             .writeEnum(side)
                .writeBoolean(side == Side.Buy)
                .write8bit(text);
    }
}
