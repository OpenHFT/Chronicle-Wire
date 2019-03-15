package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.util.StringUtils;

import java.util.Arrays;

/**
 * Unsigned 32-bit number.
 */
public class Base40IntConverter implements IntConverter {
    public static final Base40IntConverter INSTANCE = new Base40IntConverter();
    private static final String CHARS = ".ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789:-+";
    private static final char[] DECODE = CHARS.toCharArray();
    private static final byte[] ENCODE = new byte[128];

    private static final int BASE = 40;

    static {
        assert DECODE.length == BASE;
        Arrays.fill(ENCODE, (byte) -1);
        for (int i = 0; i < DECODE.length; i++) {
            char c = DECODE[i];
            ENCODE[c] = (byte) i;
        }
    }

    @Override
    public int parse(CharSequence text) {
        int v = 0;
        for (int i = 0; i < text.length(); i++) {
            byte b = ENCODE[text.charAt(i)];
            if (b >= 0)
                v = v * BASE + b;
        }
        return v;
    }

    @Override
    public void append(StringBuilder text, int value) {
        int start = text.length();
        long value2 = value & 0xFFFFFFFFL;
        while (value2 != 0) {
            int v = (int) (value2 % BASE);
            value2 /= BASE;
            text.append(DECODE[v]);
        }
        StringUtils.reverse(text, start);
    }
}
