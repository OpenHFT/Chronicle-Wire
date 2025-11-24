/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.benchmarks;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;

/**
 * Variant of {@link Data} used to compare different serialisation strategies in microbenchmarks.
 * <p>
 * Uses a different field ordering and access pattern to highlight layout effects.
 */
public class Data2 implements Marshallable, BytesMarshallable {
    int smallInt = 0;
    long longInt = 0;
    double price = 0;
    boolean flag = false;
    transient Bytes<?> text = Bytes.allocateElasticOnHeap(16).unchecked(true);
    Side side;

    /**
     * Construct a populated DTO using the provided values and append the given text into the
     * reusable {@link Bytes} buffer.
     */
    public Data2(int smallInt, long longInt, double price, boolean flag, CharSequence text, Side side) {
        this.smallInt = smallInt;
        this.longInt = longInt;
        this.price = price;
        this.flag = flag;
        this.side = side;
        this.text.appendUtf8(text);
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

    /**
     * Replace the current text content with the supplied string.
     */
    public void setText(String text) {
        this.text.clear();
        this.text.appendUtf8(text);
    }

    /**
     * Direct access to the mutable {@link Bytes} backing the text field for zero-copy use.
     */
    public Bytes<?> textAsBytes() {
        return text;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    /**
     * Read the DTO using the manual Bytes-based encoding used in the microbenchmarks.
     * The side is encoded as a boolean and the text as an 8-bit string for compactness.
     */
    @Override
    public void readMarshallable(BytesIn<?> bytes) {
        price = bytes.readDouble();
        longInt = bytes.readLong();
        smallInt = bytes.readInt();
        flag = bytes.readBoolean();
//        side = bytes.readEnum(Side.class);
        side = bytes.readBoolean() ? Side.Buy : Side.Sell;
        bytes.read8bit(text);
    }

    /**
     * Write the DTO using the manual Bytes-based encoding used in the microbenchmarks.
     */
    @Override
    public void writeMarshallable(BytesOut<?> bytes) {
        bytes.writeDouble(price)
                .writeLong(longInt)
                .writeInt(smallInt)
                .writeBoolean(flag)
//             .writeEnum(side)
                .writeBoolean(side == Side.Buy)
                .write8bit(text);
    }
}
