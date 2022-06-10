package net.openhft.chronicle.wire.utils;

import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.NanoTimestampLongConverter;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;

public class TestEvent extends SelfDescribingMarshallable {
    @LongConversion(NanoTimestampLongConverter.class)
    long eventTime;

    @LongConversion(NanoTimestampLongConverter.class)
    long processedTime;
    @LongConversion(NanoTimestampLongConverter.class)
    long currentTime;
}
