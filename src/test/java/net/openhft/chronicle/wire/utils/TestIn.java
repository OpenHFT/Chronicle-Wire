package net.openhft.chronicle.wire.utils;

import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.NanoTimestampLongConverter;

public interface TestIn {
    void time(@LongConversion(NanoTimestampLongConverter.class) long time);

    void testEvent(TestEvent dto);
}
