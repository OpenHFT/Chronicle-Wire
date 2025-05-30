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
 * Indicates that a field or parameter should be treated as a {@link Marshallable}.
 * <p>
 * Useful when the declared type is an interface but the value is known to be a
 * concrete marshallable implementation. The annotation hints to the marshalling
 * framework that the object should be serialised or deserialised as that concrete
 * type rather than as a proxy of the interface.
 */
@Retention(RetentionPolicy.RUNTIME) // Annotation is visible at runtime
@Target({ElementType.FIELD, ElementType.PARAMETER}) // Applicable to fields and parameters
public @interface AsMarshallable {
}
