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

import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import net.openhft.chronicle.wire.util.BooleanConsumer;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * Created by peter on 12/08/15.
 */
public class Data implements Marshallable {
    int smallInt = 0;
    long longInt = 0;
    double price = 0;
    boolean flag = false;
    StringBuilder text = new StringBuilder();
    Side side;
    private IntConsumer setSmallInt = x -> smallInt = x;
    private LongConsumer setLongInt = x -> longInt = x;
    private DoubleConsumer setPrice = x -> price = x;
    private BooleanConsumer setFlag = x -> flag = x;
    private Consumer<Side> setSide = x -> side = x;

    public Data(int smallInt, long longInt, double price, boolean flag, CharSequence text, Side side) {
        this.smallInt = smallInt;
        this.longInt = longInt;
        this.price = price;
        this.flag = flag;
        this.side = side;
        this.text.append(text);
    }

    public Data() {

    }

    @Override
    public void readMarshallable(WireIn wire) throws IllegalStateException {
        wire.read(DataFields.price).float64(setPrice)
                .read(DataFields.flag).bool(setFlag)
                .read(DataFields.text).text(text)
                .read(DataFields.side).asEnum(Side.class, setSide)
                .read(DataFields.smallInt).int32(setSmallInt)
                .read(DataFields.longInt).int64(setLongInt);
    }

    @Override
    public void writeMarshallable(WireOut wire) {
        wire.write(DataFields.price).float64(price)
                .write(DataFields.flag).bool(flag)
                .write(DataFields.text).text(text)
                .write(DataFields.side).asEnum(side)
                .write(DataFields.smallInt).int32(smallInt)
                .write(DataFields.longInt).int64(longInt);
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
        this.text.setLength(0);
        this.text.append(text);
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }
}
