package net.openhft.chronicle.wire;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * Created by peter on 14/01/15.
 */
public interface WriteValue<W> {
    W sequence(Object... array);

    W sequence(Iterable array);

    W sequenceStart();

    W sequenceEnd();

    /*
     * length type.
     */
    long startLength(int bytes);

    boolean endLength(long startPosition);

    /*
     * data types
     */
    W flag(Boolean flag);

    W text(CharSequence s);

    W int8(int i8);

    W uint8(int u8);

    W int16(int i16);

    W uint16(int u16);

    W utf8(int codepoint);

    W int32(int i32);

    W uint32(long u32);

    W float32(float f);

    W float64(double d);

    W int64(long i64);

    W comment(CharSequence s);

    W mapStart();

    W mapEnd();

    W time(LocalTime localTime);

    W zonedDateTime(ZonedDateTime zonedDateTime);

    W date(LocalDate zonedDateTime);

    W object(Marshallable type);

    W type(CharSequence typeName);
}
