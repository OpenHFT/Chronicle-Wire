package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.util.StringUtils;

import java.math.BigInteger;
import java.util.Arrays;

import static net.openhft.chronicle.wire.Base85LongConverter.TWO_TO_64;

/**
 * Unsigned 64-bit number.
 */
public class Base40LongConverter implements LongConverter {
    public static final Base40LongConverter INSTANCE = new Base40LongConverter();
    private static final String CHARS = ".ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_:+";
    private static final char[] DECODE = CHARS.toCharArray();
    private static final byte[] ENCODE = new byte[128];

    private static final int BASE = 40;
    static final BigInteger BASE_BI = BigInteger.valueOf(BASE);

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
