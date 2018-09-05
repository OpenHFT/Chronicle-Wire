package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.util.StringUtils;

import java.math.BigInteger;

public class Base95LongConverter implements LongConverter {
    public static final Base95LongConverter INSTANCE = new Base95LongConverter();
    private static final int BASE = 95;
    private static final BigInteger BASE_BI = BigInteger.valueOf(BASE);

    @Override
    public long parse(CharSequence text) {
        long v = 0;
        for (int i = 0; i < text.length(); i++)
            v = v * BASE + text.charAt(i) - ' ' + 1;
        return v;
    }

    @Override
    public void append(StringBuilder text, long value) {
        int start = text.length();
        if (value < 0) {
            BigInteger bi = BigInteger.valueOf(value).add(BigInteger.ONE.shiftLeft(64));
            int v = bi.mod(BASE_BI).intValueExact();
            value = bi.divide(BASE_BI).longValueExact();
            text.append((char) (' ' + v - 1));
        }
        while (value != 0) {
            int v = (int) (value % BASE);
            value /= BASE;
            text.append((char) (' ' + v - 1));
        }
        StringUtils.reverse(text, start);
    }
}
