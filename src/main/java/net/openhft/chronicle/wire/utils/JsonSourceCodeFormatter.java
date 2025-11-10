//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire.utils;

/**
 * This is the JsonSourceCodeFormatter class, extending the functionality of the {@link SourceCodeFormatter} class.
 * It provides a specialized formatting behavior tailored for JSON content. By default, it uses an indentation of 2 spaces,
 * adhering to common standards for JSON formatting.
 */
public class JsonSourceCodeFormatter extends SourceCodeFormatter {

    /**
     * Constructor for the JsonSourceCodeFormatter class.
     * Initializes the formatter with an indentation of 2 spaces.
     */
    public JsonSourceCodeFormatter() {
        super(2);
    }
}
