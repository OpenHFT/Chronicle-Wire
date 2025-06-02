/*
 * Copyright 2016-2025 chronicle.software
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation used to associate a concrete {@link LongConverter}
 * implementation with another annotation. Custom marker annotations (for
 * example {@code @Hexadecimal} or {@code @Base64}) can declare which converter
 * should be applied when serialising or deserialising {@code long} fields or
 * parameters.
 *
 * <p>Example:</p>
 * <pre>{@code
 * @LongConversion(HexadecimalLongConverter.class)
 * @Retention(RetentionPolicy.RUNTIME)
 * @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
 * public @interface MyHexFormat {
 * }
 * }</pre>
 *
 * @see LongConverter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.PARAMETER})
public @interface LongConversion {

    /**
     * Specifies the {@link LongConverter} class that provides the logic for
     * converting the value to and from text.
     *
     * @return the {@link LongConverter} implementation class
     */
    Class<? extends LongConverter> value();
}
