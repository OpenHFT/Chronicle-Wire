package net.openhft.chronicle.wire;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * Created by peter on 14/01/15.
 */
public interface WriteValue {
    Wire sequence(Object... array);

    Wire sequence(Iterable array);

    Wire sequenceStart();

    Wire sequenceEnd();

    /*
     * length type.
     */
    long startLength(int bytes);

    boolean endLength(long startPosition);

    /*
     * data types
     */
    Wire text(CharSequence s);

    Wire int8(int i8);

    Wire uint8(int u8);

    Wire int16(int i16);

    Wire uint16(int u16);

    Wire utf8(int codepoint);

    Wire int32(int i32);

    Wire uint32(long u32);

    Wire float32(float f);

    Wire float64(double d);

    Wire int64(long i64);

    Wire comment(CharSequence s);

    Wire mapStart();

    Wire mapEnd();

    Wire time(LocalTime localTime);

    Wire zonedDateTime(ZonedDateTime zonedDateTime);

    Wire date(LocalDate zonedDateTime);

    Wire object(Marshallable type);
}
