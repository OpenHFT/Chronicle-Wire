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

package net.openhft.chronicle.wire;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to hint that a field or parameter should be marshalled as a
 * {@link Marshallable} even if its declared type is more generic (e.g. an
 * interface).  It is typically employed when the runtime type implements
 * {@code Marshallable} but the declared type does not.
 */
@Retention(RetentionPolicy.RUNTIME)  // Indicates that this annotation should be retained at runtime.
@Target({ElementType.FIELD, ElementType.PARAMETER})  // Specifies that this annotation can be applied to fields and method parameters.
public @interface AsMarshallable {
}
