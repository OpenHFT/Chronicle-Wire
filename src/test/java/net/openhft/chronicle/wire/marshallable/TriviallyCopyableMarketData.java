/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.bytes.util.BinaryLengthLength;
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
    @LongConversion(Base85LongConverter.class)
    long securityId;

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

    /**
     * Setter for the security ID.
     *
     * @param securityId Unique identifier for the security.
     */
    public void securityId(long securityId) {
        this.securityId = securityId;
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
