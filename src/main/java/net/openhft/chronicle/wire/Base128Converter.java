package net.openhft.chronicle.wire;

public enum Base128Converter implements LongConverter {
    INSTANCE;

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
        while (value > 0) {
            text.append((char) (value & 0x7F));
            value >>>= 7;
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
