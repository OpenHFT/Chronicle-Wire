//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
 * Annotation to indicate that a given field or parameter, represented as a long value,
 * should be treated as a string containing 0 to 10 characters in Base85 format.
 * <p>
 * Base85, also known as Ascii85, is a binary-to-ASCII encoding scheme optimized for
 * encoding binary data in a compact ASCII string format. It's particularly useful for
 * transporting binary data over text-based protocols where binary formats are not supported.
 * <p>
 * When this annotation is applied to a field or parameter, it provides a hint about the expected format
 * and representation of the data, allowing for potential encoding and decoding operations based on Base85.
 * <p>
 * The provided {@link #INSTANCE} is a default converter that can be used for operations relevant to the Base85 format.
 * <b>Example:</b>
 * <pre>
 * {@code @Base85}
 * private long encodedData;
 * </pre>
 *
 * @see LongConverter
 * @see Base85LongConverter
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
