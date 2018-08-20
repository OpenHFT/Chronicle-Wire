package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.AppendableUtil;

public class UnsignedLongConverter implements LongConverter {

    @Override
    public long parse(CharSequence text) {
        return Long.parseUnsignedLong(text.toString());
    }

    @Override
    public void append(StringBuilder text, long value) {
        if (value >= 0)
            AppendableUtil.append(text, value);
        else
            text.append(Long.toUnsignedString(value));
    }
}
