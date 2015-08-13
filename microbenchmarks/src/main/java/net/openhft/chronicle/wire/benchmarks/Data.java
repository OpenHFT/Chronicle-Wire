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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import net.minidev.json.JSONObject;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import net.openhft.chronicle.wire.benchmarks.bytes.NativeData;
import net.openhft.chronicle.wire.util.BooleanConsumer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * Created by peter on 12/08/15.
 */
public class Data implements Marshallable, BytesMarshallable {
    int smallInt = 0;
    long longInt = 0;
    double price = 0;
    boolean flag = false;
    Bytes text = Bytes.allocateDirect(16);
    Side side;
    private transient IntConsumer setSmallInt = x -> smallInt = x;
    private transient LongConsumer setLongInt = x -> longInt = x;
    private transient DoubleConsumer setPrice = x -> price = x;
    private transient BooleanConsumer setFlag = x -> flag = x;
    private transient Consumer<Side> setSide = x -> side = x;

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
        this.text.clear();
        this.text.append(text);
    }

    public Bytes getTextAsBytes() {
        return text;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    public void writeTo(JSONObject obj) {
        obj.put("price", price);
        obj.put("flag", flag);
        obj.put("text", text);
        obj.put("side", side);
        obj.put("smallInt", smallInt);
        obj.put("longInt", longInt);
    }

    public void readFrom(JSONObject obj) {
        price = obj.getAsNumber("price").doubleValue();
        flag = Boolean.parseBoolean(obj.getAsString("flag"));
        setText(obj.getAsString("text"));
        side = Side.valueOf(obj.getAsString("side"));
        smallInt = obj.getAsNumber("smallInt").intValue();
        longInt = obj.getAsNumber("longInt").longValue();
    }

    public void readFrom(JsonParser parser) throws IOException {
        parser.nextToken();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldname = parser.getCurrentName();
            parser.nextToken();
            switch (fieldname) {
                case "price":
                    setPrice(parser.getDoubleValue());
                    break;
                case "flag":
                    flag = parser.getBooleanValue();
                    break;
                case "text":
                    setText(parser.getValueAsString());
                    break;
                case "side":
                    side = Side.valueOf(parser.getValueAsString());
                    break;
                case "smallInt":
                    smallInt = parser.getIntValue();
                    break;
                case "longInt":
                    longInt = parser.getLongValue();
                    break;
            }
        }
    }

    public void writeTo(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        generator.writeNumberField("price", price);
        generator.writeBooleanField("flag", flag);
        generator.writeStringField("text", text.toString());
        generator.writeStringField("side", side.name());
        generator.writeNumberField("smallInt", smallInt);
        generator.writeNumberField("longInt", longInt);
        generator.close();
    }

    public void copyTextTo(ByteBuffer textBuffer) {
        for (int i = 0; i < text.length(); i++)
            textBuffer.put((byte) text.charAt(i));
    }

    public void copyTo(NativeData nd) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readMarshallable(Bytes<?> bytes) {
        price = bytes.readStopBitDouble();
        longInt = bytes.readStopBit();
        smallInt = (int) bytes.readStopBit();
        flag = bytes.readBoolean();
//        side = bytes.readEnum(Side.class);
        side = bytes.readBoolean() ? Side.Buy : Side.Sell;
        bytes.read8bit(text);
    }

    @Override
    public void writeMarshallable(Bytes bytes) {
        bytes.writeStopBit(price)
                .writeStopBit(longInt)
                .writeStopBit(smallInt)
//             .writeEnum(side)
                .writeBoolean(flag)
                .writeBoolean(side == Side.Buy)
                .write8bit(text);
    }
}
