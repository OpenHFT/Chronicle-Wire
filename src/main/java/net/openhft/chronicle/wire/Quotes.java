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
 * Enumerates the types of quotation marks.
 * This enum represents the most common quotation marks: none, single, and double.
 * Each enumeration value is associated with its corresponding character representation.
 *
 * @since 2023-09-11
 */
enum Quotes {

    /** Represents the absence of a quotation mark. */
    NONE(' '),

    /** Represents a single quotation mark. */
    SINGLE('\''),

    /** Represents a double quotation mark. */
    DOUBLE('"');

    // The character representation of the quotation mark
    final char q;

    /**
     * Constructs a new instance of {@code Quotes} with the provided character representation.
     *
     * @param q The character representation of the quotation mark.
     */
    Quotes(char q) {
        this.q = q;
    }
}
