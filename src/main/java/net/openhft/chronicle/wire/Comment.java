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
 * The Comment annotation is used to associate a human-readable comment with
 * a field, parameter or type. This can be useful for auto-generation of
 * documentation, and for including explanatory comments in serialized
 * output of objects.
 * <p>
 * The value of the Comment annotation is intended to be a format string,
 * which can include a "%s" placeholder. If the annotated item is a String,
 * this placeholder will be replaced with the value of the String when the
 * comment is included in serialized output.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
public @interface Comment {

    /**
     * Returns the comment associated with this annotation.
     *
     * @return A string containing the comment text.
     */
    String value();
}

