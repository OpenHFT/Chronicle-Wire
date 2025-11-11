/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation used to associate a concrete {@link LongConverter}
 * implementation with another annotation. Custom marker annotations (for
 * example {@code @Hexadecimal} or {@code @Base64}) can declare which converter
 * should be applied when serialising or deserialising {@code long} fields or
 * parameters. The referenced converter annotation should either have a static
 * final instance named `INSTANCE` or should be a {@link LongConverter}
 *
 * <p>Example usage:
 * <pre><code>
 * &#64;LongConversion(HexadecimalLongConverter.class)
 * &#64;Retention(RetentionPolicy.RUNTIME)
 * &#64;Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
 * public &#64;interface MyHexFormat {
 *     LongConverter INSTANCE = new MyHexFormatConverter("0123456789ABCDEF");
 * }
 * </code></pre>
 *
 * @see LongConverter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.PARAMETER})
public @interface LongConversion {

    /**
     * Returns the class responsible for converting the long value or an annotation with an INSTANCE of one.
     *<p>
     * The {@link LongConverter} class to be used for conversion.
     * The class specified should either have a static final field named INSTANCE,
     * or a constructor that takes a single string parameter for initialization.
     *
     * @return The implementing class which either contains a static final field named `INSTANCE`
     *         or provides a constructor that takes a string for initialization.
     */
    Class<?> value();
}
