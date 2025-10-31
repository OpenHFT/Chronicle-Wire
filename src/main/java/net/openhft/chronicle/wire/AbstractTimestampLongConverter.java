/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.AppendableUtil;
import net.openhft.chronicle.bytes.Bytes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.concurrent.TimeUnit;

/**
 * This abstract class serves as the base for LongConverters that handle timestamp values.
 * The timezone can be set for the subclasses of this converter, and this will be applied to
 * the timestamp values when they are output. When no timezone is specified, the system will
 * default to the one specified by the `timestampLongConverters.zoneId` system property. If this
 * system property is not set, the default will be UTC.
 * <p>
 * All long values that are handled by this converter are assumed to be timestamps in UTC.
 * <p>
 * Parsing of ISO dates, with or without timestamps, is supported. If an ISO date is read with no
 * timezone, it is assumed to be in the converter's zone.
 *
 * @see LongConverter for the interface this abstract class implements.
 */
@SuppressWarnings("this-escape")
public abstract class AbstractTimestampLongConverter implements LongConverter {
    /**
     * Universal Time Coordinated (UTC) timezone
     */
    public static final ZoneId UTC = ZoneId.of("UTC");

    /**
     * System property to specify the ZoneId for timestamp conversion.
     */
    public static final String TIMESTAMP_LONG_CONVERTERS_ZONE_ID_SYSTEM_PROPERTY = "timestampLongConverters.zoneId";

    // The specific timezone used by this converter
    private final ZoneId zoneId;

    // Formatter used for parsing timestamps
    private final DateTimeFormatter formatterForParsing;

    // Formatter used for formatting timestamps
    private final DateTimeFormatter formatterForFormatting;

    // Flag to indicate if UTC dates are written without a suffix
    private final boolean writingUtcDatesWithNoSuffix;

    // The amount of timestamps that fits in a second
    private final long amountPerSecond;

    // The equivalent nanoseconds for a timestamp
    private final long nanosPerAmount;

    /**
     * Constructs a new {@code AbstractTimestampLongConverter} with the specified time unit.
     * The zone ID is fetched from the system property. If the system property is not set, UTC is used.
     *
     * @param timeUnit the time unit for the conversion of long values
     */
    protected AbstractTimestampLongConverter(TimeUnit timeUnit) {
        this(System.getProperty(TIMESTAMP_LONG_CONVERTERS_ZONE_ID_SYSTEM_PROPERTY, UTC.toString()), timeUnit);
    }

    /**
     * Constructs a new {@code AbstractTimestampLongConverter} with the specified
     * zone ID and time unit.
     *
     * @param zoneId   The string representation of the {@link ZoneId} (for
     *                 example "UTC" or "Europe/London") used when a parsed date
     *                 does not contain its own offset. All internal long values
     *                 are treated as UTC based.
     * @param timeUnit The {@link TimeUnit} defining the precision of the long
     *                 timestamp values handled by this converter.
     */
    protected AbstractTimestampLongConverter(String zoneId, TimeUnit timeUnit) {
        this.zoneId = ZoneId.of(zoneId);
        this.writingUtcDatesWithNoSuffix = this.zoneId.equals(UTC);
        this.amountPerSecond = timeUnit.convert(1, TimeUnit.SECONDS);
        this.nanosPerAmount = TimeUnit.NANOSECONDS.convert(1, timeUnit);
        this.formatterForParsing = createFormatter();
        if (writingUtcDatesWithNoSuffix) {
            this.formatterForFormatting = createFormatterWithNoZoneSuffix();
        } else {
            this.formatterForFormatting = formatterForParsing;
        }
    }

