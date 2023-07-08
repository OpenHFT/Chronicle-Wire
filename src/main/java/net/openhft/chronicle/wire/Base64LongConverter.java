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
 * The Base64LongConverter class is a subclass of DelegatingLongConverter.
 * It represents an unsigned 64-bit number and encodes it using a custom Base64 scheme.
 * The custom Base64 encoding includes all alphanumeric characters (0-9, A-Z, and a-z),
 * along with period (.) and underscore (_).
 */
public class Base64LongConverter extends DelegatingLongConverter {

    /**
     * A singleton instance of the Base64LongConverter.
     */
    public static final Base64LongConverter INSTANCE = new Base64LongConverter();

    /**
     * The character set used for the custom Base64 encoding.
     */
    private static final String CHARS = ".ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_";

    /**
     * Constructs a Base64LongConverter.
     * This private constructor initializes the internal LongConverter with a custom defined character set.
     */
    private Base64LongConverter() {
        super(CHARS);
    }
}
