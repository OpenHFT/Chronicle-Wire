/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.*;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.*;

/**
 * Read in data after reading a field.
 */
public interface ValueIn {
    Consumer<ValueIn> DISCARD = v -> {
    };

    /*
     * Text / Strings.
     */
    @NotNull
    WireIn bool(@NotNull BooleanConsumer flag);

    @NotNull
    default WireIn text(@NotNull Consumer<String> s) {
        s.accept(text());
        return wireIn();
    }

    default <T> WireIn text(T t, @NotNull BiConsumer<T, String> ts) {
        ts.accept(t, text());
        return wireIn();
    }

    default WireIn text(@NotNull StringBuilder sb) {
        if (textTo(sb) == null)
            sb.setLength(0);
        return wireIn();
    }

    default WireIn text(@NotNull Bytes sdo) {
        sdo.clear();
        textTo(sdo);
        return wireIn();
    }

    @Nullable
    String text();

    @Nullable
    StringBuilder textTo(@NotNull StringBuilder sb);

    @Nullable
    Bytes textTo(@NotNull Bytes bytes);

    @NotNull
    WireIn bytes(@NotNull Bytes<?> toBytes);

    @NotNull
    WireIn bytesMatch(@NotNull BytesStore compareBytes, BooleanConsumer consumer);

    @NotNull
    WireIn bytes(@NotNull ReadMarshallable wireInConsumer);

    @Nullable
    byte[] bytes();

    @Nullable
    default BytesStore bytesStore() {
        byte[] bytes = bytes();
        return bytes == null ? null : BytesStore.wrap(bytes);
    }

    @NotNull
    WireIn wireIn();

    /**
     * the length of the field as bytes including any encoding and header character
     */
    long readLength();

    @NotNull
    WireIn int8(@NotNull ByteConsumer b);

    @NotNull
    default <T> WireIn int8(@NotNull T t, @NotNull ObjByteConsumer<T> tb) {
        return int8(b -> tb.accept(t, b));
    }

    @NotNull
    WireIn uint8(@NotNull ShortConsumer i);

    @NotNull
    default <T> WireIn uint8(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
        return uint8(i -> ti.accept(t, i));
    }

    @NotNull
    WireIn int16(@NotNull ShortConsumer i);

    @NotNull
    default <T> WireIn int16(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
        return int16(i -> ti.accept(t, i));
    }

    @NotNull
    WireIn uint16(@NotNull IntConsumer i);

    @NotNull
    default <T> WireIn uint16(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
        return uint16(i -> ti.accept(t, i));
    }

    @NotNull
    WireIn int32(@NotNull IntConsumer i);

    @NotNull
    default <T> WireIn int32(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
        return int32(i -> ti.accept(t, i));
    }

    @NotNull
    WireIn uint32(@NotNull LongConsumer i);

    @NotNull
    default <T> WireIn uint32(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
        return uint32(l -> tl.accept(t, l));
    }

    @NotNull
    WireIn int64(@NotNull LongConsumer l);

    @NotNull
    default <T> WireIn int64(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
        return int64(l -> tl.accept(t, l));
    }

    @NotNull
    WireIn float32(@NotNull FloatConsumer v);

    @NotNull
    default <T> WireIn float32(@NotNull T t, @NotNull ObjFloatConsumer<T> tf) {
        return float32(f -> tf.accept(t, f));
    }

    @NotNull
    WireIn float64(@NotNull DoubleConsumer d);

    @NotNull
    default <T> WireIn float64(@NotNull T t, @NotNull ObjDoubleConsumer<T> td) {
        return float64(d -> td.accept(t, d));
    }

    @NotNull
    WireIn time(@NotNull Consumer<LocalTime> setLocalTime);

    @NotNull
    default <T> WireIn time(@NotNull T t, @NotNull BiConsumer<T, LocalTime> setLocalTime) {
        return time(localTime -> setLocalTime.accept(t, localTime));
    }

    @NotNull
    WireIn zonedDateTime(@NotNull Consumer<ZonedDateTime> zonedDateTime);

