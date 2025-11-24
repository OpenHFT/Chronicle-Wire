//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
