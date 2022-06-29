package net.openhft.chronicle.wire.converter;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.util.StringUtils;
import net.openhft.chronicle.wire.LongConverter;

import java.util.Arrays;

public class PowerOfTwoLongConverter implements LongConverter {
    private final int shift, mask;
    private final short[] encode;
    private final char[] decode;
    private final int maxParseLength;

    public PowerOfTwoLongConverter(String symbols) {
        final int length = symbols.length();
        assert Maths.isPowerOf2(length);
        shift = Maths.intLog2(length);
        mask = (1 << shift) - 1;
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
            v = (v << shift) + encode[ch];
        }
        return v;
    }

    @Override
    public void append(StringBuilder text, long value) {
        int start = text.length();
        while (value != 0) {
            int val = (int) (value & mask);
            text.append(decode[val]);
            value >>>= shift;
        }

        StringUtils.reverse(text, start);

        if (text.length() > start + maxParseLength()) {
            Jvm.warn().on(getClass(), "truncated because the value was too large");
            text.setLength(start + maxParseLength());
        }
    }

    @Override
    public void append(Bytes<?> text, long value) {
        int start = text.length();
        while (value != 0) {
            int val = (int) (value & mask);
            text.append(decode[val]);
            value >>>= shift;
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
