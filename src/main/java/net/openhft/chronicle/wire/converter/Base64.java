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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate that a given field or parameter, represented as a long value,
 * should be treated as a string containing 0 to 10 characters in Base64 format.
 * <p>
 * When this annotation is applied to a field or parameter, it provides a hint about the expected format
 * and representation of the data, allowing for potential encoding and decoding operations based on Base64.
 * </p>
 * <p>
 * The provided {@link #INSTANCE} is a default converter that can be used for operations relevant to the Base64 format.
 * </p>
 *
 * <b>Example:</b>
 * <pre>
 * {@code
 * @Base64
 * private long encodedData;
 * }
 * </pre>
 *
 * @see LongConverter
 * @see PowerOfTwoLongConverter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@LongConversion(Base64.class)
public @interface Base64 {

    /**
     * An instance of {@link PowerOfTwoLongConverter} specifically configured for Base64 conversions.
     * This converter uses a defined character set suitable for Base64 representations.
     *
     * @return the Base64 long converter instance.
     */
    LongConverter INSTANCE = new PowerOfTwoLongConverter(".ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_");
}
