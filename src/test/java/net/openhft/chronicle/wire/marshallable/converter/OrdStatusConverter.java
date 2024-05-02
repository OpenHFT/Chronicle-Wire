package net.openhft.chronicle.wire.marshallable.converter;

import net.openhft.chronicle.wire.LongConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class OrdStatusConverter implements LongConverter {
    public static final char NEW = '0';
    public static final char PARTIALLY_FILLED = '1';
    public static final char FILLED = '2';
    public static final char CANCELED = '4';
    public static final char REPLACED = '5';
    public static final char REJECTED = '8';
    public static final char SUSPENDED = '9';
    public static final char EXPIRED = 'C';

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
            case OrdStatusConverter.NEW:
                return "NEW";
            case OrdStatusConverter.PARTIALLY_FILLED:
                return "PARTIALLY_FILLED";
            case OrdStatusConverter.FILLED:
                return "FILLED";
            case OrdStatusConverter.CANCELED:
                return "CANCELED";
            case OrdStatusConverter.REPLACED:
                return "REPLACED";
            case OrdStatusConverter.REJECTED:
                return "REJECTED";
            case OrdStatusConverter.SUSPENDED:
                return "SUSPENDED";
            case OrdStatusConverter.EXPIRED:
                return "EXPIRED";
            default:
                return "";
        }
    }

    private long valueOf(@NotNull CharSequence value) {
        if ("NEW".contentEquals(value)) {
            return OrdStatusConverter.NEW;
        }
        if ("PARTIALLY_FILLED".contentEquals(value)) {
            return OrdStatusConverter.PARTIALLY_FILLED;
        }
        if ("FILLED".contentEquals(value)) {
            return OrdStatusConverter.FILLED;
        }
        if ("CANCELED".contentEquals(value)) {
            return OrdStatusConverter.CANCELED;
        }
        if ("REPLACED".contentEquals(value)) {
            return OrdStatusConverter.REPLACED;
        }
        if ("REJECTED".contentEquals(value)) {
            return OrdStatusConverter.REJECTED;
        }
        if ("SUSPENDED".contentEquals(value)) {
            return OrdStatusConverter.SUSPENDED;
        }
        if ("EXPIRED".contentEquals(value)) {
            return OrdStatusConverter.EXPIRED;
        }
        throw new IllegalArgumentException(value + " is not recognised");
    }
}