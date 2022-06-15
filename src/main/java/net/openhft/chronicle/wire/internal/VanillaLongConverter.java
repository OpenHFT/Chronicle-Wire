package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.StringUtils;
import net.openhft.chronicle.wire.LongConverter;

import java.util.Arrays;

public class VanillaLongConverter implements LongConverter {
    private final int factor;
    private final short[] encode;
    private final char[] decode;
    private final int maxParseLength;

    public VanillaLongConverter(String symbols) {
        final int length = symbols.length();
        factor = length;
        decode = symbols.toCharArray();
        encode = new short[128];
        Arrays.fill(encode, (short) -1);
        for (int i = 0; i < decode.length; i++)
            encode[decode[i]] = (short) i;
        maxParseLength = LongConverter.maxParseLength(length);
    }

    @Override
    public int maxParseLength() {
        return maxParseLength;
    }

    @Override
    public long parse(CharSequence text) {
        lengthCheck(text);
        long v = 0;
        for (int i = 0; i < text.length(); i++) {
            final char ch = text.charAt(i);
            if (ch >= encode.length || encode[ch] < 0)
                throw new IllegalArgumentException("Unexpected character '" + ch + "' in \"" + text + "\"");
            v = v * factor + encode[ch];
        }
        return v;
    }

    @Override
    public void append(StringBuilder text, long value) {
        final int start = text.length();
        if (value < 0) {
            int v = (int) Long.remainderUnsigned(value, factor);
            value = Long.divideUnsigned(value, factor);
            text.append(decode[v]);
        }

        while (value != 0) {
            int v = (int) (value % factor);
            value /= factor;
            text.append(decode[v]);
        }

        StringUtils.reverse(text, start);

        if (text.length() > start + maxParseLength()) {
            Jvm.warn().on(getClass(), "truncated because the value was too large");
            text.setLength(start + maxParseLength());
        }
    }

    public void append(Bytes<?> text, long value) {
        final int start = text.length();
        if (value < 0) {
            int v = (int) Long.remainderUnsigned(value, factor);
            value = Long.divideUnsigned(value, factor);
            text.append(decode[v]);
        }

        while (value != 0) {
            int v = (int) (value % factor);
            value /= factor;
            text.append(decode[v]);
        }

        BytesUtil.reverse(text, start);

        if (text.length() > start + maxParseLength()) {
            Jvm.warn().on(getClass(), "truncated because the value was too large");
            text.readLimit(start + maxParseLength());
        }
    }

    public void addEncode(char alias, char as) {
        encode[alias] = encode[as];
    }
}