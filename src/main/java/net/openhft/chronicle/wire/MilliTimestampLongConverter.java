/*
 * Copyright 2016-2020 chronicle.software
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

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.time.LongTime;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.concurrent.TimeUnit;
/**
 * A {@code MilliTimestampLongConverter} is an implementation of {@code AbstractTimestampLongConverter}
 * which handles conversions between long timestamps and date-time strings.
 * The precision of this converter is to the millisecond.
 * This converter is singleton, the instance can be accessed using the public field INSTANCE.
 */
public class MilliTimestampLongConverter extends AbstractTimestampLongConverter {

    /**
     * The singleton instance of this converter.
     */
    public static final MilliTimestampLongConverter INSTANCE = new MilliTimestampLongConverter();

    /**
     * Constructs a new {@code MilliTimestampLongConverter} with the default zone ID (fetched from the system property or UTC).
     */
    public MilliTimestampLongConverter() {
        super(TimeUnit.MILLISECONDS);
    }

    /**
     * Constructs a new {@code MilliTimestampLongConverter} with the specified zone ID.
     *
     * @param zoneId the zone ID to be used for the conversion of long values
     */
    public MilliTimestampLongConverter(String zoneId) {
        super(zoneId, TimeUnit.MILLISECONDS);
    }

    /**
     * Parses a formatted date into a long timestamp.
     * This implementation uses the epoch day and time of the day to compute the long timestamp.
     *
     * @param value The parsed formatted date (in UTC zone)
     * @return The value as a long timestamp
     */
    @Override
    protected long parseFormattedDate(ZonedDateTime value) {
        long time = value.getLong(ChronoField.EPOCH_DAY) * 86400_000L;
        if (value.isSupported(ChronoField.MILLI_OF_DAY))
            time += value.getLong(ChronoField.MILLI_OF_DAY);
        else if (value.isSupported(ChronoField.SECOND_OF_DAY))
            time += value.getLong(ChronoField.SECOND_OF_DAY) * 1_000L;

        return time;
    }

    /**
     * Parses a long timestamp.
     * The provided timestamp value is converted to milliseconds.
     * A debug log is printed if the timestamp is in milliseconds or if it's a non-zero value.
     *
     * @param value The parsed timestamp
     * @param text  The text version of the timestamp
     * @return The value as a long timestamp
     */
    @Override
    protected long parseTimestamp(long value, CharSequence text) {
        long number = LongTime.toMillis(value);
        if (LongTime.isMillis(number)) {
            Jvm.debug().on(getClass(), "In input data, replace " + text + " with " + asString(number));
        } else {
            if (number != 0)
                Jvm.debug().on(getClass(), "In input data, replace " + text + " with a real date.");
        }
        return number;
    }

    /**
     * Appends the fraction of the second to the provided {@code DateTimeFormatterBuilder}.
     * The fraction is defined in milliseconds and can be 0 to 3 digits long.
     *
     * @param builder The builder after the initial date format has been added
     */
    @Override
    protected void appendFraction(DateTimeFormatterBuilder builder) {
        builder.appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true);
    }
}
