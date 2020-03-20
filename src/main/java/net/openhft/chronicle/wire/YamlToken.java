package net.openhft.chronicle.wire;

public enum YamlToken {
    NONE,
    COMMENT,
    TAG,
    DIRECTIVE,
    DOCUMENT_END(),
    DIRECTIVES_END(DOCUMENT_END),
    MAPPING_KEY(NONE),
    MAPPING_END(),
    MAPPING_START(MAPPING_END),
    SEQUENCE_END(),
    SEQUENCE_ENTRY,
    SEQUENCE_START(SEQUENCE_END),
    TEXT,
    ANCHOR,
    ALIAS,
    RESERVED,
    STREAM_END,
    STREAM_START(STREAM_END);

    public final YamlToken toEnd;

    YamlToken() {
        this(null);
    }

    YamlToken(YamlToken toEnd) {
        this.toEnd = toEnd;
    }
}
