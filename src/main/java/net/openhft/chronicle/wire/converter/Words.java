package net.openhft.chronicle.wire.converter;

import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.LongConverter;
import net.openhft.chronicle.wire.WordsLongConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate fields or parameters to signify the long value represent a String of 0 to 6 words in base 2048
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@LongConversion(Words.class)
public @interface Words {
    LongConverter INSTANCE = new WordsLongConverter();
}
