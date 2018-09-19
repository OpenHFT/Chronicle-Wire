package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.AppendableUtil;

public class UnsignedIntConverter implements IntConverter {
    private static final long MASK_32 = 0xFFFF_FFFFL;

    @Override
    public int parse(CharSequence text) {
        return Integer.parseUnsignedInt(text.toString());
    }

    @Override
    public void append(StringBuilder text, int value) {
        AppendableUtil.append(text, value & MASK_32);
    }
}
