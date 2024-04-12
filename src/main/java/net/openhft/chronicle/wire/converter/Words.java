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
import net.openhft.chronicle.wire.WordsLongConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates fields or parameters to signify that the long value represents a string consisting
 * of 0 to 6 words using a base 2048 encoding. This is mainly utilized for converting between
 * long representations and word sequences for better human readability.
 * <p>
 * The actual conversion between long values and words is handled by the {@link WordsLongConverter} class.
 *
 * @see WordsLongConverter
 * @see LongConverter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@LongConversion(Words.class)
public @interface Words {

    /**
     * An instance of the {@link WordsLongConverter} to be used for
     * converting between long values and their word representations.
     */
    LongConverter INSTANCE = new WordsLongConverter();
}
