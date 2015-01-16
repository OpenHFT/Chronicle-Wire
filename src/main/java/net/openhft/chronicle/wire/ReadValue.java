package net.openhft.chronicle.wire;

import net.openhft.chronicle.util.BooleanConsumer;
import net.openhft.chronicle.util.ByteConsumer;
import net.openhft.chronicle.util.FloatConsumer;
import net.openhft.chronicle.util.ShortConsumer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.function.*;

/**
 * Created by peter on 14/01/15.
 */
public interface ReadValue<W> {
    W sequenceLength(IntConsumer length);

    W sequence(Supplier<Collection> collection);

    /*
     * Text / Strings.
     */
    W flag(BooleanConsumer flag);

    W text(StringBuilder s);

    W type(StringBuilder s);

    W int8(ByteConsumer i);

    W uint8(ShortConsumer i);

    W int16(ShortConsumer i);

    W uint16(IntConsumer i);

    W int32(IntConsumer i);

    W uint32(LongConsumer i);

    W int64(LongConsumer i);

    W float32(FloatConsumer v);

    W float64(DoubleConsumer v);

    W mapStart();

    W mapEnd();

    W time(Consumer<LocalTime> localTime);

    W zonedDateTime(Consumer<ZonedDateTime> zonedDateTime);

    W date(Consumer<LocalDate> zonedDateTime);

    W object(Supplier<Marshallable> type);
}
