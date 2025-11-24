/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.benchmarks.bytes;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.wire.benchmarks.Data;
import net.openhft.chronicle.wire.benchmarks.Side;

/**
 * {@link Byteable} view of the {@link Data} structure laid out directly in a {@link BytesStore}.
 * <p>
 * Used to benchmark native memory access patterns versus object based DTOs.
 */
public class NativeData implements Byteable {
    static final int PRICE = 0;
    static final int LONG_INT = PRICE + 8;
    static final int SMALL_INT = LONG_INT + 8;
    static final int SIDE = SMALL_INT + 4;
    static final int FLAG = SIDE + 1;
    static final int TEXT = FLAG + 1;
    private static final int MAX_TEXT = 16;

    private BytesStore<?, ?> bytesStore;
    private long offset;
    private long length;

    /**
     * Read the 32-bit field stored at {@link #SMALL_INT}.
     */
    public int getSmallInt() {
        return bytesStore.readInt(offset + SMALL_INT);
    }

    /**
     * Write the 32-bit {@code smallInt} field into the backing store.
     */
    public void setSmallInt(int smallInt) {
        bytesStore.writeInt(offset + SMALL_INT, smallInt);
    }

    /**
     * Read the 64-bit field stored at {@link #LONG_INT}.
     */
    public long getLongInt() {
        return bytesStore.readLong(offset + LONG_INT);
    }

    /**
     * Write the 64-bit {@code longInt} field into the backing store.
     */
    public void setLongInt(long longInt) {
        bytesStore.writeLong(offset + LONG_INT, longInt);
    }

    /**
     * Read the price value from the backing store.
     */
    public double getPrice() {
        return bytesStore.readDouble(offset + PRICE);
    }

    /**
     * Write the price value into the backing store.
     */
    public void setPrice(double price) {
        bytesStore.writeDouble(offset + PRICE, price);
    }

    /**
     * Read the boolean flag from the backing store.
     */
    public boolean isFlag() {
        return bytesStore.readBoolean(offset + FLAG);
    }

    /**
     * Write the boolean flag into the backing store.
     */
    public void setFlag(boolean flag) {
        bytesStore.writeBoolean(offset + FLAG, flag);
    }

    /**
     * Decode the side flag; true represents {@link Side#Buy}.
     */
    public Side getSide() {
        return bytesStore.readBoolean(offset + SIDE) ? Side.Buy : Side.Sell;
    }

    /**
     * Encode the side flag; {@link Side#Buy} is stored as true.
     */
    public void setSide(Side side) {
        bytesStore.writeBoolean(offset + SIDE, side == Side.Buy);
    }

    /**
     * Point this view at a new {@link BytesStore} region.
     *
     * @param bytesStore backing store containing the encoded fields
     * @param offset     starting offset of the record
     * @param length     total length of the region
     */
    @Override
    public void bytesStore(BytesStore bytesStore, long offset, long length) {
        this.bytesStore = bytesStore;
        this.offset = offset;
        this.length = length;
    }

    /**
     * Total bytes currently used by this record, including the variable-length text.
     */
    public int encodedLength() {
        return TEXT + 1 + bytesStore.readUnsignedByte(offset + TEXT);
    }

    /**
     * Maximum encoded size supported by this view, assuming the text field uses {@link #MAX_TEXT}
     * bytes.
     */
    @Override
    public long maxSize() {
        return TEXT + 1 + MAX_TEXT;
    }

    /**
     * Copy the native layout into the object-based {@link Data} DTO.
     * <p>
     * Not implemented in this benchmark fixture; provided for symmetry with
     * {@link Data#copyTo(NativeData)}.
     */
    public void copyTo(Data data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BytesStore<?, ?> bytesStore() {
        return bytesStore;
    }

    @Override
    public long offset() {
        return offset;
    }
}
