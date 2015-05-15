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
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.wire.util.BooleanConsumer;
import net.openhft.chronicle.wire.util.ByteConsumer;
import net.openhft.chronicle.wire.util.FloatConsumer;
import net.openhft.chronicle.wire.util.ShortConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
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
    @NotNull
    WireIn bool(@NotNull BooleanConsumer flag);

    @NotNull
    WireIn text(@NotNull Consumer<String> s);

    default String text() {
        StringBuilder sb = Wires.acquireStringBuilder();
        text(sb);
        return sb.toString();
    }

    @NotNull
    WireIn text(@NotNull StringBuilder s);

    @NotNull
    WireIn int8(@NotNull ByteConsumer i);

    @NotNull
    WireIn bytes(@NotNull Bytes<?> toBytes);

    @NotNull
    WireIn bytes(@NotNull Consumer<WireIn> wireInConsumer);

    byte[] bytes();

    @NotNull
    WireIn wireIn();

    /**
     * the length of the field as bytes including any encoding and header character
     */
    long readLength();

    @NotNull
    WireIn uint8(@NotNull ShortConsumer i);

    @NotNull
    WireIn int16(@NotNull ShortConsumer i);

    @NotNull
    WireIn uint16(@NotNull IntConsumer i);

    @NotNull
    WireIn int32(@NotNull IntConsumer i);

    @NotNull
    WireIn uint32(@NotNull LongConsumer i);

    @NotNull
    WireIn int64(@NotNull LongConsumer i);

    @NotNull
    WireIn float32(@NotNull FloatConsumer v);

    @NotNull
    WireIn float64(@NotNull DoubleConsumer v);

    @NotNull
    WireIn time(@NotNull Consumer<LocalTime> localTime);

    @NotNull
    WireIn zonedDateTime(@NotNull Consumer<ZonedDateTime> zonedDateTime);

    @NotNull
    WireIn date(@NotNull Consumer<LocalDate> localDate);

    boolean hasNext();

    boolean hasNextSequenceItem();

    WireIn uuid(@NotNull Consumer<UUID> uuid);

    WireIn int64array(@Nullable LongArrayValues values, @NotNull Consumer<LongArrayValues> setter);

    WireIn int64(@Nullable LongValue value, @NotNull Consumer<LongValue> setter);

    WireIn int32(@Nullable IntValue value, @NotNull Consumer<IntValue> setter);

    WireIn sequence(@NotNull Consumer<ValueIn> reader);

    <T> T applyToMarshallable(Function<WireIn, T> marshallableReader);


    @NotNull
    default ReadMarshallable typedMarshallable() {
        try {
            StringBuilder sb = Wires.acquireStringBuilder();
            type(sb);

            // its possible that the object that you are allocating may not have a
            // default constructor
            final Class clazz = (Class) Class.forName(sb.toString());

            if (!Marshallable.class.isAssignableFrom(clazz))
                throw new IllegalStateException("its not possible to Marshallable and object that" +
                        " is not of type Marshallable, type=" + sb);

            final ReadMarshallable m = UnsafeMemory.MEMORY.allocateInstance((Class<ReadMarshallable>) clazz);

            marshallable(m);
            return m;
        } catch (Exception e) {
            throw new IORuntimeException(e);
        }
    }

    @NotNull
    WireIn type(@NotNull StringBuilder s);

    @NotNull
    WireIn marshallable(@NotNull ReadMarshallable object);


    /**
     * reads the map from the wire
     */
    default void map(@NotNull Map<String, String> usingMap) {
        map(String.class, String.class, usingMap);
    }

    <K extends ReadMarshallable, V extends ReadMarshallable>
    void typedMap(@NotNull final Map<K, V> usingMap);

    /**
     * reads the map from the wire
     */
    <K, V> Map<K, V> map(@NotNull Class<K> kClazz,
                         @NotNull Class<V> vClass,
                         @NotNull Map<K, V> usingMap);

    boolean bool();

    byte int8();

    short int16();

    int uint16();

    int int32();

    long int64();

    double float64();

    float float32();

    boolean isNull();


    @Nullable
    default <E> E object(@NotNull Class<E> clazz) {
        return object(null, clazz);
    }


    default <E> E object(@Nullable E using,
                         @NotNull Class<E> clazz) {

        if (byte[].class.isAssignableFrom(clazz))
            return (E) bytes();

        else if (Marshallable.class.isAssignableFrom(clazz)) {

            final E v;
            if (using == null)
                try {
                    v = clazz.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            else
                v = using;

            marshallable((Marshallable) v);
            return v;

        } else if (StringBuilder.class.isAssignableFrom(clazz)) {
            StringBuilder builder = (using == null)
                    ? Wires.acquireStringBuilder()
                    : (StringBuilder) using;
            text(builder);
            return (E) builder;

        } else if (CharSequence.class.isAssignableFrom(clazz)) {
            //noinspection unchecked
            return (E) text();

        } else if (Long.class.isAssignableFrom(clazz)) {
            //noinspection unchecked
            return (E) (Long) int64();
        } else if (Double.class.isAssignableFrom(clazz)) {
            //noinspection unchecked
            return (E) (Double) float64();

        } else if (Integer.class.isAssignableFrom(clazz)) {
            //noinspection unchecked
            return (E) (Integer) int32();

        } else if (Float.class.isAssignableFrom(clazz)) {
            //noinspection unchecked
            return (E) (Float) float32();

        } else if (Short.class.isAssignableFrom(clazz)) {
            //noinspection unchecked
            return (E) (Short) int16();

        } else if (Character.class.isAssignableFrom(clazz)) {
            //noinspection unchecked
            final String text = text();
            if (text == null || text.length() == 0)
                return null;
            return (E) (Character) text.charAt(0);

        } else if (Byte.class.isAssignableFrom(clazz)) {
            //noinspection unchecked
            return (E) (Byte) int8();

        } else if (Map.class.isAssignableFrom(clazz)) {
            //noinspection unchecked

            final HashMap result = new HashMap();
            map(result);
            return (E) result;

        } else {
            throw new IllegalStateException("unsupported type");
        }
    }


}
