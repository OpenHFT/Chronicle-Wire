package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.internal.VanillaLongConverter;

/**
 * An identifier that acts as a base 66 string of up to 10 characters, or a nanosecond timestamp for dates from 2019-09-14.
 * <p>
 * The base 66 encoding support 0-9, A-Z, a-z, period, underscore, tilde and caret. Leading zeros are truncated.
 * <p>
 * As this is intended for timestamps based on the wall clock, these shouldn't conflict.
 * <p>
 * Negative ids are reserved for application specific encodings.
 */
public class IdentifierLongConverter implements LongConverter {
    public static final IdentifierLongConverter INSTANCE = new IdentifierLongConverter();

    protected static final VanillaLongConverter SMALL_POSITIVE = new VanillaLongConverter(
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz._~^");
    protected static final long MAX_SMALL_ID = 1568336880910795775L; // 66^10-1
    static final String MIN_DATE = "2019-09-13T01:08:00.910795776";
    static final String MAX_DATE = "2262-04-11T23:47:16.854775807";


    protected IdentifierLongConverter() {
    }

    @Override
    public long parse(CharSequence text) {
        return text.length() <= 10
                ? SMALL_POSITIVE.parse(text)
                : NanoTimestampLongConverter.INSTANCE.parse(text);
    }

    @Override
    public void append(StringBuilder text, long value) {
        if (value < 0)
            throw new IllegalArgumentException("value: " + value); // reserved
        if (value <= MAX_SMALL_ID)
            SMALL_POSITIVE.append(text, value);
        else
            NanoTimestampLongConverter.INSTANCE.append(text, value);
    }

    @Override
    public void append(Bytes<?> bytes, long value) {
        if (value < 0)
            throw new IllegalArgumentException("value: " + value); // reserved
        if (value <= MAX_SMALL_ID)
            SMALL_POSITIVE.append(bytes, value);
        else
            NanoTimestampLongConverter.INSTANCE.append(bytes, value);
    }

    @Override
    public int maxParseLength() {
        return NanoTimestampLongConverter.INSTANCE.maxParseLength();
    }
}
