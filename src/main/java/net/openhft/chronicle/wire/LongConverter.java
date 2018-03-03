package net.openhft.chronicle.wire;

public interface LongConverter {
    long parse(CharSequence text);

    void append(StringBuilder text, long value);
}
