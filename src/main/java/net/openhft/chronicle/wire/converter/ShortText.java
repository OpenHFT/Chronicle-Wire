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

package net.openhft.chronicle.wire.converter;

import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.LongConverter;
import net.openhft.chronicle.wire.ShortTextLongConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate that a given field or parameter, represented as a long value,
 * should be treated as a string containing 0 to 10 characters in Base85 format. This truncated leading spaces, but preserves leading zero c.f. {@link Base85}
 * <p>
 * Base85, also known as Ascii85, is a binary-to-ASCII encoding scheme that provides
 * an efficient way to encode binary data for transport over text-based protocols.
 * </p>
 * <p>
 * When this annotation is applied to a field or parameter, it provides a hint about the expected format
 * and representation of the data, allowing for potential encoding and decoding operations based on Base85.
 * </p>
 * <p>
 * The provided {@link #INSTANCE} is a default converter that can be used for operations relevant to the Base85 format.
 * </p>
 *
 * <b>Usage Example:</b>
 * <pre>
 * {@code
 * @ShortText
 * private long encodedText;
 * }
 * </pre>
 *
 * @see LongConverter
 * @see Base85
 * @see ShortTextLongConverter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@LongConversion(ShortText.class)
public @interface ShortText {

    /**
     * An instance of {@link ShortTextLongConverter} specifically configured for Base85 conversions.
     * This converter uses a character set defined by the {@link ShortTextLongConverter} to represent Base85 encoded data.
     *
     * @return the specialized Base85 long converter instance for ShortText.
     */
    LongConverter INSTANCE = ShortTextLongConverter.INSTANCE;
}
