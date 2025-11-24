//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.converter;

import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.LongConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate that a given field or parameter, represented as a long value,
 * should be treated as a string containing 0 to 10 characters in Base64 format.
 * <p>
 * When this annotation is applied to a field or parameter, it provides a hint about the expected format
 * and representation of the data, allowing for potential encoding and decoding operations based on Base64.
 * <p>
 * The provided {@link #INSTANCE} is a default converter that can be used for operations relevant to the Base64 format.
 * <b>Example:</b>
 * <pre>
 * {@code @Base64}
 * private long encodedData;
 * </pre>
 *
 * @see LongConverter
 * @see PowerOfTwoLongConverter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@LongConversion(Base64.class)
public @interface Base64 {

    /**
     * An instance of {@link PowerOfTwoLongConverter} specifically configured for Base64 conversions.
     * This converter uses a defined character set suitable for Base64 representations.
     */
    LongConverter INSTANCE = new PowerOfTwoLongConverter(".ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_");
}
