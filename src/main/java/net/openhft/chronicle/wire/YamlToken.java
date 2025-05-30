/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
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
 * for tasks such as parsing or tokenization of YAML documents.
 */
public enum YamlToken {
    /** No token present. */
    NONE,

    /** A comment line beginning with '#'. */
    COMMENT,

    /** A YAML tag such as <code>!type</code>. */
    TAG,

    /** A YAML directive starting with '%'. */
    DIRECTIVE,

    /** End of a document marker (<code>...</code>). */
    DOCUMENT_END(),

    /** Marker for the end of the directives section (<code>---</code>). */
    DIRECTIVES_END(DOCUMENT_END),

    /** A key within a mapping. */
    MAPPING_KEY(NONE),

    /** End of a mapping (<code>}</code>). */
    MAPPING_END(),

    /** Start of a mapping (<code>{</code>). */
    MAPPING_START(MAPPING_END),

    /** End of a sequence (<code>]</code>). */
    SEQUENCE_END(),

    /** Entry within a sequence. */
    SEQUENCE_ENTRY,

    /** Start of a sequence (<code>[</code> or '-'). */
    SEQUENCE_START(SEQUENCE_END),

    /** Scalar text value. */
    TEXT,

    /** Literal block (<code>|</code> or <code>></code>). */
    LITERAL,

    /** Anchor (e.g. <code>&amp;id</code>). */
    ANCHOR,

    /** Alias referencing an anchor (e.g. <code>*id</code>). */
    ALIAS,

    /** Reserved indicator. */
    RESERVED,

    /** End of the YAML stream. */
    STREAM_END,

    /** Start of the YAML stream. */
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
