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
 * The {@code HexadecimalLongConverter} class is responsible for converting 64-bit unsigned
 * numbers into a hexadecimal representation.
 *
 * <p>This class extends the {@link AbstractLongConverter} class, and inherits its methods
 * to perform the conversions. The class is a singleton, accessed via the {@code INSTANCE}
 * constant. It's thread-safe and can be used across the application.
 *
 * @see AbstractLongConverter
 */
public class HexadecimalLongConverter extends AbstractLongConverter {

    /**
     * The singleton instance of {@code HexadecimalLongConverter}.
     */
    public static final LongConverter INSTANCE = new HexadecimalLongConverter();

    /**
     * The character set used for hexadecimal encoding.
     */
    private static final String CHARS = "0123456789abcdef";

    /**
     * Default constructor that sets up the hexadecimal character set.
     */
    private HexadecimalLongConverter() {
        super(CHARS);
    }
}

