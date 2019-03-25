package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.util.StringUtils;

import static net.openhft.chronicle.wire.Base32IntConverter.*;

/**
 * Unsigned 64-bit number with encoding to be as disambigous as possible.
 */
public class Base32LongConverter implements LongConverter {
    public static final Base32LongConverter INSTANCE = new Base32LongConverter();

    @Override
    public long parse(CharSequence text) {
        long v = 0;
        for (int i = 0; i < text.length(); i++) {
            byte b = ENCODE[text.charAt(i)];
            if (b >= 0)
                v = (v << 5) + b;
        }
        return v;
    }

    @Override
    public void append(StringBuilder text, long value) {
        int start = text.length();
        while (value != 0) {
            int v = (int) (value & (BASE - 1));
            value >>>= 5;
            text.append(DECODE[v]);
        }
        StringUtils.reverse(text, start);
    }
}
