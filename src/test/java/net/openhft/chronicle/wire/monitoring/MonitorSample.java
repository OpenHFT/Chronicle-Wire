package net.openhft.chronicle.wire.monitoring;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.time.SystemTimeProvider;
import net.openhft.chronicle.wire.JSONWire;
import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.MicroTimestampLongConverter;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;

public class MonitorSample extends SelfDescribingMarshallable {
    // id for the stream
    String id;
    // timestamtp in microseconds
    @LongConversion(MicroTimestampLongConverter.class)
    long ts;
    // latencies in microseconds.
    long p50, p90, p99;

    public static void main(String[] args) {
        MonitorSample ms = new MonitorSample();
        ms.id = "order-sent";
        ms.ts = SystemTimeProvider.INSTANCE.currentTimeMicros();
        ms.p50 = 35;
        ms.p90 = 67;
        ms.p99 = 131;

        JSONWire w = new JSONWire(Bytes.elasticByteBuffer());
        w.getValueOut().object(ms);
        // prints
        // "id":"order-sent","ts":"2019-11-26T12:29:47.178","p50":35,"p90":67,"p99":131
        System.out.println(w);
    }
}
