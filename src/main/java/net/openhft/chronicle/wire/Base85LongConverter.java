package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.util.StringUtils;

import java.util.Arrays;

public class Base85LongConverter implements LongConverter {
    public static final Base85LongConverter INSTANCE = new Base85LongConverter();
    private static final String CHARS = "0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz\"#$%&'()*+,-./ ";
    private static final char[] DECODE = CHARS.toCharArray();
    private static final byte[] ENCODE = new byte[128];
    private static final int BASE = 85;

    static {
        assert DECODE.length == BASE;
        Arrays.fill(ENCODE, (byte) -1);
        for (int i = 0; i < DECODE.length; i++) {
            char c = DECODE[i];
            ENCODE[c] = (byte) i;
        }
    }

    @Override
    public long parse(CharSequence text) {
        long v = 0;
        for (int i = 0; i < text.length(); i++) {
            byte b = ENCODE[text.charAt(i)];
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
            text.append(DECODE[v]);
            value = (h2 << 32) + (l2 & 0xFFFFFFFFL);
        }
        while (value != 0) {
            int v = (int) (value % BASE);
            value /= BASE;
            text.append(DECODE[v]);
        }
        StringUtils.reverse(text, start);
    }
}
