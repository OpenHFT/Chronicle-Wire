/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.benchmarks;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import net.openhft.chronicle.wire.benchmarks.bytes.NativeData;

import java.nio.ByteBuffer;

public class Data implements Marshallable, BytesMarshallable {
    int smallInt = 0;
    long longInt = 0;
    double price = 0;
    boolean flag = false;
    transient Bytes<?> text = Bytes.allocateElasticOnHeap(16).unchecked(true);
    Side side;

    public Data(int smallInt, long longInt, double price, boolean flag, CharSequence text, Side side) {
        this.smallInt = smallInt;
        this.longInt = longInt;
        this.price = price;
        this.flag = flag;
        this.side = side;
        this.text.appendUtf8(text);
    }

    public Data() {

    }

    @Override
    public void readMarshallable(WireIn wire) throws IllegalStateException {
        wire.read(DataFields.price).float64(this, (o, x) -> o.price = x)
                .read(DataFields.flag).bool(this, (o, x) -> o.flag = x)
                .read(DataFields.text).text(text)
                .read(DataFields.side).asEnum(Side.class, this, (o, x) -> o.side = x)
                .read(DataFields.smallInt).int32(this, (o, x) -> o.smallInt = x)
                .read(DataFields.longInt).int64(this, (o, x) -> o.longInt = x);
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
        this.text.appendUtf8(text);
    }

    public Bytes<?> textAsBytes() {
        return text;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    public void copyTextTo(ByteBuffer textBuffer) {
        for (int i = 0; i < text.length(); i++)
            textBuffer.put((byte) text.charAt(i));
    }

    public void copyTo(NativeData nd) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readMarshallable(BytesIn<?> bytes) {
        price = bytes.readStopBitDouble();
        longInt = bytes.readStopBit();
        smallInt = (int) bytes.readStopBit();
        flag = bytes.readBoolean();
//        side = bytes.readEnum(Side.class);
        side = bytes.readBoolean() ? Side.Buy : Side.Sell;
        bytes.read8bit(text);
    }

    @Override
    public void writeMarshallable(BytesOut<?> bytes) {
        bytes.writeStopBit(price)
                .writeStopBit(longInt)
                .writeStopBit(smallInt)
//             .writeEnum(side)
                .writeBoolean(flag)
                .writeBoolean(side == Side.Buy)
                .write8bit(text);
    }
}
