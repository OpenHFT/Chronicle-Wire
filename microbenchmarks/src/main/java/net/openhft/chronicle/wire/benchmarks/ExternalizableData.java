/*
 *     Copyright (C) 2015-2020 chronicle.software
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
import net.openhft.chronicle.wire.benchmarks.bytes.NativeData;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;

public class ExternalizableData implements Externalizable {
    int smallInt = 0;
    long longInt = 0;
    double price = 0;
    boolean flag = false;
    String text;
    Side side;

    public ExternalizableData(int smallInt, long longInt, double price, boolean flag, String text, Side side) {
        this.smallInt = smallInt;
        this.longInt = longInt;
        this.price = price;
        this.flag = flag;
        this.side = side;
        this.text = text;
    }

    public ExternalizableData() {

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
        return text;
    }

    public void setText(String text) {
        this.text = text;
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
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeDouble(price);
        out.writeLong(longInt);
        out.writeInt(smallInt);
        out.writeBoolean(flag);
        out.writeObject(side);
        out.writeObject(getText());
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        setPrice(in.readDouble());
        setLongInt(in.readLong());
        setSmallInt(in.readInt());
        setFlag(in.readBoolean());
        setSide((Side) in.readObject());
        setText((String) in.readObject());
    }
}