    /**
     * Parses the provided text and converts it into a long timestamp.
     * The input may be an ISO-8601 date, a date-time with or without a zone, or
     * a plain numeric timestamp. When no zone is present, the converter's
     * {@link ZoneId} is assumed.
     *
     * @param text The character sequence representing a date, timestamp or
     *             numeric value to be parsed into a UTC long in this
     *             converter's {@link TimeUnit}.
     * @return a long value representing the parsed timestamp
     */
    @Override
    public long parse(CharSequence text) {
        if (text == null || text.length() == 0)
            return 0;
        try {
            if (text.length() > 4 && text.charAt(4) == '/')
                text = text.toString().replace('/', '-');
            final TemporalAccessor parse = formatterForParsing.parse(text);
            if (parse.query(TemporalQueries.zoneId()) != null) {
                return parseFormattedDate(ZonedDateTime.from(parse).withZoneSameInstant(UTC));
            } else {
                return parseFormattedDate(LocalDateTime.from(parse).atZone(zoneId).withZoneSameInstant(UTC));
            }
        } catch (DateTimeParseException dtpe) {
            try {
                return parseTimestamp(Long.parseLong(text.toString()), text);
            } catch (NumberFormatException e) {
                throw dtpe;
            }
        }
    }

    /**
     * Interpret a formatted date that has already been normalised to UTC.
     *
     * @param value The {@link ZonedDateTime} parsed from the text and adjusted
     *              to UTC. The implementation extracts the epoch based long in
     *              this converter's {@link TimeUnit}.
     * @return The value as a long timestamp
     */
    protected abstract long parseFormattedDate(ZonedDateTime value);

    /**
     * Interpret a long value that has been parsed directly from the input text.
     *
     * @param value The numeric value extracted from {@code text} before any unit
     *              conversion is applied.
     * @param text  The original character sequence. Implementations may use it
     *              for logging or context when the conversion is ambiguous.
     * @return The value as a long timestamp
     */
    protected abstract long parseTimestamp(long value, CharSequence text);

    /**
     * Constructs a {@code DateTimeFormatter} for parsing timestamps. The formatter includes fraction parsing
     * and optional offset parsing.
     *
     * @return a newly constructed {@code DateTimeFormatter}
     */
    private DateTimeFormatter createFormatter() {
        final DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss");
        appendFraction(builder);
        builder.optionalStart().appendOffsetId().optionalEnd();
        return builder.toFormatter();
    }

    /**
     * Constructs a {@code DateTimeFormatter} for parsing timestamps without a 'Z' suffix. The formatter includes fraction parsing.
     *
     * @return a newly constructed {@code DateTimeFormatter}
     */
    private DateTimeFormatter createFormatterWithNoZoneSuffix() {
        final DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss");
        appendFraction(builder);
        return builder.toFormatter();
    }

    /**
     * Append the fraction-of-second pattern to the formatter builder.
     *
     * @param builder The {@link DateTimeFormatterBuilder} after the basic date
     *                and time pattern has been added.
     */
    protected abstract void appendFraction(DateTimeFormatterBuilder builder);

    /**
     * Format the timestamp value and append it to the supplied {@link Appendable}.
     *
     * @param text  The destination for the formatted date-time, such as a
     *              {@link StringBuilder} or {@link Bytes} instance.
     * @param value The UTC timestamp in this converter's {@link TimeUnit}.
     */
    public void append(Appendable text, long value) {
        if (value <= 0) {
            AppendableUtil.append(text, value);
            return;
        }
        LocalDateTime ldt = LocalDateTime.ofEpochSecond(
                value / amountPerSecond,
                (int) (value % amountPerSecond * nanosPerAmount),
                ZoneOffset.UTC);
        if (writingUtcDatesWithNoSuffix) {
            formatterForFormatting.formatTo(ldt, text);
        } else {
            formatterForFormatting.formatTo(ZonedDateTime.of(ldt, UTC)
                    .withZoneSameInstant(zoneId), text);
        }
    }

    /**
     * Convenience overload that delegates to {@link #append(Appendable, long)}.
     *
     * @param text  The {@link StringBuilder} to receive the formatted value.
     * @param value The UTC timestamp in this converter's {@link TimeUnit}.
     */
    @Override
    public void append(StringBuilder text, long value) {
        append((Appendable) text, value);
    }

    /**
     * Convenience overload for appending to a {@link Bytes} instance.
     *
     * @param bytes The {@link Bytes} sink for the formatted value.
     * @param value The UTC timestamp in this converter's {@link TimeUnit}.
     */
    @Override
    public void append(Bytes<?> bytes, long value) {
        append((Appendable) bytes, value);
    }
}
