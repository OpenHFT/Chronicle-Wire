package net.openhft.chronicle.wire.marshallable.converter;

import net.openhft.chronicle.wire.LongConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ExecTypeConverter implements LongConverter {
    public static final char UNKNOWN = 0;

    public static final char REJECTED = '6';
    public static final char NEW = '0';
    public static final char CANCELED = '4';
    public static final char REPLACED = '5';
    public static final char EXPIRED = 'C';
    public static final char RESTATED = 'D';
    public static final char TRADE = 'F';
    public static final char SUSPENDED = '9';


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
            case NEW:
                return "NEW";
            case CANCELED:
                return "CANCELED";
            case REPLACED:
                return "REPLACED";
            case REJECTED:
                return "REJECTED";
            case SUSPENDED:
                return "SUSPENDED";
            case EXPIRED:
                return "EXPIRED";
            case RESTATED:
                return "RESTATED";
            case TRADE:
                return "TRADE";

            default:
                return "";
        }
    }

    private long valueOf(@NotNull CharSequence value) {
        if ("NEW".contentEquals(value)) {
            return NEW;
        }
        if ("CANCELED".contentEquals(value)) {
            return CANCELED;
        }
        if ("REPLACED".contentEquals(value)) {
            return REPLACED;
        }
        if ("REJECTED".contentEquals(value)) {
            return REJECTED;
        }
        if ("SUSPENDED".contentEquals(value)) {
            return SUSPENDED;
        }
        if ("EXPIRED".contentEquals(value)) {
            return EXPIRED;
        }
        if ("RESTATED".contentEquals(value)) {
            return RESTATED;
        }
        if ("TRADE".contentEquals(value)) {
            return TRADE;
        }
        return UNKNOWN;
    }
}