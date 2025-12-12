/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.benchmarks;

import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.wire.Base85LongConverter;
import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.Marshallable;

/**
 * Trivially copyable DTO used in Chronicle Wire benchmarks.
 * <p>
 * Uses {@link BytesUtil#triviallyCopyableRange(Class)} and base85 text encoding to support very
 * fast bulk {@link net.openhft.chronicle.bytes.Bytes} operations.
 */
public class TCData implements Marshallable, BytesMarshallable {
    int smallInt = 0;
    long longInt = 0;
    double price = 0;
    boolean flag = false;
    char side;
    @LongConversion(Base85LongConverter.class)
    long text;

    public TCData(int smallInt, long longInt, double price, boolean flag, CharSequence text, Side side) {
        this.smallInt = smallInt;
        this.longInt = longInt;
        this.price = price;
        this.flag = flag;
        setSide(side);
        setText(text);
    }

    public TCData() {

    }

    @Override
    public void readMarshallable(BytesIn<?> bytes) throws IllegalStateException {
        bytes.unsafeReadObject(this, DTO_START, DTO_LENGTH);
    }

    @Override
    public final void writeMarshallable(BytesOut<?> bytes) {
        bytes.unsafeWriteObject(this, DTO_START, DTO_LENGTH);
    }

    /**
     * Starting byte offset of the trivially copyable range for the provided class.
     */
    public static int start(Class<?> c) {
        return BytesUtil.triviallyCopyableRange(c)[0];
    }

    /**
     * Length in bytes of the trivially copyable range for the provided class.
     */
    public static int length(Class<?> c) {
        int[] BYTE_RANGE = BytesUtil.triviallyCopyableRange(c);
        return BYTE_RANGE[1] - BYTE_RANGE[0];
    }

    private static final int DTO_START = start(TCData.class);
    private static final int DTO_LENGTH = length(TCData.class);

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

    public CharSequence getText() {
        return Base85LongConverter.INSTANCE.asText(text);
    }

    public void setText(CharSequence text) {
        this.text = Base85LongConverter.INSTANCE.parse(text);
    }

    /**
     * Side derived from the compact char representation; 'B' maps to {@link Side#Buy}.
     */
       public Side getSide() {
        return side == 'B' ? Side.Buy : Side.Sell;
    }

    /**
     * Store the side as a single character code to keep the struct trivially copyable.
     */
    public void setSide(Side side) {
        this.side = side.name().charAt(0);
    }
}
