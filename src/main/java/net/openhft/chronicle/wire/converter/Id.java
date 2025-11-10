//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
