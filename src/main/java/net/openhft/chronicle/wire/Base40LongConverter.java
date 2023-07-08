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
 * Unsigned 64-bit number.
 */
@Deprecated(/* to remove in x.25 */)
public class Base40LongConverter extends DelegatingLongConverter {

    public static final int MAX_LENGTH = LongConverter.maxParseLength(40);
    public static final Base40LongConverter UPPER = new Base40LongConverter();
    public static final Base40LongConverter INSTANCE = UPPER;
    private static final String CHARS = ".ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_^~";
    public static final Base40LongConverter LOWER = new Base40LongConverter(CHARS.toLowerCase());

    private Base40LongConverter() {
        this(CHARS);
    }

    private Base40LongConverter(String chars) {
        super(chars);
        // support both cases
        for (int i = 0; i < chars.length(); i++) {
            char c = chars.charAt(i);
            if (Character.isLowerCase(c))
                converter.addEncode(Character.toUpperCase(c), c);
            else
                converter.addEncode(Character.toLowerCase(c), c);
        }
    }
}
