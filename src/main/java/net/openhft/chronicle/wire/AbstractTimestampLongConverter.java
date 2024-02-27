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

import net.openhft.chronicle.bytes.AppendableUtil;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;

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
 * <p>
 * As of x.26, the property `timestampLongConverters.includeZoneSuffixWhenZoneIsUTC` will be deprecated
 * and UTC dates will always be written with a 'Z' suffix.
 *
 * @see LongConverter for the interface this abstract class implements.
 */
public abstract class AbstractTimestampLongConverter implements LongConverter {
    /**
     * Universal Time Coordinated (UTC) timezone
     */
    public static final ZoneId UTC = ZoneId.of("UTC");

    /**
     * System property to specify the ZoneId for timestamp conversion.
     */
    public static final String TIMESTAMP_LONG_CONVERTERS_ZONE_ID_SYSTEM_PROPERTY = "timestampLongConverters.zoneId";

    /**
     * System property to specify whether to include the 'Z' suffix for UTC zone timestamps. To be deprecated in x.26 version.
     */
    public static final String INCLUDE_ZONE_SUFFIX_WHEN_ZONE_IS_UTC_SYSTEM_PROPERTY = "timestampLongConverters.includeZoneSuffixWhenZoneIsUTC";

    /**
     * The specific timezone used by this converter.
     */
    private final ZoneId zoneId;

    /**
     * Formatter used for parsing timestamps.
     */
    private final DateTimeFormatter formatterForParsing;

    /**
     * Formatter used for formatting timestamps.
     */
    private final DateTimeFormatter formatterForFormatting;

    /**
     * Flag to indicate if UTC dates are written without a suffix.
     */
    private final boolean writingUtcDatesWithNoSuffix;

    /**
     * The amount of timestamps that fits in a second.
     */
    private final long amountPerSecond;

    /**
     * The equivalent nanoseconds for a timestamp.
     */
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
     * Constructs a new {@code AbstractTimestampLongConverter} with the specified zone ID and time unit.
     * The flag for including zone suffix for UTC is fetched from the system property.
     *
     * @param zoneId   the zone ID to be used for the conversion of long values
     * @param timeUnit the time unit for the conversion of long values
     */
    protected AbstractTimestampLongConverter(String zoneId, TimeUnit timeUnit) {
        this(zoneId, timeUnit, Jvm.getBoolean(INCLUDE_ZONE_SUFFIX_WHEN_ZONE_IS_UTC_SYSTEM_PROPERTY));
    }

    /**
     * Constructs a new {@code AbstractTimestampLongConverter} with the specified zone ID, time unit and flag for including zone suffix for UTC.
     * This constructor is set to be deprecated in x.26 version.
     *
     * @param zoneId                  the zone ID to be used for the conversion of long values
     * @param timeUnit                the time unit for the conversion of long values
     * @param includeZoneSuffixForUTC the flag to indicate if 'Z' suffix should be included for UTC zone timestamps
     */
    @Deprecated(/* To be removed in x.26 */)
    protected AbstractTimestampLongConverter(String zoneId, TimeUnit timeUnit, boolean includeZoneSuffixForUTC) {
        this.zoneId = ZoneId.of(zoneId);
        this.writingUtcDatesWithNoSuffix = this.zoneId.equals(UTC) && !includeZoneSuffixForUTC;
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
     * The text can be an ISO date or a timestamp. If the text includes a timezone, it's used for conversion;
     * otherwise, the converter's timezone is used.
     *
     * @param text the text to be parsed
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
     * Interpret formatted date
     *
     * @param value The parsed formatted date (in UTC zone)
     * @return The value as a long timestamp
     */
    protected abstract long parseFormattedDate(ZonedDateTime value);

    /**
     * Interpret long timestamp
     *
     * @param value The parsed timestamp
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
     * Appends the fraction of the second to the provided {@code DateTimeFormatterBuilder}.
     *
     * @param builder The builder after the initial date format has been added
     */
    protected abstract void appendFraction(DateTimeFormatterBuilder builder);

    public void append(Appendable text, long value) {
        System.out.printf("DEBUG AbstractTimestampLongConverter#append(%s, %s)%n", text, value);
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
     * Appends the provided long value to the given {@code StringBuilder}. This method delegates to {@code append(Appendable, long)}.
     *
     * @param text  the {@code StringBuilder} to append to
     * @param value the long value to be appended
     */
    @Override
    public void append(StringBuilder text, long value) {
        append((Appendable) text, value);
    }

    /**
     * Appends the provided long value to the given {@code Bytes}. This method delegates to {@code append(Appendable, long)}.
     *
     * @param bytes the {@code Bytes} to append to
     * @param value the long value to be appended
     */
    @Override
    public void append(Bytes<?> bytes, long value) {
        append((Appendable) bytes, value);
    }
}
