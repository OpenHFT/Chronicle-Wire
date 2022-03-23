/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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
    LITERAL,
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
