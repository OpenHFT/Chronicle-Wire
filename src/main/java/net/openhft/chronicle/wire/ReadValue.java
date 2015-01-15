package net.openhft.chronicle.wire;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.function.*;

/**
 * Created by peter on 14/01/15.
 */
public interface ReadValue {
    Wire sequenceLength(IntConsumer length);

    Wire sequence(Supplier<Collection> collection);

    /*
     * Text / Strings.
     */
    Wire text(Supplier<StringBuilder> s);

    Wire int32(IntConsumer i);

    Wire float64(DoubleConsumer v);

    Wire int64(LongConsumer i);

    Wire comment(Supplier<StringBuilder> s);

    Wire mapStart();

    Wire mapEnd();

    Wire time(Consumer<LocalTime> localTime);

    Wire zonedDateTime(Consumer<ZonedDateTime> zonedDateTime);

    Wire date(Consumer<LocalDate> zonedDateTime);

    Wire object(Supplier<Marshallable> type);
}
