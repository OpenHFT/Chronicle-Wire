package net.openhft.chronicle.wire.converter;

import net.openhft.chronicle.wire.IdentifierLongConverter;
import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.LongConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate fields or parameters to signify the long value represent an identifier as a  Nanosecond resolution timestamp from epoch.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@LongConversion(Id.class)
public @interface Id {
    LongConverter INSTANCE = IdentifierLongConverter.INSTANCE;
}
