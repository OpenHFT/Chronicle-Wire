package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.IORuntimeException;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.util.BooleanConsumer;
import net.openhft.chronicle.util.ByteConsumer;
import net.openhft.chronicle.util.FloatConsumer;
import net.openhft.chronicle.util.ShortConsumer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * Created by peter on 14/01/15.
 */
public interface ValueIn {
    /*
     * Text / Strings.
     */
    WireIn bool(BooleanConsumer flag);

    WireIn text(StringBuilder s);

    WireIn text(Consumer<String> s);

    default String text() {
        StringBuilder sb = Wires.acquireStringBuilder();
        text(sb);
        return sb.toString();
    }

    WireIn type(StringBuilder s);

    WireIn int8(ByteConsumer i);

    WireIn bytes(Bytes<?> toBytes);

    WireIn bytes(Consumer<byte[]> bytesConsumer);

    WireIn wireIn();

    /**
     * the length of the field as bytes.
     */
    long readLength();

    WireIn uint8(ShortConsumer i);

    WireIn int16(ShortConsumer i);

    WireIn uint16(IntConsumer i);

    WireIn int32(IntConsumer i);

    WireIn uint32(LongConsumer i);

    WireIn int64(LongConsumer i);

    WireIn float32(FloatConsumer v);

    WireIn float64(DoubleConsumer v);

    WireIn time(Consumer<LocalTime> localTime);

    WireIn zonedDateTime(Consumer<ZonedDateTime> zonedDateTime);

    WireIn date(Consumer<LocalDate> localDate);

    boolean hasNext();

    WireIn expectText(CharSequence s);

    WireIn uuid(Consumer<UUID> uuid);

    WireIn int64(LongValue value);

    WireIn int32(IntValue value);

    WireIn sequence(Consumer<ValueIn> reader);

    WireIn marshallable(Marshallable object);

    default Marshallable typedMarshallable() {
        try {
            StringBuilder sb = Wires.acquireStringBuilder();
            type(sb);
            Marshallable m = Class.forName(sb.toString()).asSubclass(Marshallable.class).newInstance();
            marshallable(m);
            return m;
        } catch (Exception e) {
            throw new IORuntimeException(e);
        }
    }

    long int64();
}
