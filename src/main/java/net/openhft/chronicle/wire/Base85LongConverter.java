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
 * The Base85LongConverter class is a subclass of DelegatingLongConverter.
 * It represents an unsigned 64-bit number and encodes it using a custom Base85 scheme.
 * The custom Base85 encoding includes all alphanumeric characters, common symbols, and some whitespace characters.
 */
public class Base85LongConverter extends DelegatingLongConverter {

    /**
     * The maximum length for parsing using the Base85 encoding scheme.
     */
    public static final int MAX_LENGTH = 10;

    /**
     * A singleton instance of the Base85LongConverter.
     */
    public static final Base85LongConverter INSTANCE = new Base85LongConverter();

    /**
     * The character set used for the custom Base85 encoding.
     */
    private static final String CHARS =
            "0123456789" +
                    ":;<=>?@" +
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ_" +
                    "abcdefghijklmnopqrstuvwxyz" +
                    "\"#$%&'()*+,-./ ";

    /**
     * Constructs a Base85LongConverter.
     * This private constructor initializes the internal LongConverter with a custom defined character set.
     */
    private Base85LongConverter() {
        super(CHARS);
    }

    /**
     * {@inheritDoc}
     * For the Base85 encoding scheme, the maximum parsing length is 10.
     */
    @Override
    public int maxParseLength() {
        return MAX_LENGTH;
    }

    /**
     * {@inheritDoc}
     * For the Base85 encoding scheme, not all characters are considered safe. Therefore, this method always returns false.
     */
    @Override
    public boolean allSafeChars(WireOut wireOut) {
        return false;
    }
}
