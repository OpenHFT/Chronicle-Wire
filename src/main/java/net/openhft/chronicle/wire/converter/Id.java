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

import net.openhft.chronicle.wire.IdentifierLongConverter;
import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.LongConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to signify that the annotated field or parameter represents an identifier,
 * specifically as a Nanosecond resolution timestamp from epoch.
 * <p>
 * This annotation could be used in scenarios where the system wants to generate
 * unique identifiers based on the precise timestamp at which they are created.
 * <p>
 * The INSTANCE field is a singleton instance of the IdentifierLongConverter class,
 * which is used to perform the conversion between long values and the nanosecond timestamp.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@LongConversion(Id.class)
public @interface Id {
    LongConverter INSTANCE = IdentifierLongConverter.INSTANCE;
}
