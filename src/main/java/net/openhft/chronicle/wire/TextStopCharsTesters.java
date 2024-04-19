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

import net.openhft.chronicle.bytes.StopCharsTester;

/**
 * Enumerates testers that determine if a combination of characters should act as stopping
 * points during parsing based on various contexts.
 * <p>
 * Each tester in this enum can take into account the current character as well as a peek
 * at the next character to decide if it should signal a stop.
 */
enum TextStopCharsTesters implements StopCharsTester {
    STRICT_END_OF_TEXT {
        @Override
        public boolean isStopChar(int ch, int peekNextCh) throws IllegalStateException {
            switch (ch) {
                // one character stop.
                case '"':
                case '#':
                case '\0':
                case '\r':
                case '\n':
                case '}':
                case ']':
                    return true;
                // two character stop.
                case ':':
                case ',':
                    return isASeparator(peekNextCh);
                default:
                    return false;
            }
        }
    },
    STRICT_END_OF_TEXT_JSON {
        @Override
        public boolean isStopChar(int ch, int peekNextCh) throws IllegalStateException {
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
    },
    END_EVENT_NAME {
        @Override
        public boolean isStopChar(int ch, int peekNextCh) throws IllegalStateException {
            return ch <= ' ' || STRICT_END_OF_TEXT.isStopChar(ch, peekNextCh);
        }
    };

    /**
     * Checks if the given character is a typical separator in textual data.
     *
     * @param peekNextCh The character to test.
     * @return True if the character is a separator; false otherwise.
     */
    public static boolean isASeparator(int peekNextCh) {
        return peekNextCh <= ' '
                || peekNextCh == '!'
                || peekNextCh == '{'
                || peekNextCh == '"'
                || peekNextCh == '[';
    }
}
