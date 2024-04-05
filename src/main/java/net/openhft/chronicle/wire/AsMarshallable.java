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
 * This annotation indicates that the associated field or parameter should be treated as
 * a {@code Marshallable} type, implying it should be serialized or deserialized accordingly.
 * It can be applied to fields or method parameters to provide metadata about their marshalling behavior.
 */
@Retention(RetentionPolicy.RUNTIME)  // Indicates that this annotation should be retained at runtime.
@Target({ElementType.FIELD, ElementType.PARAMETER})  // Specifies that this annotation can be applied to fields and method parameters.
public @interface AsMarshallable {
}
