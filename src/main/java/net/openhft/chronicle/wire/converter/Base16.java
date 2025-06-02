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
 * Annotation for fields or parameters to indicate that the annotated long value
 * represents a string of 0 to 16 characters encoded in Base16.
 * <p>
 * This allows for consistent, self-descriptive annotations in fields or parameters,
 * guiding developers and API users about the intended format of the long value.
 * <p>
 * The Base16 format is often used for representing binary data in an ASCII string format,
 * and this annotation helps in ensuring that the long value adheres to this representation.
 *
 * @see PowerOfTwoLongConverter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@LongConversion(Base16.class)
public @interface Base16 {

    /**
     * A shared instance of {@link PowerOfTwoLongConverter} for Base16 conversions.
     * <p>
     * This instance is initialized with symbols "0123456789abcdef" to represent
     * the 16 possible values in a Base16 system.
     */
    LongConverter INSTANCE = new PowerOfTwoLongConverter("0123456789abcdef");
}
