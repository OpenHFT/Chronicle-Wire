package net.openhft.chronicle.wire.marshallable.converter;

import net.openhft.chronicle.wire.LongConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SideConverter implements LongConverter {
    public static final char UNKNOWN = 0;

    public static final char BUY = '1';
    public static final char SELL = '2';

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
            case BUY:
                return "BUY";
            case SELL:
                return "SELL";
            default:
                return "";
        }
    }

    private long valueOf(@NotNull CharSequence value) {
        if ("BUY".contentEquals(value)) {
            return BUY;
        }
        if ("SELL".contentEquals(value)) {
            return SELL;
        }
        return UNKNOWN;
    }
}