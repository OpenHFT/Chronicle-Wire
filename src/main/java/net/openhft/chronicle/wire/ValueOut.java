package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.values.IntValue;
import net.openhft.chronicle.values.LongValue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Created by peter on 14/01/15.
 */
public interface ValueOut {
    /*
     * data types
     */
    WireOut bool(Boolean flag);

    WireOut text(CharSequence s);

    default WireOut int8(long x) {
        return int8(Maths.toInt8(x));
    }

    WireOut int8(byte i8);

    WireOut bytes(Bytes fromBytes);

    ValueOut writeLength(long remaining);

    WireOut bytes(byte[] fromBytes);

    default WireOut uint8(long x) {
        return uint8checked((byte) Maths.toUInt8(x));
    }

    WireOut uint8checked(int u8);

    default WireOut int16(long x) {
        return int16(Maths.toInt16(x));
    }

    WireOut int16(short i16);

    default WireOut uint16(long x) {
        return uint16checked((short) Maths.toUInt16(x));
    }

    WireOut uint16checked(int u16);

    WireOut utf8(int codepoint);

    default WireOut int32(long x) {
        return int32(Maths.toInt32(x));
    }

    WireOut int32(int i32);

    default WireOut uint32(long x) {
        return uint32checked((int) Maths.toUInt32(x));
    }

    WireOut uint32checked(long u32);

    WireOut int64(long i64);

    WireOut float32(float f);

    WireOut float64(double d);

    WireOut time(LocalTime localTime);

    WireOut zonedDateTime(ZonedDateTime zonedDateTime);

    WireOut date(LocalDate localDate);

    WireOut type(CharSequence typeName);

    WireOut uuid(UUID uuid);

    WireOut int32(IntValue value);

    WireOut int64(LongValue readReady);

    WireOut sequence(Runnable writer);

    WireOut writeMarshallable(Marshallable object);
}
