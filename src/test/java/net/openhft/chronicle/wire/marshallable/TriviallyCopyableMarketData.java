/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.bytes.util.BinaryLengthLength;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import net.openhft.chronicle.wire.Base85LongConverter;
import net.openhft.chronicle.wire.BytesInBinaryMarshallable;
import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.MicroTimestampLongConverter;

/**
 * Represents market data that can be trivially copied into bytes for efficient serialization and deserialization.
 */
public class TriviallyCopyableMarketData extends BytesInBinaryMarshallable {
    // Constants to determine the start and end bytes for a trivially copyable object
    private static final int[] START_END = BytesUtil.triviallyCopyableRange(TriviallyCopyableMarketData.class);
    private static final int START = START_END[0];
    private static final int LENGTH = START_END[1] - START_END[0];

    // Unique identifier for the security, encoded in Base85 format for compactness
    @UsedViaReflection
    @LongConversion(Base85LongConverter.class)
    private long securityId;

    // Timestamp of the market data, encoded to represent microsecond precision
    @LongConversion(MicroTimestampLongConverter.class)
    long time;

    // Bid prices for up to 4 levels
    double bid0, bid1, bid2, bid3;

    // Quantities for each bid price level
    double bidQty0, bidQty1, bidQty2, bidQty3;

    // Ask prices for up to 4 levels
    double ask0, ask1, ask2, ask3;

    // Quantities for each ask price level
    double askQty0, askQty1, askQty2, askQty3;

    /**
     * Deserializes this object from a provided BytesIn source.
     *
     * @param bytes Source of bytes to read from.
     */
    @Override
    public void readMarshallable(BytesIn<?> bytes) {
        // Directly copy bytes into the object's fields for efficient deserialization
        bytes.unsafeReadObject(this, START, LENGTH);
    }

    /**
     * Serializes this object to a provided BytesOut destination.
     *
     * @param bytes Destination to write bytes to.
     */
    @Override
    public void writeMarshallable(BytesOut<?> bytes) {
        // Directly copy the object's fields into bytes for efficient serialization
        bytes.unsafeWriteObject(this, START, LENGTH);
    }

    public void securityId(long securityId) {
        this.securityId = securityId;
    }

    long fieldChecksum() {
        long result = securityId;
        result = result * 31 + time;
        result = result * 31 + Double.doubleToLongBits(bid0);
        result = result * 31 + Double.doubleToLongBits(bid1);
        result = result * 31 + Double.doubleToLongBits(bid2);
        result = result * 31 + Double.doubleToLongBits(bid3);
        result = result * 31 + Double.doubleToLongBits(bidQty0);
        result = result * 31 + Double.doubleToLongBits(bidQty1);
        result = result * 31 + Double.doubleToLongBits(bidQty2);
        result = result * 31 + Double.doubleToLongBits(bidQty3);
        result = result * 31 + Double.doubleToLongBits(ask0);
        result = result * 31 + Double.doubleToLongBits(ask1);
        result = result * 31 + Double.doubleToLongBits(ask2);
        result = result * 31 + Double.doubleToLongBits(ask3);
        result = result * 31 + Double.doubleToLongBits(askQty0);
        result = result * 31 + Double.doubleToLongBits(askQty1);
        result = result * 31 + Double.doubleToLongBits(askQty2);
        result = result * 31 + Double.doubleToLongBits(askQty3);
        return result;
    }

    /**
     * Returns the binary length used for serialization/deserialization.
     *
     * @return Binary length representation.
     */
    @Override
    public BinaryLengthLength binaryLengthLength() {
        return BinaryLengthLength.LENGTH_8BIT;
    }
}
