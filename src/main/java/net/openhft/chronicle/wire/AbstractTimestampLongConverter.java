package net.openhft.chronicle.wire;

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
 * The children of this class can be given a timezone, which will be applied to values
 * when they are output. When no timezone is given the zone to use is read from the
 * system property `timestampLongConverters.zoneId`. If this is not set, UTC is used.
 * <p>
 * All long values are assumed to be timestamps in UTC.
 * <p>
 * Parsing of ISO dates with or without timestamps is supported. When an ISO date
 * is read with no timezone, it is assumed to be in the converter's zone.
 */
public abstract class AbstractTimestampLongConverter implements LongConverter {
    public static final ZoneId UTC = ZoneId.of("UTC");
    public static final String TIMESTAMP_LONG_CONVERTERS_ZONE_ID_SYSTEM_PROPERTY = "timestampLongConverters.zoneId";
    private final ZoneId zoneId;
    private final DateTimeFormatter dtf;
    private final long amountPerSecond;
    private final long nanosPerAmount;

    protected AbstractTimestampLongConverter(TimeUnit timeUnit) {
        this(Jvm.getProperty(TIMESTAMP_LONG_CONVERTERS_ZONE_ID_SYSTEM_PROPERTY, UTC.toString()), timeUnit);
    }

    protected AbstractTimestampLongConverter(String zoneId, TimeUnit timeUnit) {
        this.zoneId = ZoneId.of(zoneId);
        this.amountPerSecond = timeUnit.convert(1, TimeUnit.SECONDS);
        this.nanosPerAmount = TimeUnit.NANOSECONDS.convert(1, timeUnit);
        this.dtf = createFormatter();
    }

    @Override
    public long parse(CharSequence text) {
        if (text == null || text.length() == 0)
            return 0;
        try {
            if (text.length() > 4 && text.charAt(4) == '/')
                text = text.toString().replace('/', '-');
            final TemporalAccessor parse = dtf.parse(text);
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

    private DateTimeFormatter createFormatter() {
        final DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss");
        appendFraction(builder);
        builder.optionalStart().appendOffsetId().optionalEnd();
        return builder.toFormatter();
    }

    /**
     * Append any fractions we expect to parse
     *
     * @param builder The builder after the initial date format has been added
     */
    protected abstract void appendFraction(DateTimeFormatterBuilder builder);

    @Override
    public void append(StringBuilder text, long value) {
        if (value <= 0) {
            text.append(value);
            return;
        }
        LocalDateTime ldt = LocalDateTime.ofEpochSecond(
                value / amountPerSecond,
                (int) (value % amountPerSecond * nanosPerAmount),
                ZoneOffset.UTC);
        if (zoneId.equals(UTC)) {
            dtf.formatTo(ldt, text);
        } else {
            dtf.formatTo(ZonedDateTime.of(ldt, UTC)
                    .withZoneSameInstant(zoneId), text);
        }
    }
}
