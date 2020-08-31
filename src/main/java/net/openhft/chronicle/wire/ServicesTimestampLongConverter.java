package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.annotation.UsedViaReflection;
import net.openhft.chronicle.core.time.LongTime;
import net.openhft.chronicle.core.time.SystemTimeProvider;
import net.openhft.chronicle.core.time.TimeProvider;
import net.openhft.chronicle.core.time.UniqueMicroTimeProvider;

import java.util.concurrent.TimeUnit;
import java.util.function.ToLongFunction;

import static net.openhft.chronicle.core.time.SystemTimeProvider.CLOCK;

public class ServicesTimestampLongConverter implements LongConverter {
    @UsedViaReflection
    public static final ServicesTimestampLongConverter INSTANCE = new ServicesTimestampLongConverter();

    private static final String SERVICES_TIME_UNIT = System.getProperty("service.time.unit", "ns");
    private static final longFunction toTime;
    private static final ToLongFunction<TimeProvider> currentTime;
    private static final LongConverter underlying;
    private static final TimeUnit timeUnit;

    static {
        switch (SERVICES_TIME_UNIT) {
            case "ms":
                toTime = LongTime::toMillis;
                currentTime = TimeProvider::currentTimeMillis;
                underlying = MilliTimestampLongConverter.INSTANCE;
                timeUnit = TimeUnit.MILLISECONDS;
                break;
            case "us":
                toTime = LongTime::toMicros;
                currentTime = TimeProvider::currentTimeMicros;
                underlying = MicroTimestampLongConverter.INSTANCE;
                timeUnit = TimeUnit.MICROSECONDS;
                break;
            case "ns":
            default:
                toTime = LongTime::toNanos;
                currentTime = TimeProvider::currentTimeNanos;
                underlying = NanoTimestampLongConverter.INSTANCE;
                timeUnit = TimeUnit.NANOSECONDS;
                break;
        }
    }

    public static long toTime(long arg) {
        return toTime.apply(arg);
    }

    public static long currentTime() {
        TimeProvider clock = CLOCK;
        // if it is default use unique time provider instead.
        if (clock == SystemTimeProvider.INSTANCE)
            clock = UniqueMicroTimeProvider.INSTANCE;
        return currentTime(clock);
    }

    public static long currentTime(TimeProvider clock) {
        return currentTime.applyAsLong(clock);
    }

    public static TimeUnit timeUnit() {
        return timeUnit;
    }

    @Override
    public long parse(CharSequence text) {
        return underlying.parse(text);
    }

    @Override
    public void append(StringBuilder text, long value) {
        underlying.append(text, value);
    }

    interface longFunction {
        long apply(long value);
    }
}
