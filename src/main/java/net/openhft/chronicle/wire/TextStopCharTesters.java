/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.StopCharTester;
import org.jetbrains.annotations.NotNull;

import java.util.BitSet;

/**
 * Defines testers that determine if a character should act as a stopping
 * character based on various parsing contexts.
 * <p>
 * This enum primarily caters to text parsing scenarios, especially when
 * determining the end of specific types or textual blocks.
 */
enum TextStopCharTesters implements StopCharTester {

    /**
     * Tester for determining the end of a type.
     * <p>
     * This tester checks if a character is considered to be a termination
     * point based on Java identifier rules, but with a few exceptions.
     */
    END_OF_TYPE {
        @NotNull
        private final BitSet eow = TextStopCharTesters.endOfTypeBitSet();
        private final int eowLength = eow.length();

        @Override
        public boolean isStopChar(int characterCode) {
            return characterCode >= eowLength || eow.get(characterCode);
        }
    },
    END_OF_TEXT {
        @Override
        public boolean isStopChar(int characterCode) throws IllegalStateException {
            switch (characterCode) {
                // one character stop.
                case '"':
                case '#':
                case '\0':
                case '\r':
                case '\n':
                case '}':
                case ']':
                case ':':
                case ',':
                    return true;
                default:
                    return false;
            }
        }
    };

    /**
     * Constructs a BitSet representing characters that mark
     * the end of a type.
     * <p>
     * By default, it considers all non-Java identifier characters as stop chars
     * but makes exceptions for certain characters.
     *
     * @return A BitSet representing the stop characters for a type.
     */
    private static BitSet endOfTypeBitSet() {
        final BitSet eow = new BitSet();
        for (int i = 0; i < 127; i++) {
            if (!Character.isJavaIdentifierPart(i))
                eow.set(i);
        }

        eow.clear('['); // not in spec
        eow.clear(']'); // not in spec
        eow.clear('-'); // not in spec
        eow.clear('!');
        eow.clear('.');
        eow.clear('$');
        return eow;
    }
}
