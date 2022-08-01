package net.openhft.chronicle.wire.channel.internal;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.Base128LongConverter;
import net.openhft.chronicle.wire.BytesInBinaryMarshallable;
import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.NanoTimestampLongConverter;

public class ChronicleEvent extends BytesInBinaryMarshallable {
    static final int START_BYTES = BytesUtil.triviallyCopyableStart(ChronicleEvent.class);
    static final int LENGTH_BYTES = BytesUtil.triviallyCopyableLength(ChronicleEvent.class);
    static int count = 0;
    private final Bytes text3 = Bytes.forFieldGroup(this, "text3");
    private final Bytes text4 = Bytes.forFieldGroup(this, "text4");
    private long sendingTimeNS;
    private long transactTimeNS;
    @LongConversion(NanoTimestampLongConverter.class)
    private long dateTime1, dateTime2, dateTime3, dateTime4;
    @LongConversion(Base128LongConverter.class)
    private long text1, text2; // up to 9 ASCII chars
    @FieldGroup("text3")
    private long text3a, text3b, text3c;
    @FieldGroup("text4")
    private long text4a, text4b, text4c, text4d, text4e;
    private int number1, number2;
    private long number3, number4;
    private double value1, value2, value3, value4, value5, value6, value7, value8;

    @Override
    public final void readMarshallable(BytesIn bytes) throws IORuntimeException {
        bytes.unsafeReadObject(this, START_BYTES, LENGTH_BYTES);
    }

    @Override
    public final void writeMarshallable(BytesOut bytes) {
        bytes.unsafeWriteObject(this, START_BYTES, LENGTH_BYTES);
        // hack to slow down the producer during warmup.
        if (bytes.writePosition() > (3 << 20)) {
            System.out.print('.');
            Jvm.pause(++count);
        }
    }

    public void transactTimeNS(long transactTimeNS) {
        this.transactTimeNS = transactTimeNS;
    }

    public long transactTimeNS() {
        return transactTimeNS;
    }

    public void sendingTimeNS(long sendingTimeNS) {
        this.sendingTimeNS = sendingTimeNS;
    }

    public long sendingTimeNS() {
        return sendingTimeNS;
    }
}
