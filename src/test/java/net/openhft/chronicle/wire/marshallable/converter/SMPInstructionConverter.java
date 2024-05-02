package net.openhft.chronicle.wire.marshallable.converter;

import net.openhft.chronicle.wire.LongConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SMPInstructionConverter implements LongConverter {
    public static final char CANCEL_NEW = 'N';
    public static final char CANCEL_OLD = 'O';
    public static final char NONE = '0';

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
            case CANCEL_NEW:
                return "CANCEL_NEW";
            case CANCEL_OLD:
                return "CANCEL_OLD";
            case NONE:
                return "NONE";
            default:
                return "";
        }
    }

    private char valueOf(@NotNull CharSequence value) {
        if ("CANCEL_NEW".contentEquals(value)) {
            return CANCEL_NEW;
        }
        if ("CANCEL_OLD".contentEquals(value)) {
            return CANCEL_OLD;
        }
        return NONE;
    }
}