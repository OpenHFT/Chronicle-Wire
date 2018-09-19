package net.openhft.chronicle.wire;

public interface IntConverter {
    int parse(CharSequence text);

    void append(StringBuilder text, int value);

    default String asString(int value) {
        StringBuilder sb = new StringBuilder();
        append(sb, value);
        return sb.toString();
    }
}
