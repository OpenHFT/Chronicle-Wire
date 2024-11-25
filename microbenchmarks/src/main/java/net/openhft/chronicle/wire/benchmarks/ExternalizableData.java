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
import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ExternalizableData extends SelfDescribingMarshallable implements Externalizable {
    // {"smallInt":123,"longInt":1234567890,"price":1234.0,"flag":true,"text":"Hello World","side":"Sell"}

    static final Bytes<Void> PRICE = Bytes.fromDirect("{\"price\":");
    static final Bytes<Void> FLAG = Bytes.fromDirect(",\"flag\":");
    static final Bytes<Void> TEXT = Bytes.fromDirect("\"text\":\"");
    static final Bytes<Void> SIDE = Bytes.fromDirect("\",\"side\":\"");
    static final Bytes<Void> SMALL_INT = Bytes.fromDirect("\",\"smallInt\":");
    static final Bytes<Void> LONG_INT = Bytes.fromDirect(",\"longInt\":");
    static final Bytes<Void> END = Bytes.fromDirect("}");
    static final Bytes<Void> TRUE = Bytes.fromDirect("true");
    static final Bytes<Void> FALSE = Bytes.fromDirect("false");

    double price = 0;
    boolean flag = false;
    String text;
    Bytes<?> textBytes;
    Side side;
    int smallInt = 0;
    long longInt = 0;

    public ExternalizableData(double price, boolean flag, String text, Side side, int smallInt, long longInt) {
        this.price = price;
        this.flag = flag;
        this.text = text;
        textBytes = Bytes.from(text);
        this.side = side;
        this.smallInt = smallInt;
        this.longInt = longInt;
    }

    public ExternalizableData() {
        textBytes = Bytes.allocateElasticOnHeap(128).unchecked(true);
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

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeDouble(price);
        out.writeBoolean(flag);
        out.writeObject(getText());
        out.writeObject(side);
        out.writeInt(smallInt);
        out.writeLong(longInt);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        setPrice(in.readDouble());
        setFlag(in.readBoolean());
        setText((String) in.readObject());
        Object s = in.readObject();
        if (s instanceof Side)
            setSide((Side) s);
        else if (s instanceof String)
            setSide(Side.valueOf((String) s));
        else
            throw new IllegalStateException();
        setSmallInt(in.readInt());
        setLongInt(in.readLong());
    }

    @Override
    public void writeMarshallable(WireOut wire) throws InvalidMarshallableException {
        wire.write("price").writeDouble(price);
        wire.write("flag").writeBoolean(flag);
        wire.write("text").writeString(text);
        wire.write("side").writeString(side.name());
        wire.write("smallInt").writeInt(smallInt);
        wire.write("longInt").writeLong(longInt);
    }

    @Override
    public void readMarshallable(WireIn wire) throws IORuntimeException, InvalidMarshallableException {
        price = wire.read("price").readDouble();
        flag = wire.read("flag").readBoolean();
        text = wire.read("text").readString();
        side = Side.valueOf(wire.read("side").readString());
        smallInt = wire.read("smallInt").readInt();
        longInt = wire.read("longInt").readLong();
    }

    @Override
    public void writeMarshallable(BytesOut<?> bytes) throws InvalidMarshallableException {
        appendBytes(bytes, PRICE);
        bytes.append(price);
        appendBytes(bytes, FLAG);
        appendBytes(bytes, flag ? TRUE : FALSE);
        appendBytes(bytes, TEXT);
        appendBytes(bytes, textBytes);
        appendBytes(bytes, SIDE);
        appendBytes(bytes, side.bs);
        appendBytes(bytes, SMALL_INT);
        bytes.append(smallInt);
        appendBytes(bytes, LONG_INT);
        bytes.append(longInt);
        appendBytes(bytes, END);
    }

    private static void appendBytes(BytesOut<?> bytes, BytesStore<?, ?> bytesAdded) {
        BytesInternal.writeFully(bytesAdded, 0, bytesAdded.readLimit(), bytes);
    }

    @Override
    public void readMarshallable(BytesIn<?> bytes) throws IORuntimeException, InvalidMarshallableException {
        expectUtf8(bytes, PRICE, 0);
        price = bytes.parseDouble();
        expectUtf8(bytes, FLAG, -1);
        flag = Boolean.TRUE.equals(bytes.parseBoolean());
        expectUtf8(bytes, TEXT, -1);
        bytes.parseUtf8(textBytes, StopCharTesters.QUOTES);
        expectUtf8(bytes, SIDE, -1);
        side = Side.valueOf(bytes.parse8bit(StopCharTesters.QUOTES));
        expectUtf8(bytes, SMALL_INT, -1);
        smallInt = bytes.parseInt();
        expectUtf8(bytes, LONG_INT, -1);
        longInt = bytes.parseLong();
        expectUtf8(bytes, END, 0);
    }

    private void expectUtf8(BytesIn<?> bytesIn, Bytes<Void> text, int readSkip) {
        Bytes<?> bytes = (Bytes<?>) bytesIn;
        bytes.readSkip(readSkip);
        if (!bytes.startsWith(text))
            throw new IORuntimeException("Expected " + text + " but got " + bytes);
        bytes.readSkip(text.length());
    }
}
