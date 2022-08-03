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

public class TriviallyCopyableMarketData extends BytesInBinaryMarshallable {
    private static final int[] START_END = BytesUtil.triviallyCopyableRange(TriviallyCopyableMarketData.class);
    private static final int START = START_END[0];
    private static final int LENGTH = START_END[1] - START_END[0];

    @LongConversion(Base85LongConverter.class)
    long securityId;

    @LongConversion(MicroTimestampLongConverter.class)
    long time;

    double bid0, bid1, bid2, bid3;
    double bidQty0, bidQty1, bidQty2, bidQty3;

    double ask0, ask1, ask2, ask3;
    double askQty0, askQty1, askQty2, askQty3;

    @Override
    public void readMarshallable(BytesIn<?> bytes) {
        bytes.unsafeReadObject(this, START, LENGTH);
    }

    @Override
    public void writeMarshallable(BytesOut<?> bytes) {
        bytes.unsafeWriteObject(this, START, LENGTH);
    }

    public void securityId(long securityId) {
        this.securityId = securityId;
    }

    @Override
    public BinaryLengthLength binaryLengthLength() {
        return BinaryLengthLength.LENGTH_8BIT;
    }
}
