package net.openhft.chronicle.wire.converter;

import net.openhft.chronicle.wire.Base85LongConverter;
import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.LongConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate fields or parameters to signify the long value represent a String of 0 to 10 characters in Base85
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@LongConversion(Base85.class)
public @interface Base85 {
    LongConverter INSTANCE = Base85LongConverter.INSTANCE;
}
