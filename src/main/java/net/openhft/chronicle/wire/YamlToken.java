/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

/**
 * Enumerates the different types of tokens that can be found in a YAML document.
 * Each token represents a distinct construct or symbol in YAML, which can be used
 * for tasks such as parsing or tokenisation of YAML documents.
 */
public enum YamlToken {
    /**
     * Represents no specific token, often used as a sentinel or when parsing
     * reaches an ambiguous state.
     */
    NONE,

    /** A YAML comment line (starts with '#'). */
    COMMENT,

    /** A YAML tag such as {@code !str} or {@code !!map}. */
    TAG,

    /** A YAML directive like {@code %YAML 1.2}. */
    DIRECTIVE,

    /** The end of a YAML document marker ({@code ...}). */
    DOCUMENT_END(),

    /** The end of the directives section marker ({@code ---}). */
    DIRECTIVES_END(DOCUMENT_END),

    /** Indicates that the following token is a key in a YAML mapping. */
    MAPPING_KEY(NONE),

    /** The end of a YAML mapping (<code>}</code>). */
    MAPPING_END(),

    /** The start of a YAML mapping (<code>{</code>). */
    MAPPING_START(MAPPING_END),

    /** The end of a YAML sequence ({@code ]}). */
    SEQUENCE_END(),

    /** Indicates a new entry in a YAML sequence. */
    SEQUENCE_ENTRY,

    /** The start of a YAML sequence (<code>[</code> or indicated by <code>-</code>). */
    SEQUENCE_START(SEQUENCE_END),

    /** A scalar textual value (string, number or boolean). */
    TEXT,

    /** A literal block scalar introduced by {@code |} or {@code >}. */
    LITERAL,

    /** A YAML anchor such as {@code &anchor_name}. */
    ANCHOR,

    /** A YAML alias such as {@code *anchor_name}. */
    ALIAS,

    /** A reserved YAML indicator, for example {@code @} or {@code `}. */
    RESERVED,

    /** The end of a YAML stream. */
    STREAM_END,

    /** The start of a YAML stream, implicit before any content. */
    STREAM_START(STREAM_END);

    /**
     * For tokens that represent the start of a block structure (for example
     * {@link #MAPPING_START}, {@link #SEQUENCE_START} or {@link #STREAM_START}),
     * this field holds the token that marks the end of that block, such as
     * {@link #MAPPING_END}. It is {@code null} when the token does not start a
     * block.
     */
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
