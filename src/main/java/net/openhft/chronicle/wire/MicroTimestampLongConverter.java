package net.openhft.chronicle.wire;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

public class MicroTimestampLongConverter implements LongConverter {
    final DateTimeFormatter dtf = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
            .toFormatter();

    @Override
    public long parse(CharSequence text) {
        TemporalAccessor parse = dtf.parse(text);
        long time = parse.getLong(ChronoField.EPOCH_DAY) * 86400_000_000L;
        if (parse.isSupported(ChronoField.MICRO_OF_DAY))
            time += parse.getLong(ChronoField.MICRO_OF_DAY);
        else if (parse.isSupported(ChronoField.MILLI_OF_DAY))
            time += parse.getLong(ChronoField.MILLI_OF_DAY) * 1_000L;
        else if (parse.isSupported(ChronoField.SECOND_OF_DAY))
            time += parse.getLong(ChronoField.SECOND_OF_DAY) * 1_000_000L;

        return time;
    }

    @Override
    public void append(StringBuilder text, long value) {
        LocalDateTime ldt = LocalDateTime.ofEpochSecond(
                value / 1_000_000,
                (int) (value % 1_000_000 * 1000),
                ZoneOffset.UTC);
        dtf.formatTo(ldt, text);
    }
}
