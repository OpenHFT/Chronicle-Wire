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

import net.openhft.chronicle.bytes.StopCharTester;
import org.jetbrains.annotations.NotNull;

import java.util.BitSet;

enum TextStopCharTesters implements StopCharTester {
    END_OF_TYPE {
        @NotNull
        private final BitSet eow = TextStopCharTesters.endOfTypeBitSet();
        private final int eowLength = eow.length();

        @Override
        public boolean isStopChar(int ch) {
            return ch >= eowLength || eow.get(ch);
        }
    },
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
