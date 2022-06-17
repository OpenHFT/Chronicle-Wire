package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;

public abstract class AbstractLongConverter implements LongConverter {
    protected final LongConverter converter;

    public AbstractLongConverter(String chars) {
        this(LongConverter.forSymbols(chars));
    }

    public AbstractLongConverter(LongConverter converter) {
        this.converter = converter;
    }

    @Override
    public int maxParseLength() {
        return converter.maxParseLength();
    }

    @Override
    public long parse(CharSequence text) {
        return converter.parse(text);
    }

    @Override
    public void append(StringBuilder text, long value) {
        converter.append(text, value);
    }

    @Override
    public void append(Bytes<?> bytes, long value) {
        converter.append(bytes, value);
    }
}
