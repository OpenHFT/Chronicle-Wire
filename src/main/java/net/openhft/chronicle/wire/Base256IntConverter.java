package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;

public class Base256IntConverter implements IntConverter {
    public static final int MAX_LENGTH = IntConverter.maxParseLength(256);
    public static final Base256IntConverter INSTANCE = new Base256IntConverter();

    @Override
    public int maxParseLength() {
        return MAX_LENGTH;
    }

    @Override
    public int parse(CharSequence text) {
        lengthCheck(text);
        int value = 0;
        for (int i = 0; i < 4 && i < text.length(); i++) {
            value <<= 8;
            value |= text.charAt(i) & 0xFF;
        }
        return value;
    }

    @Override
    public void append(StringBuilder text, int value) {
        int start = text.length();
        int chars = (32 - Integer.numberOfLeadingZeros(value) + 7) / 8;
        for (int i = chars - 1; i >= 0; i--) {
            text.append((char) ((value >> 8 * i) & 0xFF));
        }
        if (text.length() > start + maxParseLength()) {
            Jvm.warn().on(getClass(), "truncated because the value was too large");
            text.setLength(start + maxParseLength());
        }
    }
}
