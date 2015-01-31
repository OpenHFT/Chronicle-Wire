package net.openhft.chronicle.wire;

import net.openhft.lang.values.IntValue;
import net.openhft.lang.values.LongValue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Created by peter on 14/01/15.
 */
public interface ValueOut {
    ValueOut sequenceStart();

    WireOut sequenceEnd();

    /*
     * data types
     */
    WireOut bool(Boolean flag);

    WireOut text(CharSequence s);

    WireOut int8(int i8);

    WireOut uint8(int u8);

    WireOut int16(int i16);

    WireOut uint16(int u16);

    WireOut utf8(int codepoint);

    WireOut int32(int i32);

    WireOut uint32(long u32);

    WireOut float32(float f);

    WireOut float64(double d);

    WireOut int64(long i64);

    WireOut hint(CharSequence hint);

    WireOut mapStart();

    WireOut mapEnd();

    WireOut time(LocalTime localTime);

    WireOut zonedDateTime(ZonedDateTime zonedDateTime);

    WireOut date(LocalDate zonedDateTime);

    WireOut object(Marshallable type);

    WireOut type(CharSequence typeName);

    WireOut uuid(UUID uuid);

    ValueOut cacheAlign();

    WireOut int64(LongValue readReady);

    WireOut int32(IntValue value);
}
