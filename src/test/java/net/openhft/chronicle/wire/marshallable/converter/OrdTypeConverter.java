package net.openhft.chronicle.wire.marshallable.converter;

import net.openhft.chronicle.wire.LongConverter;
import org.jetbrains.annotations.NotNull;

public final class OrdTypeConverter implements LongConverter {
    public static final char UNKNOWN = 0;

    public static final char MARKET = '1';
    public static final char LIMIT = '2';
    public static final char STOP = '3';
    public static final char STOP_LIMIT = '4';
    public static final char PEGGED = 'P';

    @Override
    public long parse(CharSequence text) {
        if (text == null) {
            return 0;
        }
        if (text.length() == 0) {
            return 0;
        }

        if (text.length() == 1) {
            return text.charAt(0);
        }

        return valueOf(text);
    }

    @Override
    public void append(final @NotNull StringBuilder text, final long value) {
        String str = asString0(value);
        text.append(str);
    }

    private @NotNull String asString0(long value) {
        switch ((int) value) {
            case MARKET:
                return "MARKET";
            case LIMIT:
                return "LIMIT";
            case STOP:
                return "STOP";
            case STOP_LIMIT:
                return "STOP_LIMIT";
            case PEGGED:
                return "PEGGED";
            default:
                return "";
        }
    }

    private long valueOf(@NotNull CharSequence value) {
        if ("MARKET".contentEquals(value)) {
            return MARKET;
        }
        if ("LIMIT".contentEquals(value)) {
            return LIMIT;
        }
        if ("STOP".contentEquals(value)) {
            return STOP;
        }
        if ("STOP_LIMIT".contentEquals(value)) {
            return STOP_LIMIT;
        }
        if ("PEGGED".contentEquals(value)) {
            return PEGGED;
        }

        return UNKNOWN;
    }
}