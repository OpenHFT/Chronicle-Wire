package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.util.StringUtils;

import java.util.Arrays;

/**
 * Unsigned 64-bit number.
 */
public class Base40LongConverter implements LongConverter {
    private static final String CHARS = ".ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_:+";
    public static final Base40LongConverter UPPER = new Base40LongConverter(CHARS);
    public static final Base40LongConverter LOWER = new Base40LongConverter(CHARS.toLowerCase());
    public static final Base40LongConverter INSTANCE = UPPER;

    private final char[] decode;
    private final byte[] encode = new byte[128];

    private static final int BASE = 40;

    public Base40LongConverter(String chars) {
        decode = chars.toCharArray();
        assert decode.length == BASE;
        Arrays.fill(encode, (byte) -1);
        // support both cases
        for (int i = 0; i < decode.length; i++) {
            char c = decode[i];
            encode[Character.toLowerCase(c)] = (byte) i;
            encode[Character.toUpperCase(c)] = (byte) i;
        }
    }

    @Override
    public long parse(CharSequence text) {
        long v = 0;
        for (int i = 0; i < text.length(); i++) {
            byte b = encode[text.charAt(i)];
            if (b >= 0)
                v = v * BASE + b;
        }
        return v;
    }

    @Override
    public void append(StringBuilder text, long value) {
        int start = text.length();
        if (value < 0) {
            long hi = (value >>> 32);
            long h2 = hi / BASE, mod = hi % BASE;
            long val2 = (mod << 32) + (value & 0xFFFFFFFFL);
            int l2 = (int) (val2 / BASE), v = (int) (val2 % BASE);
            text.append(decode[v]);
            value = (h2 << 32) + (l2 & 0xFFFFFFFFL);
        }
        while (value != 0) {
            int v = (int) (value % BASE);
            value /= BASE;
            text.append(decode[v]);
        }
        StringUtils.reverse(text, start);
    }
}
