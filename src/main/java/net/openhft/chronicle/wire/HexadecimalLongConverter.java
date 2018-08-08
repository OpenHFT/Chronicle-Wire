package net.openhft.chronicle.wire;

public class HexadecimalLongConverter implements LongConverter {
    @Override
    public long parse(CharSequence text) {
        return Long.parseUnsignedLong(text.toString(), 16);
    }

    @Override
    public void append(StringBuilder text, long value) {
        text.append(Long.toHexString(value));
    }
}
