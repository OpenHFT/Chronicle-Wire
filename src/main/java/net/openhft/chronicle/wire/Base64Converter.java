package net.openhft.chronicle.wire;

import java.util.Arrays;

public enum Base64Converter implements LongConverter {
    INSTANCE;
    static final char[] CODES = ".ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuv0123456789+".toCharArray();
    static final byte[] LOOKUP = new byte[128];

    static {
        Arrays.fill(LOOKUP, (byte) -1);
        for (int i = 0; i < CODES.length; i++) {
            char code = CODES[i];
            LOOKUP[code] = (byte) i;
        }
    }

    @Override
    public long parse(CharSequence text) {
        long v = 0;
        for (int i = 0; i < text.length(); i++) {
            byte b = LOOKUP[text.charAt(i)];
            if (b >= 0)
                v = (v << 6) + b;
        }
        return v;
    }

    @Override
    public void append(StringBuilder text, long value) {
        int start = text.length();
        while (value > 0) {
            text.append(CODES[(int) (value & 0x3F)]);
            value >>>= 6;
        }
        int end = text.length() - 1;
        int mid = (start + end + 1) / 2;
        for (int i = 0; i < mid - start; i++) {
            char ch = text.charAt(start + i);
            text.setCharAt(start + i, text.charAt(end - i));
            text.setCharAt(end - i, ch);
        }
    }
}
