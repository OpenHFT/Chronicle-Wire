package net.openhft.chronicle.wire;

public class WordsIntConverter implements IntConverter {
    private final LongConverter longConverter;

    public WordsIntConverter() {
        longConverter = new WordsLongConverter();
    }

    public WordsIntConverter(char sep) {
        longConverter = new WordsLongConverter(sep);
    }

    @Override
    public int parse(CharSequence text) {
        return (int) longConverter.parse(text);
    }

    @Override
    public void append(StringBuilder text, int value) {
        longConverter.append(text, value & 0xFFFFFFFFL);
    }

}
