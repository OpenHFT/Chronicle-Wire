/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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

import net.openhft.chronicle.core.time.LongTime;

import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.concurrent.TimeUnit;

public class NanoTimestampLongConverter extends AbstractTimestampLongConverter {
    public static final NanoTimestampLongConverter INSTANCE = new NanoTimestampLongConverter();

    public NanoTimestampLongConverter() {
        super(TimeUnit.NANOSECONDS);
    }

    public NanoTimestampLongConverter(String zoneId) {
        super(zoneId, TimeUnit.NANOSECONDS);
    }

    @Override
    protected long parseFormattedDate(TemporalAccessor value) {
        long time = value.getLong(ChronoField.EPOCH_DAY) * 86400_000_000_000L;
        if (value.isSupported(ChronoField.NANO_OF_DAY))
            time += value.getLong(ChronoField.NANO_OF_DAY);
        else if (value.isSupported(ChronoField.MICRO_OF_DAY))
            time += value.getLong(ChronoField.MICRO_OF_DAY) * 1_000;
        else if (value.isSupported(ChronoField.MILLI_OF_DAY))
            time += value.getLong(ChronoField.MILLI_OF_DAY) * 1_000_000L;
        else if (value.isSupported(ChronoField.SECOND_OF_DAY))
            time += value.getLong(ChronoField.SECOND_OF_DAY) * 1_000_000_000L;

        return time;
    }

    @Override
    protected long parseTimestamp(long value, CharSequence text) {
        long number = LongTime.toNanos(value);
        if (LongTime.isMicros(number)) {
            System.out.println("In input data, replace " + text + " with " + asString(number));
        } else {
            if (number != 0)
                System.out.println("In input data, replace " + text + " with a real date.");
        }
        return number;
    }

    @Override
    protected void appendFraction(DateTimeFormatterBuilder builder) {
        builder.appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true);
    }
}
