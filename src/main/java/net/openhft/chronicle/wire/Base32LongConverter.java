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

/**
 * Unsigned 64-bit number with encoding to be as disambiguated as possible.
 */
public class Base32LongConverter extends AbstractLongConverter {

    public static final Base32LongConverter INSTANCE = new Base32LongConverter();
    private static final String CHARS = "234567ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public Base32LongConverter() {
        super(CHARS);

        converter.addEncode('0', 'O');
        converter.addEncode('1', 'l');
        converter.addEncode('8', 'B');
        converter.addEncode('9', 'q');
        for (char ch = 'a'; ch <= 'z'; ch++)
            converter.addEncode(ch, Character.toUpperCase(ch));
    }
}
