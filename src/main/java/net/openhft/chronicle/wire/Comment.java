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
 * The Comment annotation represents a way to attach user-defined comments to elements like fields,
 * parameters, or types. This can be especially useful for documenting the intention, usage, or any other
 * relevant information about the annotated element at runtime.
 * <p>
 * Being retained at runtime, it can be inspected via reflection to influence behavior based on the comment's content.
 */
@Retention(RetentionPolicy.RUNTIME)  // This annotation is available at runtime.
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})  // Specifies the kinds of elements this annotation can be applied to.
public @interface Comment {

    /**
     * Returns the comment content as a string.
     *
     * @return The comment content associated with the annotated element.
     */
    String value();
}
