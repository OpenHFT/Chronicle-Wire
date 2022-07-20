package net.openhft.chronicle.wire.utils;

import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.NanoTimestampLongConverter;

import static net.openhft.chronicle.core.time.SystemTimeProvider.CLOCK;

public class TestImpl implements TestIn {
    private final TestOut out;
    private long time;

    public TestImpl(TestOut out) {
        this.out = out;
    }

    @Override
    public void time(@LongConversion(NanoTimestampLongConverter.class) long time) {
        this.time = time;
    }

    @Override
    public void testEvent(TestEvent dto) {
        dto.processedTime = time;
        dto.currentTime = CLOCK.currentTimeNanos();
        out.testEvent(dto);
    }
}
