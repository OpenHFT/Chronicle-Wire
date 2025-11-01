/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

/**
 * Enumerates the types of quotation marks.
 * This enum represents the most common quotation marks: none, single, and double.
 * Each enumeration value is associated with its corresponding character representation.
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
