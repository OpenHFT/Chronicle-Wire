/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

/**
 * Enumerates the different types of tokens that can be found in a YAML document.
 * Each token represents a distinct construct or symbol in YAML, which can be used
 * for tasks such as parsing or tokenization of YAML documents.
 */
public enum YamlToken {
    /**
     * No token identified.
     */
    NONE,
    /** YAML comment starting with '#'. */
    COMMENT,
    /** Tag declaration such as {@code !type}. */
    TAG,
    /** YAML directive (e.g. %YAML). */
    DIRECTIVE,
    /** Marks the end of a document. */
    DOCUMENT_END(),
    /** Represents the end of the directives in a YAML document. */
    DIRECTIVES_END(DOCUMENT_END),
    /** Key within a mapping. */
    MAPPING_KEY(NONE),
    /** End of a mapping block. */
    MAPPING_END(),
    /** Represents the start of a key-value mapping in a YAML document. */
    MAPPING_START(MAPPING_END),
    /** End of a sequence block. */
    SEQUENCE_END(),
    /** Entry within a sequence. */
    SEQUENCE_ENTRY,
    /** Represents the start of a sequence in a YAML document. */
    SEQUENCE_START(SEQUENCE_END),
    /** Plain scalar text. */
    TEXT,
    /** Literal block scalar. */
    LITERAL,
    /** Anchor declaration (&amp;). */
    ANCHOR,
    /** Anchor alias (*). */
    ALIAS,
    /** Reserved token type. */
    RESERVED,
    /** Marks the end of a stream. */
    STREAM_END,
    /** Represents the start of a YAML document stream. */
    STREAM_START(STREAM_END);

    /** The corresponding end token for certain start tokens. */
    public final YamlToken toEnd;

    /**
     * Default constructor for tokens without a corresponding end token.
     */
    YamlToken() {
        this(null);
    }

    /**
     * Constructs a token with a corresponding end token.
     *
     * @param toEnd The corresponding end token.
     */
    YamlToken(YamlToken toEnd) {
        this.toEnd = toEnd;
    }
}
