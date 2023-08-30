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
 * This class provides an implementation for encoding/decoding long values to/from a Base85 string.
 * The provided Base85 string consists of the specified characters in the CHARS constant.
 *
 * @see AbstractLongConverter
 * @see LongConverter
 */
public class Base85LongConverter extends AbstractLongConverter {

    /**
     * The maximum length of a parseable string.
     */
    public static final int MAX_LENGTH = 10;

    /**
     * Singleton instance of the Base85LongConverter.
     */
    public static final Base85LongConverter INSTANCE = new Base85LongConverter();

    /**
     * The character set for Base85 encoding.
     */
    private static final String CHARS =
            "0123456789" +
                    ":;<=>?@" +
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ_" +
                    "abcdefghijklmnopqrstuvwxyz" +
                    "\"#$%&'()*+,-./ ";

    /**
     * Private constructor to prevent external instantiation.
     */
    private Base85LongConverter() {
        super(CHARS);
    }

    /**
     * Provides the maximum length of the parseable string.
     *
     * @return the maximum length of the parseable string.
     */
    @Override
    public int maxParseLength() {
        return MAX_LENGTH;
    }

    /**
     * Determines if all characters in the wireOut are safe.
     *
     * @param wireOut the WireOut instance to check.
     * @return false, as not all characters in Base85 are safe.
     */
    @Override
    public boolean allSafeChars(WireOut wireOut) {
        return false;
    }
}
