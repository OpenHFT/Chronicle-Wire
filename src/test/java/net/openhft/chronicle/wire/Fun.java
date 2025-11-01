/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.util.SerializableFunction;
import org.jetbrains.annotations.NotNull;

// Enum representing a set of functions that manipulate strings
public enum Fun implements SerializableFunction<String, String> {

    // Enum instance that appends an "A" to the given string
    ADD_A {
        // Apply the function to the input string
        @NotNull
        @Override
        public String apply(String s) {
            // Append 'A' to the given string and return
            return s + "A";
        }
    }
}
