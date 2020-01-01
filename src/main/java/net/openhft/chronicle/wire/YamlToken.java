package net.openhft.chronicle.wire;

public enum YamlToken {
    COMMENT,
    TAG,
    DIRECTIVE,
    DIRECTIVES_END,
    DOCUMENT_END,
    MAPPING_START,
    MAPPING_KEY,
    MAPPING_END,
    SEQUENCE_START,
    SEQUENCE_END,
    SEQUENCE_ENTRY,
    TEXT,
    ANCHOR,
    ALIAS,
    RESERVED,
    NONE
}
