package net.openhft.chronicle.wire;

public interface IntConverter {
    int parse(CharSequence text);

    void append(StringBuilder text, int value);
}
