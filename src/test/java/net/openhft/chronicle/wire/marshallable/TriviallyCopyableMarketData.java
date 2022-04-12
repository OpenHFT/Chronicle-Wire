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
