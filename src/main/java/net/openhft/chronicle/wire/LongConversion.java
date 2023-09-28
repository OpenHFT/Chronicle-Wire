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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * Annotation to specify the conversion strategy for a `long` value.
 * <p>
 * This annotation can be applied to fields, parameters, or other annotations to define
 * a custom conversion strategy using a specific {@link LongConverter} implementation.
 * The referenced converter annotation should either have a static final instance named
 * `INSTANCE` or should be a {@link LongConverter}
 *
 * @see LongConverter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.PARAMETER})
public @interface LongConversion {

    /**
     * Returns the class responsible for converting the long value or an annotation with an INSTANCE of one.
     *<p>
     * The {@link LongConverter} class to be used for conversion.
     * The class specified should either have a static final field named INSTANCE,
     * or a constructor that takes a single string parameter for initialization.
     *
     * @return The implementing class which either contains a static final field named `INSTANCE`
     *         or provides a constructor that takes a string for initialization.
     */
    Class<?> value();
}
