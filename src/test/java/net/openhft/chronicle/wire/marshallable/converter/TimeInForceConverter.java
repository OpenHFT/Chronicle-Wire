package net.openhft.chronicle.wire.marshallable.converter;

import net.openhft.chronicle.wire.LongConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TimeInForceConverter implements LongConverter {
    public static final char UNKNOWN = 0;

    public final static char GOOD_TILL_CANCEL = '1';
    public final static char IMMEDIATE_OR_CANCEL = '3';
    public final static char FILL_OR_KILL = '4';
    public final static char GOOD_TILL_DATE = '6';
    public final static char DAY = '0';

    @Override
    public long parse(@Nullable final CharSequence text) {
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
            case DAY:
                return "DAY";
            case GOOD_TILL_CANCEL:
                return "GOOD_TILL_CANCEL";
            case IMMEDIATE_OR_CANCEL:
                return "IMMEDIATE_OR_CANCEL";
            case FILL_OR_KILL:
                return "FILL_OR_KILL";
            case GOOD_TILL_DATE:
                return "GOOD_TILL_DATE";
            default:
                return "";
        }
    }

    private long valueOf(@NotNull CharSequence value) {
        if ("DAY".contentEquals(value)) {
            return DAY;
        }
        if ("GOOD_TILL_CANCEL".contentEquals(value)) {
            return GOOD_TILL_CANCEL;
        }
        if ("IMMEDIATE_OR_CANCEL".contentEquals(value)) {
            return IMMEDIATE_OR_CANCEL;
        }
        if ("FILL_OR_KILL".contentEquals(value)) {
            return FILL_OR_KILL;
        }
        if ("GOOD_TILL_DATE".contentEquals(value)) {
            return GOOD_TILL_DATE;
        }

        return UNKNOWN;
    }
}