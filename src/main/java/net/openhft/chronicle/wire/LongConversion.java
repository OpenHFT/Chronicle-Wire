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
 * Annotation used to specify the converter to be used for handling long values.
 * The converter specified by this annotation can be used to implement custom conversion logic for
 * serializing/deserializing long values. The class specified in {@link #value()} should be a valid
 * {@link LongConverter} implementation.
 *
 * @see LongConverter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.PARAMETER})
public @interface LongConversion {
    /**
     * The {@link LongConverter} class to be used for conversion.
     * The class specified should either have a static final field named INSTANCE,
     * or a constructor that takes a single string parameter for initialization.
     *
     * @return the implementing class which is used for long conversion.
     */
    Class<?> value();
}