    @NotNull
    default <T> WireIn zonedDateTime(@NotNull T t, @NotNull BiConsumer<T, ZonedDateTime> tZonedDateTime) {
        return zonedDateTime(zonedDateTime -> tZonedDateTime.accept(t, zonedDateTime));
    }

    @NotNull
    WireIn date(@NotNull Consumer<LocalDate> localDate);

    @NotNull
    default <T> WireIn date(@NotNull T t, @NotNull BiConsumer<T, LocalDate> tLocalDate) {
        return date(localDate -> tLocalDate.accept(t, localDate));
    }

    boolean hasNext();

    boolean hasNextSequenceItem();

    @NotNull
    WireIn uuid(@NotNull Consumer<UUID> uuid);

    @NotNull
    default <T> WireIn uuid(@NotNull T t, @NotNull BiConsumer<T, UUID> tuuid) {
        return uuid(uuid -> tuuid.accept(t, uuid));
    }

    @NotNull
    WireIn int64array(@Nullable LongArrayValues values, @NotNull Consumer<LongArrayValues> setter);

    @NotNull
    WireIn int64(@Nullable LongValue value);

    @NotNull
    WireIn int64(@Nullable LongValue value, @NotNull Consumer<LongValue> setter);

    @NotNull
    WireIn int32(@Nullable IntValue value, @NotNull Consumer<IntValue> setter);

    @NotNull
    WireIn sequence(@NotNull Consumer<ValueIn> reader);

    @NotNull
    default <T> WireIn sequence(@NotNull T t, @NotNull BiConsumer<T, ValueIn> tReader) {
        return sequence(reader -> tReader.accept(t, reader));
    }

    <T> T applyToMarshallable(Function<WireIn, T> marshallableReader);

    @Nullable
    <T extends ReadMarshallable> T typedMarshallable();

    @NotNull
    ValueIn type(@NotNull StringBuilder s);

    default <T> ValueIn type(T t, @NotNull BiConsumer<T, StringBuilder> ts) {
        return type(s -> ts.accept(t, s));
    }

    @NotNull
    default ValueIn type(@NotNull Consumer<StringBuilder> s) {
        StringBuilder sb = Wires.acquireStringBuilder();
        type(sb);
        s.accept(sb);
        return this;
    }

    @NotNull
    WireIn typeLiteralAsText(@NotNull Consumer<CharSequence> classNameConsumer);

    @NotNull
    default WireIn typeLiteral(@NotNull Function<CharSequence, Class> typeLookup, @NotNull Consumer<Class> classConsumer) {
        return typeLiteralAsText(sb -> classConsumer.accept(typeLookup.apply(sb)));
    }

    @NotNull
    default WireIn typeLiteral(@NotNull Consumer<Class> classConsumer) {
        return typeLiteral(ClassAliasPool.CLASS_ALIASES::forName, classConsumer);
    }

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
    @Nullable
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

    default Throwable throwable(boolean appendCurrentStack) {
        return Wires.throwable(this, appendCurrentStack);
    }

    default <E extends Enum<E>> E asEnum(Class<E> eClass) {
        StringBuilder sb = Wires.acquireStringBuilder();
        text(sb);
        return sb.length() == 0 ? null : Wires.internEnum(eClass, sb);
    }

    default <E extends Enum<E>> WireIn asEnum(Class<E> eClass, Consumer<E> eConsumer) {
        eConsumer.accept(asEnum(eClass));
        return wireIn();
    }

    @Nullable
    default <E> E object(@NotNull Class<E> clazz) {
        return object(null, clazz);
    }

    @Nullable
    <E> E object(@Nullable E using, @NotNull Class<E> clazz);

    @Nullable
    <E> WireIn object(@NotNull Class<E> clazz, Consumer<E> e);

    default Class typeLiteral() {
        Class[] clazz = {null};
        typeLiteral(
                ClassAliasPool.CLASS_ALIASES::forName, c -> clazz[0] = c);
        return clazz[0];
    }

    default byte[] snappy() {
        throw new UnsupportedOperationException();
    }
}
