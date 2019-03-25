package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.util.StringUtils;

import java.util.Arrays;

/**
 * Unsigned 32-bit number with encoding to be as disambigous as possible.
 */
public class Base32IntConverter implements IntConverter {
    public static final Base32IntConverter INSTANCE = new Base32IntConverter();
    static final byte[] ENCODE = new byte[128];
    static final int BASE = 32;
    private static final String CHARS = "234567ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    static final char[] DECODE = CHARS.toCharArray();

    static {
        assert DECODE.length == BASE;
        Arrays.fill(ENCODE, (byte) -1);
        for (int i = 0; i < DECODE.length; i++) {
            char c = DECODE[i];
            ENCODE[c] = (byte) i;
            ENCODE[Character.toLowerCase(c)] = (byte) i;
        }
        ENCODE['0'] = ENCODE['O'];
        ENCODE['1'] = ENCODE['l'];
        ENCODE['8'] = ENCODE['B'];
        ENCODE['9'] = ENCODE['q'];
    }

    @Override
    public int parse(CharSequence text) {
        int v = 0;
        for (int i = 0; i < text.length(); i++) {
            byte b = ENCODE[text.charAt(i)];
            if (b >= 0)
                v = (v << 5) + b;
        }
        return v;
    }

    @Override
    public void append(StringBuilder text, int value) {
        int start = text.length();
        while (value != 0) {
            int v = (int) (value & (BASE - 1));
            value >>>= 5;
            text.append(DECODE[v]);
        }
        StringUtils.reverse(text, start);
    }
}
