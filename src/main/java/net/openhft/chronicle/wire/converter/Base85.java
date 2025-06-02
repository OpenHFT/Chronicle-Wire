/*
 * Copyright 2016-2025 chronicle.software
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

import net.openhft.chronicle.wire.Base85LongConverter;
import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.LongConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the associated {@code long} value uses Base85 (Ascii85) text when
 * serialised or parsed in textual wire formats. The conversion logic is implemented
 * by {@link Base85LongConverter}.
 *
 * @see LongConversion
 * @see Base85LongConverter
 * @see LongConverter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@LongConversion(Base85.class)
public @interface Base85 {

    /**
     * An instance of {@link Base85LongConverter} specifically configured for Base85 conversions.
     * This converter uses a character set defined by the {@link Base85LongConverter} to represent Base85 encoded data.
     */
    LongConverter INSTANCE = Base85LongConverter.INSTANCE;
}
