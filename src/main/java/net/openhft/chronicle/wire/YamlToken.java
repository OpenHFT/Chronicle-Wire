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
     * No token identified for the current input stream.
     */
    NONE,
    /** YAML comment token starting with a '#' marker. */
    COMMENT,
    /** Tag declaration token such as {@code !type} marker. */
    TAG,
    /** YAML directive token such as {@code %YAML} header. */
    DIRECTIVE,
    /** Marks the end of a YAML document. */
    DOCUMENT_END(),
    /** Represents the end of the directives in a YAML document. */
    DIRECTIVES_END(DOCUMENT_END),
    /** Key token within a YAML mapping. */
    MAPPING_KEY(NONE),
    /** End token for a YAML mapping. */
    MAPPING_END(),
    /** Represents the start of a key-value mapping in a YAML document. */
    MAPPING_START(MAPPING_END),
    /** End token for a YAML sequence. */
    SEQUENCE_END(),
    /** Entry token within a YAML sequence. */
    SEQUENCE_ENTRY,
    /** Represents the start of a sequence in a YAML document. */
    SEQUENCE_START(SEQUENCE_END),
    /** Plain scalar text token in YAML. */
    TEXT,
    /** Literal block scalar token in YAML. */
    LITERAL,
    /** Anchor declaration token such as {@code &} marker. */
    ANCHOR,
    /** Anchor alias token such as {@code *} marker. */
    ALIAS,
    /** Reserved token type for future extensions. */
    RESERVED,
    /** Marks the end of a YAML stream. */
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
