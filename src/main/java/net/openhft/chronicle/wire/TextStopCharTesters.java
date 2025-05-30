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

import net.openhft.chronicle.bytes.StopCharTester;
import org.jetbrains.annotations.NotNull;

import java.util.BitSet;

/**
 * Defines testers that determine if a character should act as a stopping
 * character based on various parsing contexts.
 * <p>
 * This enum primarily caters to text parsing scenarios, especially when
 * determining the end of specific types or textual blocks. These are typically
 * used with methods like
 * {@link net.openhft.chronicle.bytes.BytesIn#parseUtf8(Appendable, StopCharTester)}.
 */
enum TextStopCharTesters implements StopCharTester {

    /**
     * A {@link StopCharTester} that stops parsing when a character is not a
     * valid part of a Java identifier. Some YAML-specific characters such as
     * {@code !}, {@code .}, {@code -}, {@code [} and {@code ]} are treated as
     * valid within type tags.
     */
    END_OF_TYPE {
        @NotNull
        private final BitSet eow = TextStopCharTesters.endOfTypeBitSet();
        private final int eowLength = eow.length();

        @Override
        public boolean isStopChar(int ch) {
            return ch >= eowLength || eow.get(ch);
        }
    },
    /**
     * A {@link StopCharTester} that stops parsing at structural characters such
     * as quotes, hash, newline, closing braces, colon or comma.
     */
    END_OF_TEXT {
        @Override
        public boolean isStopChar(int ch) throws IllegalStateException {
            switch (ch) {
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
     * Internal helper to create the {@link BitSet} used by {@link #END_OF_TYPE}.
     * By default, it considers all non-Java identifier characters as stop
     * characters but makes exceptions for a few YAML tag symbols.
     *
     * @return a BitSet representing the stop characters for a type
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
