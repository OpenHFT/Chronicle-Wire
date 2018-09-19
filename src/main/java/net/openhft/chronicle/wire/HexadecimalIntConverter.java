package net.openhft.chronicle.wire;

public class HexadecimalIntConverter implements IntConverter {
    @Override
    public int parse(CharSequence text) {
        return Integer.parseUnsignedInt(text.toString(), 16);
    }

    @Override
    public void append(StringBuilder text, int value) {
        text.append(Integer.toHexString(value));
    }
}
