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
 * The {@code HexadecimalLongConverter} class provides functionality to convert 64-bit unsigned
 * numbers into their hexadecimal string representation.
 *
 * <p>This class is an extension of the {@link AbstractLongConverter} and leverages its methods
 * to perform the conversion tasks. Designed as a singleton, this class ensures a single instance
 * is used throughout the application, ensuring both consistency and thread-safety.
 * @see AbstractLongConverter
 */
public class HexadecimalLongConverter extends AbstractLongConverter {

    /**
     * The singleton instance of {@code HexadecimalLongConverter}, ensuring a single
     * point of access for this converter across the application.
     */
    public static final LongConverter INSTANCE = new HexadecimalLongConverter();

    /**
     * The character set utilized for converting numbers into hexadecimal format.
     */
    private static final String CHARS = "0123456789abcdef";

    /**
     * Private constructor initializing the hexadecimal converter with the appropriate character set.
     * As this class follows the singleton pattern, this constructor is kept private to prevent direct instantiation.
     */
    private HexadecimalLongConverter() {
        super(CHARS);
    }
}
