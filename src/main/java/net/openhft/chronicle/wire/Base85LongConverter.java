package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.util.StringUtils;

import java.math.BigInteger;
import java.util.Arrays;

public class Base85LongConverter implements LongConverter {
    public static final Base85LongConverter INSTANCE = new Base85LongConverter();
    private static final String CHARS = "0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz\"#$%&'()*+,-./ ";
    private static final char[] DECODE = CHARS.toCharArray();
    private static final byte[] ENCODE = new byte[128];
    private static final BigInteger TWO_TO_64 = BigInteger.ONE.shiftLeft(64);

    private static final int BASE = 85;
    private static final BigInteger BASE_BI = BigInteger.valueOf(BASE);

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
            BigInteger bi = BigInteger.valueOf(value).add(TWO_TO_64);
            int v = bi.mod(BASE_BI).intValueExact();
            value = bi.divide(BASE_BI).longValueExact();
            text.append(DECODE[v]);
        }
        while (value != 0) {
            int v = (int) (value % BASE);
            value /= BASE;
            text.append(DECODE[v]);
        }
        StringUtils.reverse(text, start);
    }
}
