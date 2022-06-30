package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.wire.Base64LongConverter;
import net.openhft.chronicle.wire.BytesInBinaryMarshallable;
import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.NanoTimestampLongConverter;

public class ChronicleEvent extends BytesInBinaryMarshallable implements Event {
    @LongConversion(NanoTimestampLongConverter.class)
    private long sendingTimeNS;
    @LongConversion(NanoTimestampLongConverter.class)
    private long transactTimeNS;

    @LongConversion(Base64LongConverter.class)
    private long text1;
    private String text3;


    @Override
    public void sendingTimeNS(long sendingTimeNS) {
        this.sendingTimeNS = sendingTimeNS;
    }

    @Override
    public long sendingTimeNS() {
        return sendingTimeNS;
    }

    @Override
    public void transactTimeNS(long transactTimeNS) {
        this.transactTimeNS = transactTimeNS;
    }

    @Override
    public long transactTimeNS() {
        return transactTimeNS;
    }
}
