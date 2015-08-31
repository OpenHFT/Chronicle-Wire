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
    <T> WireIn bool(T t, @NotNull ObjBooleanConsumer<T> tFlag);

    @NotNull
    <T> WireIn int8(@NotNull T t, @NotNull ObjByteConsumer<T> tb);

    @NotNull
    <T> WireIn uint8(@NotNull T t, @NotNull ObjShortConsumer<T> ti);

    @NotNull
    <T> WireIn int16(@NotNull T t, @NotNull ObjShortConsumer<T> ti);

    @NotNull
    <T> WireIn uint16(@NotNull T t, @NotNull ObjIntConsumer<T> ti);

    @NotNull
    <T> WireIn int32(@NotNull T t, @NotNull ObjIntConsumer<T> ti);

    @NotNull
    <T> WireIn uint32(@NotNull T t, @NotNull ObjLongConsumer<T> tl);

    @NotNull
    <T> WireIn int64(@NotNull T t, @NotNull ObjLongConsumer<T> tl);

    @NotNull
    <T> WireIn float32(@NotNull T t, @NotNull ObjFloatConsumer<T> tf);

    @NotNull
    <T> WireIn float64(@NotNull T t, @NotNull ObjDoubleConsumer<T> td);

    @NotNull
    <T> WireIn time(@NotNull T t, @NotNull BiConsumer<T, LocalTime> setLocalTime);

    @NotNull
    <T> WireIn zonedDateTime(@NotNull T t, @NotNull BiConsumer<T, ZonedDateTime> tZonedDateTime);

    @NotNull
    <T> WireIn date(@NotNull T t, @NotNull BiConsumer<T, LocalDate> tLocalDate);

    boolean hasNext();

    boolean hasNextSequenceItem();

    @NotNull
    <T> WireIn uuid(@NotNull T t, @NotNull BiConsumer<T, UUID> tuuid);

    @NotNull
    <T> WireIn int64array(@Nullable LongArrayValues values, T t, @NotNull BiConsumer<T, LongArrayValues> setter);

    @NotNull
    WireIn int64(@Nullable LongValue value);

    @NotNull
    <T> WireIn int64(@Nullable LongValue value, T t, @NotNull BiConsumer<T, LongValue> setter);

    @NotNull
    <T> WireIn int32(@Nullable IntValue value, T t, @NotNull BiConsumer<T, IntValue> setter);

    @NotNull
    <T> WireIn sequence(@NotNull T t, @NotNull BiConsumer<T, ValueIn> tReader);

    <T> T applyToMarshallable(Function<WireIn, T> marshallableReader);

    @Nullable
    <T extends ReadMarshallable> T typedMarshallable();

    @NotNull
    <T> ValueIn typePrefix(T t, @NotNull BiConsumer<T, CharSequence> ts);

    @NotNull
    <T> WireIn typeLiteralAsText(T t, @NotNull BiConsumer<T, CharSequence> classNameConsumer);

    @NotNull
    default <T> WireIn typeLiteral(T t, @NotNull BiConsumer<T, Class> classConsumer) {
        return typeLiteralAsText(t, (o, x) -> classConsumer.accept(o, ClassAliasPool.CLASS_ALIASES.forName(x)));
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

    Class typeLiteral();

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

    default <E extends Enum<E>, T> WireIn asEnum(Class<E> eClass, T t, BiConsumer<T, E> teConsumer) {
        teConsumer.accept(t, asEnum(eClass));
        return wireIn();
    }

    @Nullable
    default <E> E object(@NotNull Class<E> clazz) {
        return object(null, clazz);
    }

    @Nullable
    <E> E object(@Nullable E using, @NotNull Class<E> clazz);

    @Nullable
    <T, E> WireIn object(@NotNull Class<E> clazz, T t, BiConsumer<T, E> e);

    default WireIn decompress() {
        throw new UnsupportedOperationException();
    }

    boolean isTyped();

    Class typePrefix();
}
