package net.openhft.chronicle.wire;

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

public abstract class AbstractTimestampLongConverter implements LongConverter {
    public static final ZoneId UTC = ZoneId.of("UTC");
    public static final String TIMESTAMP_LONG_CONVERTERS_ZONE_ID_SYSTEM_PROPERTY = "timestampLongConverters.zoneId";
    private final ZoneId zoneId;
    private final DateTimeFormatter dtf;
    private final long amountPerSecond;
    private final long nanosPerAmount;

    public AbstractTimestampLongConverter(TimeUnit timeUnit) {
        this(System.getProperty(TIMESTAMP_LONG_CONVERTERS_ZONE_ID_SYSTEM_PROPERTY, UTC.toString()), timeUnit);
    }

    public AbstractTimestampLongConverter(String zoneId, TimeUnit timeUnit) {
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
                return parseFormattedDate(parse);
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
     * @param value The parsed formatted date
     * @return The value as a long timestamp
     */
    abstract protected long parseFormattedDate(TemporalAccessor value);

    /**
     * Interpret long timestamp
     *
     * @param value The parsed timestamp
     * @return The value as a long timestamp
     */
    abstract protected long parseTimestamp(long value, CharSequence text);

    private DateTimeFormatter createFormatter() {
        final DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss");
        appendFraction(builder);

        if (!this.zoneId.equals(UTC))
            builder.appendLiteral(' ').appendZoneOrOffsetId();
        else
            // this allows an optional 'Z' on the end so we can support JSON timestamps
            builder.optionalStart().appendZoneId().optionalEnd();
        return builder.toFormatter();
    }

    /**
     * Append any fractions we expect to parse
     *
     * @param builder The builder after the initial date format has been added
     */
    abstract protected void appendFraction(DateTimeFormatterBuilder builder);

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
        dtf.formatTo(ZonedDateTime.of(ldt, UTC)
                .withZoneSameInstant(zoneId), text);
    }
}
