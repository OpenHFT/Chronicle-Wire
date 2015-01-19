package net.openhft.chronicle.wire;

import net.openhft.chronicle.util.BooleanConsumer;
import net.openhft.chronicle.util.ByteConsumer;
import net.openhft.chronicle.util.FloatConsumer;
import net.openhft.chronicle.util.ShortConsumer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.function.*;

/**
 * Created by peter on 14/01/15.
 */
public interface ReadValue {
    ReadValue sequenceStart();

    Wire sequenceEnd();

    /*
     * Text / Strings.
     */
    Wire bool(BooleanConsumer flag);

    Wire text(StringBuilder s);

    default String text() {
        StringBuilder sb = Wires.acquireStringBuilder();
        text(sb);
        return sb.toString();
    }

    Wire type(StringBuilder s);

    Wire int8(ByteConsumer i);

    Wire uint8(ShortConsumer i);

    Wire int16(ShortConsumer i);

    Wire uint16(IntConsumer i);

    Wire int32(IntConsumer i);

    Wire uint32(LongConsumer i);

    Wire int64(LongConsumer i);

    Wire float32(FloatConsumer v);

    Wire float64(DoubleConsumer v);

    Wire mapStart();

    Wire mapEnd();

    Wire time(Consumer<LocalTime> localTime);

    Wire zonedDateTime(Consumer<ZonedDateTime> zonedDateTime);

    Wire date(Consumer<LocalDate> zonedDateTime);

    Wire object(Supplier<Marshallable> type);

    boolean hasNext();
}
