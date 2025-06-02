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

import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.LongConverter;
import net.openhft.chronicle.wire.ShortTextLongConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code long} field or parameter that uses the compact "short text" Base85
 * representation. Leading spaces are discarded but leading zero is retained.
 * The {@link ShortTextLongConverter} performs the conversion.
 *
 * @see LongConversion
 * @see ShortTextLongConverter
 * @see Base85
 * @see LongConverter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@LongConversion(ShortText.class)
public @interface ShortText {

    /**
     * An instance of {@link ShortTextLongConverter} specifically configured for Base85 conversions.
     * This converter uses a character set defined by the {@link ShortTextLongConverter} to represent Base85 encoded data.
     */
    LongConverter INSTANCE = ShortTextLongConverter.INSTANCE;
}
