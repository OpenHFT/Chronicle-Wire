package net.openhft.chronicle.wire;

public class Base256IntConverter implements IntConverter {
    public static final Base256IntConverter INSTANCE = new Base256IntConverter();

    @Override
    public int parse(CharSequence text) {
        int value = 0;
        for (int i = 0; i < 4 && i < text.length(); i++) {
            value <<= 8;
            value |= text.charAt(i) & 0xFF;
        }
        return value;
    }

    @Override
    public void append(StringBuilder text, int value) {
        int chars = (32 - Integer.numberOfLeadingZeros(value) + 7) / 8;
        for (int i = chars - 1; i >= 0; i--) {
            text.append((char) ((value >> 8 * i) & 0xFF));
        }
    }
}
