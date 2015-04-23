/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.IORuntimeException;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.wire.util.BooleanConsumer;
import net.openhft.chronicle.wire.util.ByteConsumer;
import net.openhft.chronicle.wire.util.FloatConsumer;
import net.openhft.chronicle.wire.util.ShortConsumer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.*;

/**
 * Created by peter.lawrey on 14/01/15.
 */
public interface ValueIn {
    /*
     * Text / Strings.
     */
    WireIn bool(BooleanConsumer flag);

    WireIn text(Consumer<String> s);

    default String text() {
        StringBuilder sb = Wires.acquireStringBuilder();
        text(sb);
        return sb.toString();
    }

    WireIn text(StringBuilder s);

    WireIn int8(ByteConsumer i);

    WireIn bytes(Bytes<?> toBytes);

    WireIn bytes(Consumer<WireIn> wireInConsumer);

    byte[] bytes();

    WireIn wireIn();

    /**
     * the length of the field as bytes including any encoding and header character
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

    WireIn int64array(LongArrayValues values, Consumer<LongArrayValues> setter);

    WireIn int64(LongValue value, Consumer<LongValue> setter);

    WireIn int32(IntValue value, Consumer<IntValue> setter);

    WireIn sequence(Consumer<ValueIn> reader);

    <T> T applyToMarshallable(Function<WireIn, T> marshallableReader);

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

    WireIn type(StringBuilder s);

    WireIn marshallable(ReadMarshallable object);

    boolean bool();

    byte int8();

    short int16();

    int int32();

    long int64();

    double float64();

    float float32();

    boolean isNull();

    void map(Consumer<Map> map);
}
