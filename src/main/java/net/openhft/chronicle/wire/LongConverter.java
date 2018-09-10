package net.openhft.chronicle.wire;

public interface LongConverter {
    long parse(CharSequence text);

    void append(StringBuilder text, long value);

    default String asString(long value) {
        StringBuilder sb = new StringBuilder();
        append(sb, value);
        return sb.toString();
    }
}
