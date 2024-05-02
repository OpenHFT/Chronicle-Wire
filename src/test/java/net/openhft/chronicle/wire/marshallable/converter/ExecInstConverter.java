package net.openhft.chronicle.wire.marshallable.converter;

import net.openhft.chronicle.wire.LongConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExecInstConverter implements LongConverter {

    public static final char PRIMARY_PEG = 'R';
    public static final char MIDPRICE_PEG = 'M';
    public static final char NONE = '0'; // non-standard

    @Override
    public long parse(@Nullable final CharSequence text) {
        if (text == null)
            return 0;
        if (text.length() == 0)
            return 0;

        if (text.length() == 1)
            return text.charAt(0);

        return valueOf(text);
    }

    @Override
    public void append(final @NotNull StringBuilder text, final long value) {
        String str = asString0(value);
        text.append(str);
    }

    private @NotNull String asString0(long value) {

        switch ((int) value) {
            case PRIMARY_PEG:
                return "PRIMARY_PEG";
            case MIDPRICE_PEG:
                return "MIDPRICE_PEG";
            case NONE:
                return "NONE";
            default:
                return "";
        }
    }

    private char valueOf(@NotNull CharSequence value) {
        if ("PRIMARY_PEG".contentEquals(value))
            return PRIMARY_PEG;
        if ("MIDPRICE_PEG".contentEquals(value))
            return MIDPRICE_PEG;
        return NONE;
    }
}