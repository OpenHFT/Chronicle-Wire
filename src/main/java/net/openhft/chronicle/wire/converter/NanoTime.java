package net.openhft.chronicle.wire.converter;

import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.LongConverter;
import net.openhft.chronicle.wire.NanoTimestampLongConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate fields or parameters to signify the long value represent a Nanosecond resolution timestamp from epoch.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@LongConversion(NanoTime.class)
public @interface NanoTime {
    LongConverter INSTANCE = new NanoTimestampLongConverter();
}
