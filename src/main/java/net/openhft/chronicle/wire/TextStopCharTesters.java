/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

/*
 * Created by Peter Lawrey on 16/08/15.
 */
enum TextStopCharTesters implements StopCharTester {
    END_OF_TYPE {
        @NotNull
        BitSet EOW = new BitSet();
        {
            for (int i = 0; i < 127; i++)
                if (!Character.isJavaIdentifierPart(i))
                    EOW.set(i);
            EOW.clear('['); // not in spec
            EOW.clear(']'); // not in spec
            EOW.clear('-'); // not in spec
            EOW.clear('!');
            EOW.clear('.');
            EOW.clear('$');
        }

        @Override
        public boolean isStopChar(int ch) {
            return EOW.get(ch);
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
    }
}
