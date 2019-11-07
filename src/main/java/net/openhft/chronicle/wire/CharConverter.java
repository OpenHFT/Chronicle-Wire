package net.openhft.chronicle.wire;

public interface CharConverter {
    char parse(CharSequence text);

    void append(StringBuilder text, char value);

    default String asString(char value) {
        StringBuilder sb = new StringBuilder();
        append(sb, value);
        return sb.toString();
    }
}
