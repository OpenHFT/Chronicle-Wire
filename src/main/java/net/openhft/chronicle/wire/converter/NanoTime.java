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

import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.LongConverter;
import net.openhft.chronicle.wire.NanoTimestampLongConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate that a given field or parameter, represented as a long value,
 * should be treated as a timestamp with nanosecond resolution, based from the epoch.
 * <p>
 * The epoch, typically referred to as "UNIX epoch," is the point in time when time starts,
 * and is represented as "1970-01-01 00:00:00 UTC". A value annotated with {@code NanoTime}
 * counts the number of nanoseconds since the epoch. For instance, a value of 1,000,000,000
 * would indicate 1 second past the epoch.
 * </p>
 * <p>
 * When this annotation is applied to a field or parameter, it provides a hint about the
 * expected format and representation of the data, allowing for potential encoding, decoding,
 * and date-time operations based on nanosecond resolution timestamps.
 * </p>
 * <p>
 * The provided {@link #INSTANCE} is a default converter specifically crafted for
 * operations relevant to the nanosecond timestamp format.
 * </p>
 *
 * <b>Example:</b>
 * <pre>
 * {@code
 * @NanoTime
 * private long timestamp;
 * }
 * </pre>
 *
 * @see LongConverter
 * @see NanoTimestampLongConverter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@LongConversion(NanoTime.class)
public @interface NanoTime {

    /**
     * An instance of {@link NanoTimestampLongConverter} specifically configured for
     * conversions related to nanosecond resolution timestamps.
     * This converter processes and manipulates data using the nanosecond timestamp format.
     */
    LongConverter INSTANCE = new NanoTimestampLongConverter();
}
