/*
 * Copyright 2016-2022 chronicle.software
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

import net.openhft.chronicle.core.Jvm;

/**
 * @deprecated This doesn't really work as it uses characters that have to be escaped out in text wires
 */
@Deprecated(/* to remove in x.25 */)
public class Base256IntConverter implements IntConverter {
    public static final int MAX_LENGTH = IntConverter.maxParseLength(256);
    public static final Base256IntConverter INSTANCE = new Base256IntConverter();

    @Override
    public int maxParseLength() {
        return MAX_LENGTH;
    }

    @Override
    public int parse(CharSequence text) {
        lengthCheck(text);
        int value = 0;
        for (int i = 0; i < 4 && i < text.length(); i++) {
            value <<= 8;
            value |= text.charAt(i) & 0xFF;
        }
        return value;
    }

    @Override
    public void append(StringBuilder text, int value) {
        int start = text.length();
        int chars = (32 - Integer.numberOfLeadingZeros(value) + 7) / 8;
        for (int i = chars - 1; i >= 0; i--) {
            text.append((char) ((value >> 8 * i) & 0xFF));
        }
        if (text.length() > start + maxParseLength()) {
            Jvm.warn().on(getClass(), "truncated because the value was too large");
            text.setLength(start + maxParseLength());
        }
    }
}
