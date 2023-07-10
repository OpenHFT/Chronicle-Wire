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
 * Annotation to specify a {@link CharConverter} for a field or parameter.
 * <p>
 * This annotation can be used to indicate that a specific {@link CharConverter}
 * should be used to convert the annotated field or parameter to and from character data.
 * The value of the annotation should be the class of the {@link CharConverter} to use.
 * <p>
 * This annotation is retained at runtime and can be applied to both fields and parameters.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface CharConversion {
    /**
     * Returns the {@link CharConverter} class to be used for the annotated field or parameter.
     *
     * @return the {@link CharConverter} class
     */
    Class<? extends CharConverter> value();
}
