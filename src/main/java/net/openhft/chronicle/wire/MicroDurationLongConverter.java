package net.openhft.chronicle.wire;

import java.time.Duration;

public class MicroDurationLongConverter implements LongConverter {

    @Override
    public long parse(CharSequence text) {
        Duration parse = Duration.parse(text);
        long time = parse.getSeconds() * 1000_000 + parse.getNano() / 1000;
        return time;
    }

    @Override
    public void append(StringBuilder text, long value) {
        Duration d = Duration.ofSeconds(value / 1_000_000,
                value % 1_000_000 * 1_000);
        text.append(d);
    }
}
