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
 *
 * @since 2023-08-29
 */
public enum YamlToken {
    NONE,
    COMMENT,
    TAG,
    DIRECTIVE,
    DOCUMENT_END(),
    /** Represents the end of the directives in a YAML document. */
    DIRECTIVES_END(DOCUMENT_END),
    MAPPING_KEY(NONE),
    MAPPING_END(),
    /** Represents the start of a key-value mapping in a YAML document. */
    MAPPING_START(MAPPING_END),
    SEQUENCE_END(),
    SEQUENCE_ENTRY,
    /** Represents the start of a sequence in a YAML document. */
    SEQUENCE_START(SEQUENCE_END),
    TEXT,
    LITERAL,
    ANCHOR,
    ALIAS,
    RESERVED,
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
