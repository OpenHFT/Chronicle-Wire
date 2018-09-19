package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.util.StringUtils;

public class Base128LongConverter implements LongConverter {
    public static final Base128LongConverter INSTANCE = new Base128LongConverter();

    @Override
    public long parse(CharSequence text) {
        long v = 0;
        for (int i = 0; i < text.length(); i++)
            v = (v << 7) + text.charAt(i);
        return v;
    }

    @Override
    public void append(StringBuilder text, long value) {
        int start = text.length();
        while (value != 0) {
            text.append((char) (value & 0x7F));
            value >>>= 7;
        }
        StringUtils.reverse(text, start);
    }
}
