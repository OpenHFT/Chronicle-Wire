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
 * This class represents a converter for an unsigned 64-bit number encoded in base64.
 * The encoding scheme utilizes the characters 0-9, A-Z, a-z, as well as period and underscore.
 *
 * <p>This class extends {@link AbstractLongConverter} and provides specific implementation
 * using the defined character set.
 *
 * <p>It follows the singleton design pattern. An instance can be obtained via the {@code INSTANCE}
 * static field.
 */
public class Base64LongConverter extends AbstractLongConverter {

    /**
     * A singleton instance of {@link Base64LongConverter}.
     */
    public static final Base64LongConverter INSTANCE = new Base64LongConverter();

    /**
     * A string representing the set of characters to be used in base64 encoding.
     */
    private static final String CHARS = ".ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_";

    /**
     * Private constructor to enforce the singleton design pattern.
     * Initialises the super class with the base64 character set.
     */
    private Base64LongConverter() {
        super(CHARS);
    }
}
