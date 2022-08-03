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

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import net.openhft.chronicle.core.time.LongTime;
import net.openhft.chronicle.core.time.TimeProvider;

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
        return currentTime(CLOCK);
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

    @Override
    public void append(Bytes<?> bytes, long value) {
        underlying.append(bytes, value);
    }

    interface longFunction {
        long apply(long value);
    }
}
